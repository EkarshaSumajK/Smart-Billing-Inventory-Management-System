import os
import random
from datetime import datetime, timedelta
from pymongo import MongoClient
from dotenv import load_dotenv

load_dotenv()

# Config
MONGO_URI = os.getenv("MONGO_URI")
DB_NAME = os.getenv("DB_NAME")
TARGET_EMAIL = "sambhavkumath1357@gmail.com"

print(f"🔌 Connecting to {DB_NAME}...")
# print(f"DEBUG: URI starts with {MONGO_URI[:15]}...") 
client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000) # 5s timeout
db = client[DB_NAME]

def seed_data():
    try:
        print("   -> Pinging server...")
        client.admin.command('ping')
        print("   -> Connected!")
    except Exception as e:
        print(f"❌ Connection failed: {e}")
        return

    # 1. Find User & Shop
    user = db.users.find_one({"email": TARGET_EMAIL})
    if not user:
        print(f"❌ User {TARGET_EMAIL} not found!")
        return
    
    shop_id = user.get("shopId")
    if not shop_id:
        print(f"❌ User has no shopId associated!")
        return
        
    print(f"✅ Found User. Shop ID: {shop_id}")

    # 2. Find a Product to sell
    product = db.products.find_one({"shopId": shop_id})
    if not product:
        print("❌ No products found for this shop. Create a product first.")
        return
        
    product_id = str(product.get("productId") or product.get("_id"))
    product_name = product.get("name")
    base_price = product.get("price", 100)
    
    print(f"🎯 Target Product: {product_name} (ID: {product_id}) Base Price: {base_price}")

    # 3. Generate 20 days of Bills
    bills_to_insert = []
    
    print("📅 Generating bills for the last 20 days...")
    for i in range(20):
        # Date from 20 days ago up to today
        date_offset = 20 - i
        bill_date = datetime.now() - timedelta(days=date_offset)
        
        # Randomize price slightly to create a trend
        # Let's simulate an UPWARD trend
        price_variation = (i * 2) + random.uniform(-5, 5) # Price increases over time
        final_price = base_price + price_variation
        if final_price < 10: final_price = 10
        
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
            "addedBy": TARGET_EMAIL
        }
        bills_to_insert.append(bill)

    if bills_to_insert:
        db.bills.insert_many(bills_to_insert)
        print(f"🚀 Successfully inserted {len(bills_to_insert)} bills for {product_name}!")
        print("💡 The analytics service should now show a price trend.")
    else:
        print("⚠️ No bills generated.")

if __name__ == "__main__":
    seed_data()
