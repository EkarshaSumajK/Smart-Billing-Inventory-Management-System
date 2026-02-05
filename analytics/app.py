from flask import Flask, jsonify, request
from flask_cors import CORS
from flask_pymongo import PyMongo
import pandas as pd
from datetime import datetime
from dotenv import load_dotenv
from dateutil import parser
import os
from translations import get_translation as t
import io
import base64
import matplotlib
matplotlib.use('Agg') # Non-interactive backend
import matplotlib.pyplot as plt
from prophet import Prophet
from mlxtend.frequent_patterns import apriori, association_rules
from mlxtend.preprocessing import TransactionEncoder
import threading
import time
import math
import sample_data

# GLOBAL CACHE FOR RULES
ASSOCIATION_RULES = None
LAST_TRAINED = None


# -----------------------
# Initialization
# -----------------------
load_dotenv()
app = Flask(__name__)
CORS(app)

# MongoDB Config
app.config["MONGO_URI"] = os.getenv("MONGO_URI")
db_name = os.getenv("DB_NAME")
mongo = PyMongo(app)
db = mongo.cx[db_name]

# -----------------------
# Helper Functions
# -----------------------

def generate_prediction(shop_id, product_id):
    """
    Fit Prophet model for a product, generate forecast, and return analysis results.
    Returns: (trend: 'up'|'down'|'stable', predicted_price: float, graph_b64: str)
    """
    # 1. Fetch historical data for this product and shop
    # We aggregate by day to get weighted average price
    
    # Check if we have enough data first (e.g., at least 5 distinct days)
    pipeline = [
        {
            "$match": {
                "shopId": shop_id,
                "items.productId": product_id
            }
        },
        {"$unwind": "$items"},
        {
            "$match": {
                "items.productId": product_id
            }
        },
        {
            "$group": {
                "_id": { 
                    "date": { "$dateToString": { "format": "%Y-%m-%d", "date": { "$ifNull": ["$createdAt", "$date"] } } }
                },
                "totalRevenue": { "$sum": { "$multiply": ["$items.price", { "$ifNull": ["$items.quantity", "$items.qty"] }] } },
                "totalQty": { "$sum": { "$ifNull": ["$items.quantity", "$items.qty"] } }
            }
        },
        {"$sort": {"_id.date": 1}}
    ]
    
    results = list(db.bills.aggregate(pipeline))
    
    if len(results) < 5:
        print(f"DEBUG: Insufficient data {len(results)}")
        return {"error": "insufficient_data", "count": len(results)}
        
    print(f"DEBUG: Found {len(results)} days of history")
    data = []
    for r in results:
        # print(f"DEBUG: Row: {r}")
        if r['totalQty'] > 0:
            avg_price = r['totalRevenue'] / r['totalQty']
            data.append({
                'ds': r['_id']['date'],
                'y': avg_price
            })
            
    df = pd.DataFrame(data, columns=['ds', 'y'])
    print(f"DEBUG: DF Shape: {df.shape}")
    
    if df.empty or len(df) < 5:
        print("DEBUG: DF is empty or too small!")
        return None
    
    # --- ROBUSTNESS UPGRADE ---
    # 2. Outlier Removal (IQR Method)
    Q1 = df['y'].quantile(0.25)
    Q3 = df['y'].quantile(0.75)
    IQR = Q3 - Q1
    # Filter only extreme outliers (3x IQR) to keep natural variance
    df = df[~((df['y'] < (Q1 - 3 * IQR)) | (df['y'] > (Q3 + 3 * IQR)))]
    
    # 3. Fit Prophet Model (Enhanced Config)
    # Enable weekly seasonality (crucial for retail)
    # Adjust changepoint_prior_scale for flexibility vs overfitting
    m = Prophet(
        daily_seasonality=True, 
        yearly_seasonality=False,  # Needs year of data
        weekly_seasonality=True,   # Critical for retail (Weekends vs Weekdays)
        changepoint_prior_scale=0.1
    )
    m.fit(df)
    
    # 4. Forecast
    future = m.make_future_dataframe(periods=7)
    forecast = m.predict(future)
    
    # 5. Determine Trend (comparing average of last 3 days vs future)
    current_avg = df.tail(3)['y'].mean()
    predicted_avg = forecast.tail(7)['yhat'].mean()
    
    trend = 'stable'
    if predicted_avg > current_avg * 1.05: # 5% growth
        trend = 'up'
    elif predicted_avg < current_avg * 0.95:
        trend = 'down'
        
    # 6. Generate Graph (Internal Debug Use)
    plt.figure(figsize=(6, 4))
    plt.plot(pd.to_datetime(df['ds']), df['y'], 'k.', label='History')
    plt.plot(forecast['ds'], forecast['yhat'], 'b-', label='Prediction')
    plt.fill_between(forecast['ds'], forecast['yhat_lower'], forecast['yhat_upper'], color='blue', alpha=0.2)
    plt.title('7-Day Price Forecast')
    plt.xlabel('Date')
    plt.ylabel('Price')
    plt.legend()
    plt.tight_layout()
    
    img = io.BytesIO()
    plt.savefig(img, format='png')
    img.seek(0)
    graph_b64 = base64.b64encode(img.getvalue()).decode()
    plt.close()
    
    # 7. Prepare Data Points for Frontend (Actuals + Forecast)
    # Merge forecast with actuals to show dots on the line
    viz_data = forecast[['ds', 'yhat']].tail(30).copy() # Last 30 days for clarity
 
    viz_data['ds'] = viz_data['ds'].dt.strftime('%Y-%m-%d')
    points = viz_data.to_dict(orient='records')
    
    return {
        'trend': trend,
        'predictedPrice': round(predicted_avg, 2),
        'graphBase64': graph_b64,
        'points': points,
        'lastUpdated': datetime.utcnow()
    }

def calculate_inventory_intelligence(shop_id, product_id, current_stock):
    """
    Analyze sales velocity and forecast demand quantity.
    Returns: { velocity: str, recommended_stock: int, forecast_7d: int, points: list }
    """
    # 1. Fetch Aggregated Sales Quantity
    pipeline = [
        { "$match": { "shopId": shop_id, "items.productId": product_id } },
        { "$unwind": "$items" },
        { "$match": { "items.productId": product_id } },
        {
            "$group": {
                "_id": { "date": { "$dateToString": { "format": "%Y-%m-%d", "date": { "$ifNull": ["$createdAt", "$date"] } } } },
                "totalQty": { "$sum": { "$ifNull": ["$items.quantity", "$items.qty"] } }
            }
        },
        { "$sort": { "_id.date": 1 } }
    ]
    results = list(db.bills.aggregate(pipeline))
    
    if len(results) < 5:
        return {"status": "learning", "velocity": "unknown", "recommended_stock": 0, "reason": "Insufficient Data"}

    # 2. Velocity Calculation (Last 7 vs Prev 30)
    data = []
    for r in results:
        data.append({'ds': r['_id']['date'], 'y': r['totalQty']})
    
    df = pd.DataFrame(data)
    df['ds'] = pd.to_datetime(df['ds'])
    
    last_date = df['ds'].max()
    week_start = last_date - pd.Timedelta(days=7)
    month_start = last_date - pd.Timedelta(days=37)
    
    recent_sales = df[df['ds'] > week_start]['y'].mean() or 0
    historical_sales = df[(df['ds'] <= week_start) & (df['ds'] > month_start)]['y'].mean() or 0.1 # avoid div/0
    
    growth = ((recent_sales - historical_sales) / historical_sales) * 100
    
    if growth > 20: velocity = "fast"
    elif growth < -10: velocity = "slow"
    else: velocity = "moderate"

    # 3. Forecast Demand (Prophet)
    m = Prophet(daily_seasonality=False, yearly_seasonality=False, weekly_seasonality=False)
    m.fit(df)
    future = m.make_future_dataframe(periods=7)
    forecast = m.predict(future)
    
    # Sum of forecasted quantity for next 7 days
    next_7_days = forecast.tail(7)['yhat'].sum()
    if next_7_days < 0: next_7_days = 0
    
    # 4. Recommendation
    # Safety Buffer 1.2x
    needed = math.ceil(next_7_days * 1.2)
    to_buy = max(0, needed - int(current_stock))
    
    # Viz Points for Volume Graph
    viz_data = forecast[['ds', 'yhat']].tail(60).copy() # Show last 60 days of volume trend
    viz_data['ds'] = viz_data['ds'].dt.strftime('%Y-%m-%d')
    points = viz_data.to_dict(orient='records')

    # 5. Stock Coverage (Days)
    daily_demand = next_7_days / 7
    if daily_demand > 0:
        coverage_days = round(int(float(current_stock)) / daily_demand, 1)
    else:
        coverage_days = 999 if int(float(current_stock)) > 0 else 0

    return {
        "status": "success",
        "velocity": velocity,
        "growth_pct": round(growth, 1),
        "forecast_7d": round(next_7_days, 1),
        "recommended_stock": to_buy,
        "stockCoverageDays": coverage_days,
        "points": points
    }

def normalize_bills(raw_bills):
    """Normalize bills into a consistent DataFrame"""
    records = []
    for bill in raw_bills:
        # Prefer 'createdAt'
        bill_date = bill.get("createdAt") or bill.get("date")
        if isinstance(bill_date, dict) and "$date" in bill_date:
            bill_date = bill_date["$date"]
        bill_date = pd.to_datetime(bill_date)

        total = bill.get("totalAmount") or bill.get("total", 0)
        items = bill.get("items", [])
        for item in items:
            qty = item.get("quantity") or item.get("qty", 0)
            price = item.get("price", 0)
            product_id = item.get("productId") or item.get("product_id") or item.get("id")
            name = item.get("name") or item.get("productName") or "Unknown Product"

            # Only add records with valid data
            if qty > 0 and price > 0:
                records.append({
                    "date": bill_date,
                    "productId": str(product_id) if product_id else "unknown",
                    "name": name,
                    "qty": qty,
                    "price": price,
                    "revenue": price * qty,
                    "total": total
                })

    df = pd.DataFrame(records)
    print(f"📦 Normalized {len(df)} item records from {len(raw_bills)} bills")
    if not df.empty:
        print(f"   Products found: {df['name'].unique().tolist()[:5]}")
    return df


def get_filtered_bills():
    """Apply date filtering if provided"""
    start_str = request.args.get("startDate")
    end_str = request.args.get("endDate")
    shop_id = request.args.get("shopId")

    query = {}
    if start_str and end_str:
        try:
            start_dt = datetime.strptime(start_str, "%Y-%m-%d")
            end_dt = datetime.strptime(end_str, "%Y-%m-%d").replace(hour=23, minute=59, second=59)
            query["createdAt"] = {"$gte": start_dt, "$lte": end_dt}
            print(f"✅ Filtering bills between {start_dt} → {end_dt}")
        except ValueError:
            print("❌ Invalid date format received")
            return jsonify({"error": "Invalid date format. Use YYYY-MM-DD"}), 400
    
    if shop_id:
        query["shopId"] = shop_id
        print(f"✅ Filtering bills for shopId: {shop_id}")

    bills = list(db.bills.find(query))
    print(f"📊 Found {len(bills)} bills matching the criteria")
    return bills

# -----------------------
# API Endpoints
# -----------------------

@app.route("/")
def home():
    return jsonify({"status": "Analytics API running 🚀"})


@app.route("/analytics/daily")
def daily_sales():
    raw_bills = get_filtered_bills()
    df = normalize_bills(raw_bills)
    if df.empty:
        return jsonify({
            "daily": [],
            "weekly": [],
            "monthly": [],
            "top_products": [],
            "revenue_trend": [],
            "summary": {
                "total_revenue": 0,
                "total_orders": 0,
                "total_products_sold": 0
            },
            "message": "No billing data"
        }), 200


    summary = (
        df.groupby(df["date"].dt.date)["revenue"]
        .sum()
        .reset_index()
        .rename(columns={"date": "day", "revenue": "totalSales"})
    )
    summary["day"] = summary["day"].astype(str)
    return jsonify(summary.to_dict(orient="records"))


@app.route("/analytics/monthly")
def monthly_sales():
    raw_bills = get_filtered_bills()
    df = normalize_bills(raw_bills)
    if df.empty:
        return jsonify({
            "daily": [],
            "weekly": [],
            "monthly": [],
            "top_products": [],
            "revenue_trend": [],
            "summary": {
                "total_revenue": 0,
                "total_orders": 0,
                "total_products_sold": 0
            },
            "message": "No billing data"
        }), 200


    summary = (
        df.groupby(df["date"].dt.to_period("M"))["revenue"]
        .sum()
        .reset_index()
    )
    summary["date"] = summary["date"].astype(str)
    summary = summary.rename(columns={"date": "month", "revenue": "totalSales"})
    return jsonify(summary.to_dict(orient="records"))


@app.route("/analytics/weekly")
def weekly_sales():
    raw_bills = get_filtered_bills()
    df = normalize_bills(raw_bills)
    if df.empty:
        return jsonify({
            "daily": [],
            "weekly": [],
            "monthly": [],
            "top_products": [],
            "revenue_trend": [],
            "summary": {
                "total_revenue": 0,
                "total_orders": 0,
                "total_products_sold": 0
            },
            "message": "No billing data"
        }), 200


    summary = (
        df.groupby(df["date"].dt.to_period("W"))["revenue"]
        .sum()
        .reset_index()
    )
    summary["date"] = summary["date"].astype(str)
    summary = summary.rename(columns={"date": "week", "revenue": "totalSales"})
    return jsonify(summary.to_dict(orient="records"))


@app.route("/analytics/top-products")
def top_products():
    """Get top products filtered by date range"""
    raw_bills = get_filtered_bills()
    df = normalize_bills(raw_bills)
    if df.empty:
        return jsonify({
            "daily": [],
            "weekly": [],
            "monthly": [],
            "top_products": [],
            "revenue_trend": [],
            "summary": {
                "total_revenue": 0,
                "total_orders": 0,
                "total_products_sold": 0
            },
            "message": "No billing data"
        }), 200


    # Fetch all products for name and category lookup (filtered by shopId)
    shop_id = request.args.get("shopId")
    product_query = {}
    if shop_id:
        product_query["shopId"] = shop_id
    
    products = list(db.products.find(product_query))
    products_df = pd.DataFrame(products)

    # Merge bill data with product data
    merged = df.merge(
        products_df[["productId", "name", "category"]],
        on="productId",
        how="left",
        suffixes=("_bill", "_prod")
    )

    # Combine names (prefer bill name, fallback to product name)
    merged["name"] = merged["name_bill"].combine_first(merged["name_prod"])
    merged["category"] = merged["category"].fillna("Unknown")

    # Aggregate by product
    summary = (
        merged.groupby(["productId", "name", "category"])
        .agg({"qty": "sum", "revenue": "sum"})
        .reset_index()
        .sort_values("revenue", ascending=False)
        .head(10)
    )

    print(f"🏆 Top {len(summary)} products calculated")
    return jsonify(summary.to_dict(orient="records"))


@app.route("/analytics/revenue-trend")
def revenue_trend():
    """Get revenue trend filtered by date range"""
    raw_bills = get_filtered_bills()
    df = normalize_bills(raw_bills)
    if df.empty:
        return jsonify({
            "daily": [],
            "weekly": [],
            "monthly": [],
            "top_products": [],
            "revenue_trend": [],
            "summary": {
                "total_revenue": 0,
                "total_orders": 0,
                "total_products_sold": 0
            },
            "message": "No billing data"
        }), 200

    trend = (
        df.groupby(df["date"].dt.date)["revenue"]
        .sum()
        .reset_index()
        .rename(columns={"date": "day", "revenue": "totalRevenue"})
    )
    trend["day"] = trend["day"].astype(str)
    return jsonify(trend.to_dict(orient="records"))


@app.route("/analytics/report")
def sales_report():
    """Comprehensive sales report filtered by date range"""
    raw_bills = get_filtered_bills()
    df = normalize_bills(raw_bills)
    if df.empty:
        # Return sample data instead of empty response
        print("⚠️  No bills found, returning sample sales data")
        return jsonify(sample_data.get_sample_sales_data()), 200


    # Fetch all products for name and category lookup
    products = list(db.products.find({}))
    products_df = pd.DataFrame(products)

    # ----- Daily -----
    daily = (
        df.groupby(df["date"].dt.date)["revenue"]
        .sum()
        .reset_index()
        .rename(columns={"date": "day", "revenue": "totalSales"})
    )
    daily["day"] = daily["day"].astype(str)

    # ----- Weekly -----
    weekly = (
        df.groupby(df["date"].dt.to_period("W"))["revenue"]
        .sum()
        .reset_index()
    )
    weekly["date"] = weekly["date"].astype(str)
    weekly = weekly.rename(columns={"date": "week", "revenue": "totalSales"})

    # ----- Monthly -----
    monthly = (
        df.groupby(df["date"].dt.to_period("M"))["revenue"]
        .sum()
        .reset_index()
    )
    monthly["date"] = monthly["date"].astype(str)
    monthly = monthly.rename(columns={"date": "month", "revenue": "totalSales"})

    # ----- Top Products (filtered by date range) -----
    merged = df.merge(
        products_df[["productId", "name", "category"]],
        on="productId",
        how="left",
        suffixes=("_bill", "_prod")
    )
    merged["name"] = merged["name_bill"].combine_first(merged["name_prod"])
    merged["category"] = merged["category"].fillna("Unknown")

    top_products = (
        merged.groupby(["productId", "name", "category"])
        .agg({"qty": "sum", "revenue": "sum"})
        .reset_index()
        .sort_values("revenue", ascending=False)
        .head(10)
    )

    # ----- Revenue Trend (filtered by date range) -----
    trend = (
        df.groupby(df["date"].dt.date)["revenue"]
        .sum()
        .reset_index()
        .rename(columns={"date": "day", "revenue": "totalRevenue"})
    )
    trend["day"] = trend["day"].astype(str)

    # ----- Combine Report -----
    report = {
        "daily": daily.to_dict(orient="records"),
        "weekly": weekly.to_dict(orient="records"),
        "monthly": monthly.to_dict(orient="records"),
        "top_products": top_products.to_dict(orient="records"),
        "revenue_trend": trend.to_dict(orient="records"),
        "summary": {
            "total_revenue": float(df["revenue"].sum()),
            "total_orders": int(len(raw_bills)),
            "total_products_sold": int(df["qty"].sum())
        }
    }

    print(f"📈 Report generated with {len(top_products)} top products")
    return jsonify(report)


@app.route("/analytics/report/text")
def sales_report_text():
    try:
        # --- NEW: Get language from header ---
        # Get 'en', 'hi', 'mr', 'te' from 'hi-IN,hi;q=0.9,en-US;q=0.8,en;q=0.7'
        lang = request.headers.get('Accept-Language', 'en').split(',')[0].split('-')[0]
        # --- End NEW ---

        raw_bills = get_filtered_bills()
        
        shop_id = request.args.get("shopId")
        product_query = {}
        if shop_id:
            product_query["shopId"] = shop_id
            
        products = list(db.products.find(product_query))
        df = normalize_bills(raw_bills)

        if df.empty:
            return jsonify({
                "daily": [],
                "weekly": [],
                "monthly": [],
                "top_products": [],
                "revenue_trend": [],
                "summary": {
                    "total_revenue": 0,
                    "total_orders": 0,
                    "total_products_sold": 0
                },
                "message": "No billing data"
            }), 200

        # Create products DataFrame robustly
        if not products:
            products_df = pd.DataFrame(columns=["productId", "name", "category"])
        else:
            products_df = pd.DataFrame(products)
            
        # Ensure required columns exist
        for col in ["productId", "name", "category"]:
            if col not in products_df.columns:
                products_df[col] = None

        # ---- Summary ----
        total_revenue = float(df["revenue"].sum())
        total_orders = int(len(raw_bills))
        total_products_sold = int(df["qty"].sum())

        # ---- Daily ----
        daily = (
            df.groupby(df["date"].dt.date)["revenue"]
            .sum()
            .reset_index()
            .rename(columns={"date": "day", "revenue": "totalSales"})
        )
        daily["day"] = daily["day"].astype(str)
        if daily.empty:
             best_day_str = "N/A"
             best_day_val = 0.0
        else:
             best_day = daily.loc[daily["totalSales"].idxmax()]
             best_day_str = best_day['day']
             best_day_val = best_day['totalSales']

        # ---- Weekly ----
        weekly = (
            df.groupby(df["date"].dt.to_period("W"))["revenue"]
            .sum()
            .reset_index()
        )
        weekly = weekly.rename(columns={"date": "week", "revenue": "totalSales"})
        if weekly.empty:
            best_week_str = "N/A"
            best_week_val = 0.0
        else:
            best_week = weekly.loc[weekly["totalSales"].idxmax()]
            best_week_str = best_week['week']
            best_week_val = best_week['totalSales']

        # ---- Monthly ----
        monthly = (
            df.groupby(df["date"].dt.to_period("M"))["revenue"]
            .sum()
            .reset_index()
        )
        monthly["date"] = monthly["date"].astype(str)
        monthly = monthly.rename(columns={"date": "month", "revenue": "totalSales"})
        if monthly.empty:
            best_month_str = "N/A"
            best_month_val = 0.0
        else:
             best_month = monthly.loc[monthly["totalSales"].idxmax()]
             best_month_str = best_month['month']
             best_month_val = best_month['totalSales']

        # ---- Top Products ----
        merged = df.merge(
            products_df[["productId", "name", "category"]],
            on="productId",
            how="left",
            suffixes=("_bill", "_prod")
        )
        # Handle cases where columns might be all NaN if merge failed
        if "name_bill" not in merged.columns: merged["name_bill"] = merged["name"]
        if "name_prod" not in merged.columns: merged["name_prod"] = None

        merged["name"] = merged["name_bill"].combine_first(merged["name_prod"])
        merged["category"] = merged["category"].fillna("Unknown")

        top_products = (
            merged.groupby(["productId", "name", "category"])
            .agg({"qty": "sum", "revenue": "sum"})
            .reset_index()
            .sort_values("revenue", ascending=False)
            .head(3)
        )

        # ---- Generate Plain Text Report (TRANSLATED) ----
        report_lines = []
        report_lines.append(t(lang, 'report_title'))
        report_lines.append(t(lang, 'total_revenue').format(total_revenue=total_revenue))
        report_lines.append(t(lang, 'total_orders').format(total_orders=total_orders))
        report_lines.append(t(lang, 'total_products_sold').format(total_products_sold=total_products_sold))
        
        # Using variables extracted safely above
        report_lines.append(t(lang, 'best_day').format(best_day=best_day_str, total_sales=best_day_val))
        report_lines.append(t(lang, 'best_week').format(best_week=best_week_str, revenue=best_week_val))
        report_lines.append(t(lang, 'best_month').format(best_month=best_month_str, revenue=best_month_val))

        report_lines.append(t(lang, 'top_products_title'))
        for _, row in top_products.iterrows():
            report_lines.append(
                t(lang, 'top_product_line').format(name=row['name'], category=row['category'], qty=row['qty'], revenue=row['revenue'])
            )
        
        return jsonify({"report": "\n".join(report_lines)}), 200

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e), "trace": traceback.format_exc()}), 500
    # --- End of modifications ---

    # -----------------------
# ML / Prediction Endpoints
# -----------------------

@app.route("/analytics/predict-price", methods=['GET'])
def predict_price():
    shop_id = request.args.get("shopId")
    product_id = request.args.get("productId")
    
    if not shop_id or not product_id:
        return jsonify({"error": "Missing shopId or productId"}), 400
        
    # Try to get from cache first
    cached = db.price_predictions.find_one({
        "shopId": shop_id,
        "productId": product_id
    })
    
    # If cached and less than 24 hours old, return it
    if cached and (datetime.utcnow() - cached.get('lastUpdated', datetime.min)).total_seconds() < 86400:
        return jsonify({
            "trend": cached['trend'],
            "predictedPrice": cached['predictedPrice'],
            "graphBase64": cached['graphBase64'],
            "points": cached.get('points', [])
        })
        
    # Otherwise calculate fresh
    try:
        prediction = generate_prediction(shop_id, product_id)
        if prediction and "error" not in prediction:
            # Cache it
            db.price_predictions.update_one(
                {"shopId": shop_id, "productId": product_id},
                {"$set": prediction},
                upsert=True
            )
            return jsonify(prediction)
        else:
            # Insufficient data - return sample prediction
            print(f"⚠️  Insufficient data for {product_id}, returning sample data")
            sample_prediction = sample_data.get_sample_price_prediction(product_id)
            return jsonify(sample_prediction), 200
            
    except Exception as e:
        print(f"❌ Prediction error: {e}")
        return jsonify({"error": str(e)}), 500
        return jsonify({"error": str(e)}), 500


@app.route("/analytics/inventory-intelligence", methods=['GET'])
def inventory_intelligence():
    shop_id = request.args.get("shopId")
    product_id = request.args.get("productId")
    current_stock = request.args.get("currentStock", 0)

    if not shop_id or not product_id:
        return jsonify({"error": "Missing shopId or productId"}), 400

    try:
        result = calculate_inventory_intelligence(shop_id, product_id, current_stock)
        return jsonify(result), 200
    except Exception as e:
        print(f"❌ Inventory intelligence error: {e}")
        return jsonify({"error": str(e)}), 500


@app.route("/analytics/update-model", methods=['POST'])
def update_model():
    data = request.json
    shop_id = data.get("shopId")
    product_ids = data.get("productIds", [])
    
    if not shop_id or not product_ids:
        return jsonify({"error": "Missing shopId or productIds"}), 400
        
    count = 0
    # In a real heavy-load system, this should likely be pushed to a Celery queue.
    # For this assignment, we'll process synchronously or expect the caller to fire-and-forget,
    # or keep it fast enough if data isn't huge.
    # However, since the user asked for "Online Learning" where we refit,
    # and "Update mechanism where the model refits", we do it here.
    
    print(f"🔄 Updating models for {len(product_ids)} products...")
    
    for pid in product_ids:
        try:
            prediction = generate_prediction(shop_id, pid)
            if prediction:
                db.price_predictions.update_one(
                    {"shopId": shop_id, "productId": pid},
                    {"$set": prediction},
                    upsert=True
                )
                count += 1
        except Exception as e:
            print(f"⚠️ Failed to update model for {pid}: {e}")
            
    return jsonify({"message": f"Updated models for {count} products"}), 200

# -----------------------
# Run Server
# -----------------------


@app.route("/clear-cache", methods=['POST'])
def clear_cache():
    # Clear all price predictions to force re-calculation
    result = db.price_predictions.delete_many({})
    print(f"🧹 Cleared {result.deleted_count} cached predictions.")
    return jsonify({"message": f"Cleared {result.deleted_count} cached predictions"}), 200
def seed_data_endpoint():
    target_email = "sambhavkumath1357@gmail.com"
    print(f"🌱 Seeding data for {target_email}...")
    
    user = db.users.find_one({"email": target_email})
    if not user:
        return jsonify({"error": f"User {target_email} not found"}), 404
    
    shop_id = user.get("shopId")
    if not shop_id:
        return jsonify({"error": "User has no shopId"}), 400
        
    product = db.products.find_one({"shopId": shop_id})
    if not product:
        return jsonify({"error": "No products found for this shop"}), 404
        
    product_id = str(product.get("productId") or product.get("_id"))
    product_name = product.get("name")
    base_price = product.get("price", 100)
    
    bills_to_insert = []
    import random
    from datetime import timedelta
    
    for i in range(20):
        date_offset = 20 - i
        bill_date = datetime.now() - timedelta(days=date_offset)
        
        # Upward trend simulation
        price_variation = (i * 2) + random.uniform(-5, 5)
        final_price = float(base_price + price_variation)
        if final_price < 10: final_price = 10.0
        
        qty = random.randint(1, 5)
        total = final_price * qty
        
        bill = {
            "shopId": shop_id,
            "billId": f"SEED_{int(bill_date.timestamp())}_{i}",
            "customer": {
                "name": "Test Customer",
                "mobile": "9999999999",
                "email": "test@example.com"
            },
            "items": [
                {
                    "productId": product_id,
                    "name": product_name,
                    "price": final_price,
                    "qty": qty,
                    "itemTotal": total
                }
            ],
            "totalAmount": total,
            "paymentMode": "CASH",
            "createdAt": bill_date,
            "updatedAt": bill_date,
            "addedBy": target_email
        }
        bills_to_insert.append(bill)
        
    if bills_to_insert:
        db.bills.insert_many(bills_to_insert)
        return jsonify({"message": f"inserted {len(bills_to_insert)} bills", "product": product_name}), 200
    
    return jsonify({"message": "No bills generated"}), 200

# -----------------------
# Run Server
# -----------------------

# -----------------------
# RECOMMENDATION ENGINE (Apriori)
# -----------------------

# GLOBAL CACHE FOR RULES
ASSOCIATION_RULES = {} # { shopId: DataFrame }
LAST_TRAINED = {} # { shopId: timestamp }

# -----------------------
# RECOMMENDATION ENGINE (Apriori)
# -----------------------

def train_for_shop(shop_id):
    """
    Train Apriori model for a specific shop.
    """
    global ASSOCIATION_RULES, LAST_TRAINED
    print(f" [Recommender] Starting Training for Shop: {shop_id}...")
    
    try:
        # 1. Fetch Bills for this Shop
        pipeline = [
            { "$match": { "shopId": shop_id, "items": { "$exists": True, "$ne": [] } } },
            { "$project": { "items.productId": 1 } }
        ]
        cursor = db.bills.aggregate(pipeline)
        
        transactions = []
        for doc in cursor:
            if 'items' in doc:
                basket = [item.get('productId') for item in doc['items'] if item.get('productId')]
                if len(basket) > 0:
                    transactions.append(basket)
                    
        print(f" [Recommender] Shop {shop_id}: Found {len(transactions)} baskets.")
        
        if len(transactions) < 5:
            print(f" [Recommender] Shop {shop_id}: Not enough data.")
            ASSOCIATION_RULES[shop_id] = None
            return

        # 2. One-Hot Encoding
        te = TransactionEncoder()
        te_ary = te.fit(transactions).transform(transactions)
        df_trans = pd.DataFrame(te_ary, columns=te.columns_)
        
        # 3. Apriori 
        # Low support to find any patterns in small data, higher in prod
        min_sup = 0.01 if len(transactions) > 100 else 0.05
        frequent_itemsets = apriori(df_trans, min_support=min_sup, use_colnames=True)
        
        if frequent_itemsets.empty:
             print(f" [Recommender] Shop {shop_id}: No frequent patterns.")
             ASSOCIATION_RULES[shop_id] = None
             return

        # 4. Association Rules (Lift > 1.0)
        rules = association_rules(frequent_itemsets, metric="lift", min_threshold=1.0)
        
        if not rules.empty:
            rules['antecedents'] = rules['antecedents'].apply(list)
            rules['consequents'] = rules['consequents'].apply(list)
            
            ASSOCIATION_RULES[shop_id] = rules
            LAST_TRAINED[shop_id] = datetime.utcnow()
            print(f" [Recommender] Shop {shop_id}: Generated {len(rules)} rules.")
        else:
            ASSOCIATION_RULES[shop_id] = None

    except Exception as e:
        print(f" [Recommender] Shop {shop_id} Training Failed: {e}")

def train_association_rules_task():
    """
    Background Task: Train models for ALL shops.
    """
    # Find all distinct shopIds
    shops = db.bills.distinct("shopId")
    print(f" [Recommender] Training needed for {len(shops)} shops.")
    for shop_id in shops:
        if shop_id:
            train_for_shop(shop_id)

@app.route('/analytics/train-recommendations', methods=['POST'])
def trigger_training():
    thread = threading.Thread(target=train_association_rules_task)
    thread.start()
    return jsonify({"status": "Global Training started", "time": datetime.utcnow()})

@app.route('/analytics/recommend', methods=['POST', 'GET'])
def get_recommendations():
    """
    Input: { "cart": ["p1", "p2"], "shopId": "..." } or GET params
    """
    # Handle GET (query params) or POST (json)
    if request.method == 'GET':
        shop_id = request.args.get('shopId')
        cart_str = request.args.get('currentCart', '') # comma separated
        cart = cart_str.split(',') if cart_str else []
    else:
        data = request.json
        cart = data.get('cart', [])
        shop_id = data.get('shopId')

    if not shop_id:
        return jsonify({"error": "shopId required", "recommendations": []})

    rules = ASSOCIATION_RULES.get(shop_id)
    cart_set = set(cart)
    
    if rules is None:
        # Check if shop has ANY bills first
        bill_count = db.bills.count_documents({"shopId": shop_id})
        
        if bill_count == 0:
            # No bills at all - return sample recommendations
            print(f" [Recommender] No bills for shop {shop_id}. Returning sample recommendations.")
            return jsonify(sample_data.get_sample_recommendations(cart))
        
        # Fallback to Trending (Cold Start)
        print(f" [Recommender] Cold Start for Shop {shop_id}. Fetching Trending.")
        pipeline = [
            { "$match": { "shopId": shop_id } },
            { "$unwind": "$items" },
            { "$group": { "_id": "$items.productId", "count": { "$sum": 1 } } },
            { "$sort": { "count": -1 } },
            { "$limit": 5 }
        ]
        trending = list(db.bills.aggregate(pipeline))
        hits = []
        for t in trending:
            pid = t['_id']
            if pid and pid not in cart_set:
                 hits.append({
                    "productId": pid,
                    "reason": "🔥 Hot Seller (Trending)",
                    "confidence": 0.0,
                    "lift": 0.0
                })
        # Skip rule filtering
    else:
        # Rules exist, check for matches
        hits = []
        cart_set = set(cart)
        
        for idx, rule in rules.iterrows():
            antecedents = set(rule['antecedents'])
            if antecedents.issubset(cart_set):
                consequents = rule['consequents']
                for rec_id in consequents:
                    if rec_id not in cart_set:
                        hits.append({
                            "productId": rec_id,
                            "reason": "Frequently bought together",
                            "confidence": float(rule['confidence']),
                            "lift": float(rule['lift'])
                        })
        
        # If no hits found in rules, fallback to Trending
        if not hits:
            print(f" [Recommender] No rules matched cart. Fetching Trending.")
            # 1. Try Shop-Specific Trending
            pipeline = [
                { "$match": { "shopId": shop_id } },
                { "$unwind": "$items" },
                { "$group": { "_id": "$items.productId", "count": { "$sum": 1 } } },
                { "$sort": { "count": -1 } },
                { "$limit": 5 }
            ]
            trending = list(db.bills.aggregate(pipeline))
            
            # 2. If Shop invalid/empty, Try GLOBAL DEMO SHOP (The Seeded One)
            if not trending:
                 print(f" [Recommender] Shop has no bills. Using DEMO Fallback.")
                 demo_shop_id = "691edbe52388451c9a800cc8"
                 pipeline[0]["$match"]["shopId"] = demo_shop_id
                 trending = list(db.bills.aggregate(pipeline))

            for t in trending:
                pid = t['_id']
                if pid and pid not in cart_set:
                     hits.append({
                        "productId": pid,
                        "reason": "🔥 Hot Seller (Global)",
                        "confidence": 0.0,
                        "lift": 0.0
                    })
                    
    # Deduplicate
    unique_hits = {}
    for h in hits:
        pid = h['productId']
        if pid not in unique_hits or h['confidence'] > unique_hits[pid]['confidence']:
            unique_hits[pid] = h
            
    sorted_recs = sorted(unique_hits.values(), key=lambda x: x['lift'], reverse=True)[:5]
    
    # Enrichment
    enriched = []
    for rec in sorted_recs:
        # Search in product collection. 
        # CAUTION: If user doesn't have these products in their shop, this might fail to find name/price.
        # But wait, seed_rec_data added products to the TARGET shop. 
        # If user is different, they might not have "P_BREAD".
        # So we should only suggest items that EXIST in the CURRENT shop ? 
        # OR we assume the user has imported the seed products ?
        # Let's try finding in CURRENT shop first.
        prod = db.products.find_one({"productId": rec['productId'], "shopId": shop_id})
        
        # If not found, and we used Demo Fallback, find in Demo Shop to at least show the name?
        # No, frontend filters by "displayProducts". If user doesn't have P_BREAD, they won't see it.
        # But seed_rec_data upserted products to specific shop.
        # WE NEED TO ENSURE USER HAS P_BREAD.
        # Assuming user ran seed, or we are just showing "Global" recs.
        
        if not prod:
             # Try generic lookup (any shop) just to get metadata
             prod = db.products.find_one({"productId": rec['productId']})
        
        if prod:
            stock = prod.get('quantity', 0)
            rec['name'] = prod.get('name', 'Unknown')
            rec['price'] = prod.get('price', 0)
            rec['stock'] = stock
            enriched.append(rec)
            
    return jsonify({
        "recommendations": enriched,
        "meta": { "rule_count": len(rules) if rules is not None else 0 }
    })

# Start training on boot (delayed)
def boot_training():
    time.sleep(10)
    train_association_rules_task()

threading.Thread(target=boot_training).start()

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)
