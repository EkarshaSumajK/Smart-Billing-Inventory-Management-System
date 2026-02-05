import pymongo
from datetime import datetime, timedelta
import random
import os
from dotenv import load_dotenv

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/smart_billing")
client = pymongo.MongoClient(MONGO_URI)
db = client.get_database() # Uses default db from connection string

# 1. Define Product Bundles (Patterns)
BUNDLES = {
    "Breakfast": [
        {"productId": "P_BREAD", "name": "Whole Wheat Bread", "price": 40, "category": "Bakery", "quantity": 100},
        {"productId": "P_BUTTER", "name": "Amul Butter 100g", "price": 56, "category": "Dairy", "quantity": 100},
        {"productId": "P_JAM", "name": "Mixed Fruit Jam", "price": 120, "category": "Pantry", "quantity": 50},
        {"productId": "P_EGGS", "name": "Farm Fresh Eggs (6pcs)", "price": 45, "category": "Pantry", "quantity": 200},
    ],
    "Tech": [
        {"productId": "P_PHONE", "name": "SuperSmart Phone X", "price": 15000, "category": "Electronics", "quantity": 20},
        {"productId": "P_CASE", "name": "Silicone Case for X", "price": 299, "category": "Accessories", "quantity": 50},
        {"productId": "P_SCREEN", "name": "Tempered Glass Guard", "price": 199, "category": "Accessories", "quantity": 100},
        {"productId": "P_EARBUDS", "name": "Wireless Earbuds", "price": 1499, "category": "Electronics", "quantity": 30},
    ],
    "Snacks": [
        {"productId": "P_CHIPS", "name": "Potato Chips Salted", "price": 20, "category": "Snacks", "quantity": 1000},
        {"productId": "P_COKE", "name": "Coca Cola 750ml", "price": 40, "category": "Beverages", "quantity": 500},
        {"productId": "P_DIP", "name": "Cheesy Dip", "price": 85, "category": "Snacks", "quantity": 150},
    ]
}

def seed_recommendation_data():
    print("🌱 Seeding Recommendation Data...")
    
    # A. Upsert Products
    print("   -> Creating Products...")
    all_products = []
    for bundle_name, items in BUNDLES.items():
        for item in items:
            db.products.update_one(
                {"productId": item['productId']},
                {"$set": item},
                upsert=True
            )
            all_products.append(item['productId'])
    print("      ✓ Products Ready.")

    # B. Generate 100 Bills with Patterns
    print("   -> Generating 100 Bills...")
    
    # CORRECT SHOP ID for sambhavkumath1357@gmail.com
    target_shop_id = "691edbe52388451c9a800cc8" 

    # Cleanup PREVIOUS Run (Global Cleanup of seeder data)
    print("   -> Cleaning up old seed data...")
    db.bills.delete_many({"addedBy": "seeder_script"})
    
    # Ensure Products belong to this shop (otherwise they won't appear in search)
    print("   -> Assigning Products to target shop...")
    for pid in all_products:
        db.products.update_one(
            {"productId": pid},
            {"$set": {"shopId": target_shop_id}}
        )

    bills = []
    start_date = datetime.utcnow() - timedelta(days=30)

    for i in range(100):
        # Pick a primary bundle logic
        r = random.random()
        
        cart_items = []
        
        if r < 0.4: # 40% Breakfast
            # Strong pattern: Bread + Butter
            cart_items.append("P_BREAD")
            cart_items.append("P_BUTTER")
            if random.random() < 0.5: cart_items.append("P_JAM")
            if random.random() < 0.3: cart_items.append("P_EGGS")
            
        elif r < 0.7: # 30% Tech
            # Pattern: Phone + (Case OR Screen)
            cart_items.append("P_PHONE")
            if random.random() < 0.8: cart_items.append("P_CASE")
            if random.random() < 0.8: cart_items.append("P_SCREEN")
            
        else: # 30% Snacks
            # Pattern: Chips + Coke
            cart_items.append("P_CHIPS")
            cart_items.append("P_COKE")
            if random.random() < 0.2: cart_items.append("P_DIP")

        # Noise: Add random unrelated item sometimes
        if random.random() < 0.1:
            cart_items.append(random.choice(all_products))

        # Build Bill Object
        bill_items = []
        total_amt = 0
        for pid in set(cart_items): # Dedupe
            # Find price
            p_data = None
            for b_list in BUNDLES.values():
                for p in b_list:
                    if p['productId'] == pid: p_data = p
            
            qty = random.randint(1, 3)
            price = p_data['price']
            total_amt += price * qty
            
            bill_items.append({
                "productId": pid,
                "name": p_data['name'],
                "qty": qty,
                "price": price,
                "itemTotal": price * qty
            })

        bill_date = start_date + timedelta(days=random.randint(0, 30), hours=random.randint(9, 21))
        
        bill = {
            "billId": f"REC_SEED_{1000+i}",
            "shopId": target_shop_id,
            "items": bill_items,
            "totalAmount": total_amt,
            "customer": {"mobile": "9999999999", "name": "Test User", "email": "test@example.com"},
            "createdAt": bill_date,
            "paymentMode": "CASH",
            "addedBy": "seeder_script"
        }
        bills.append(bill)

    # Bulk Insert
    if bills:
        try:
            db.bills.insert_many(bills)
            print(f"      ✓ Inserted {len(bills)} Bills for Shop {target_shop_id}.")
        except Exception as e:
            print("Error inserting bills:", e)

    print("✅ Seeding Complete!")

if __name__ == "__main__":
    seed_recommendation_data()
