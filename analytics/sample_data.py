"""
Sample/Demo data for analytics when insufficient real data exists.
Returns zero values as placeholders with messages about data requirements.
"""

from datetime import datetime, timedelta

def get_sample_price_prediction(product_id=None):
    """
    Returns placeholder price prediction with zeros.
    Shows clear message about needing 5+ days of sales.
    """
    # Return zero values as placeholders
    points = []
    for i in range(7):
        date = (datetime.now() + timedelta(days=i)).strftime('%Y-%m-%d')
        points.append({
            'ds': date,
            'yhat': 0,
            'yhat_lower': 0,
            'yhat_upper': 0
        })
    
    return {
        'trend': 'insufficient_data',
        'predictedPrice': 0,
        'graphBase64': '',
        'points': points,
        'is_sample': True,
        'data_required': 5,
        'data_current': 0,
        'message': 'Need at least 5 days of sales history to generate price predictions'
    }


def get_sample_inventory_intelligence(current_stock=0):
    """
    Returns placeholder inventory intelligence with zeros.
    """
    return {
        'status': 'success',
        'velocity': 'unknown',
        'growth_pct': 0,
        'forecast_7d': 0,
        'recommended_stock': 0,
        'stockCoverageDays': 0,
        'points': [],
        'is_sample': True,
        'data_required': 5,
        'data_current': 0,
        'message': 'Need at least 5 days of sales history for inventory analysis'
    }


def get_sample_recommendations(cart=None):
    """
    Returns empty recommendations with data requirement message.
    """
    return {
        'recommendations': [],
        'is_sample': True,
        'data_required': 5,
        'data_current': 0,
        'message': 'Need at least 5 transactions for product recommendations',
        'meta': {
            'rule_count': 0
        }
    }


def get_sample_sales_data():
    """
    Returns zero sales data with data requirement message.
    """
    # Generate empty placeholders
    daily = []
    for i in range(30):
        date = (datetime.now() - timedelta(days=30-i)).strftime('%Y-%m-%d')
        daily.append({
            'day': date,
            'totalSales': 0
        })
    
    weekly = []
    for i in range(4):
        week_start = datetime.now() - timedelta(weeks=4-i)
        weekly.append({
            'week': week_start.strftime('%Y-W%W'),
            'totalSales': 0
        })
    
    monthly = []
    for i in range(3):
        month = datetime.now() - timedelta(days=30*(3-i))
        monthly.append({
            'month': month.strftime('%Y-%m'),
            'totalSales': 0
        })
    
    top_products = []
    revenue_trend = [{'day': d['day'], 'totalRevenue': 0} for d in daily]
    
    summary = {
        'total_revenue': 0,
        'total_orders': 0,
        'total_products_sold': 0
    }
    
    return {
        'daily': daily,
        'weekly': weekly,
        'monthly': monthly,
        'top_products': top_products,
        'revenue_trend': revenue_trend,
        'summary': summary,
        'is_sample': True,
        'data_required': 5,
        'data_current': 0,
        'message': 'Need at least 5 sales to generate analytics reports'
    }
