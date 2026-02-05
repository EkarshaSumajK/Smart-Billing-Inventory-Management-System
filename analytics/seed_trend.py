import os
import random
from datetime import datetime, timedelta
from pymongo import MongoClient
from dotenv import load_dotenv

# Load env variables
load_dotenv()

MONGO_URI = os.getenv("MONGO_URI") or "mongodb+srv://admin:Sambhav%40123@cluster0.sjgksuv.mongodb.net/smartretail?retryWrites=true&w=majority&tls=true"
DB_NAME = os.getenv("DB_NAME") or "smartretail"

print(f"🔌 Connecting to {DB_NAME}...")
client = MongoClient(MONGO_URI)
db = client[DB_NAME]

def seed_upward_trend():
    target_email = "sambhavkumath1357@gmail.com"
    user = db.users.find_one({"email": target_email})
    
    if not user:
        print("❌ User not found!")
        return

    shop_id = user["shopId"]
    product_id = "1001"
    product_name = "Laptop"
    
    print(f"✅ Seeding TRENDING UP data for Shop: {shop_id}")

    # 1. Clear existing bills for this product to avoid noise
    # db.bills.delete_many({"shopId": shop_id, "items.productId": product_id})
    # Actually, let's keep them but add NEW ones that are recent and higher price.
    
    base_price = 15000
    today = datetime.now()
    
    bills_to_insert = []
    
    # Generate 20 days of data, increasing price by 5% every 5 days
    # Day 1-20
    for i in range(20):
        date = today - timedelta(days=(20 - i))
        
        # Linear increase: 15000 -> 18000
        # Increase by 150 Rs per day = 3000 Rs over 20 days = 20% increase
        daily_increase = 200 * i 
        price = base_price + daily_increase
        
        # Add some noise
        price += random.uniform(-50, 50)
        
        bill = {
            "shopId": shop_id,
            "date": date,
            "createdAt": date, # Important for sorting
            "totalAmount": price,
            "items": [
                {
                    "productId": product_id,
                    "name": product_name,
                    "qty": 1,
                    "price": price
                }
            ],
            "customerName": "Trend Tester",
            "paymentMode": "CASH"
        }
        bills_to_insert.append(bill)

    if bills_to_insert:
        db.bills.insert_many(bills_to_insert)
        print(f"🚀 Inserted {len(bills_to_insert)} bills with INCREASING price (15000 -> {price:.2f})")
        
    # 2. Clear Cache
    db.price_predictions.delete_many({"shopId": shop_id})
    print("🧹 Cleared prediction cache.")

if __name__ == "__main__":
    seed_upward_trend()
