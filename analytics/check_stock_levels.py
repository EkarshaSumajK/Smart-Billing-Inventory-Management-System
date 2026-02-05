from pymongo import MongoClient
import pandas as pd
from prophet import Prophet
import os

# FORCE LOCALHOST
MONGO_URI = "mongodb://localhost:27017/smart_retail"
client = MongoClient(MONGO_URI)
db = client.get_database()

products = db.products.find({})

print(f"{'Product':<20} | {'Stock':<5} | {'7-Day Forecast':<15} | {'Rec Stock':<10}")
print("-" * 60)

for p in products:
    p_id = str(p.get("productId") or p.get("_id"))
    shop_id = p.get("shopId")
    stock = p.get("quantity", 0)
    
    # Get Sales History
    pipeline = [
        { "$match": { "shopId": shop_id, "items.productId": p_id } },
        { "$unwind": "$items" },
        { "$match": { "items.productId": p_id } },
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
        print(f"{p['name'][:20]:<20} | {stock:<5} | {'Not Enough Data':<15} | -")
        continue

    data = [{'ds': r['_id']['date'], 'y': r['totalQty']} for r in results]
    df = pd.DataFrame(data)
    
    try:
        m = Prophet(daily_seasonality=False, yearly_seasonality=False, weekly_seasonality=False)
        m.fit(df)
        future = m.make_future_dataframe(periods=7)
        forecast = m.predict(future)
        next_7 = forecast.tail(7)['yhat'].sum()
        
        needed = next_7 * 1.2
        rec = max(0, int(needed - stock))
        
        print(f"{p['name'][:20]:<20} | {stock:<5} | {round(next_7, 1):<15} | {rec:<10}")
        
    except Exception as e:
        print(f"Error for {p['name']}: {e}")
