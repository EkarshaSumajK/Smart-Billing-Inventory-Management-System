import os
from pymongo import MongoClient
from dotenv import load_dotenv
import sys

# Load env from .env file which is in the same directory
load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")
DB_NAME = os.getenv("DB_NAME")
TARGET_EMAIL = "sambhavkumath1357@gmail.com"

if not MONGO_URI:
    print("❌ MONGO_URI not found in environment")
    sys.exit(1)

print(f"🔌 Connecting to {DB_NAME}...")
client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
db = client[DB_NAME]

def check_data():
    try:
        client.admin.command('ping')
        print("   -> Connected to MongoDB!")
    except Exception as e:
        print(f"❌ Connection failed: {e}")
        return

    # 1. Check User
    user = db.users.find_one({"email": TARGET_EMAIL})
    if not user:
        print(f"❌ User {TARGET_EMAIL} NOT FOUND")
        return
    
    shop_id = user.get("shopId")
    print(f"✅ User found. ShopID: {shop_id}")

    # 2. Check Bills for this Shop
    bill_count = db.bills.count_documents({"shopId": shop_id})
    print(f"📊 Total Bills for Shop {shop_id}: {bill_count}")
    
    if bill_count > 0:
        last_bill = db.bills.find_one({"shopId": shop_id}, sort=[("createdAt", -1)])
        print(f"   Latest Bill ID: {last_bill.get('billId')}")
        print(f"   Latest Bill Date: {last_bill.get('createdAt')}")
        print(f"   Items in last bill: {last_bill.get('items')}")
        
    # 3. Check Price Prediction Cache
    prediction = db.price_predictions.find_one({"shopId": shop_id})
    if prediction:
        print("✅ Prediction Cache Found:")
        print(f"   Product: {prediction.get('productId')}")
        print(f"   Trend: {prediction.get('trend')}")
        print(f"   Last Updated: {prediction.get('lastUpdated')}")
    else:
        print("❌ No Price Predictions found in cache.")

if __name__ == "__main__":
    check_data()
