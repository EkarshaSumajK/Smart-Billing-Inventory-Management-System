from pymongo import MongoClient
import datetime
import random
import math
import os
from dotenv import load_dotenv

load_dotenv()

# Configuration
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/smart_retail")
client = MongoClient(MONGO_URI)
db = client.get_database()
print(f"Connected to {db.name}")

# Get all products
products = list(db.products.find({}))
print(f"Found {len(products)} products to seed.")

# 180 Days of History
start_date = datetime.datetime.now() - datetime.timedelta(days=180)
bills_batch = []
BATCH_SIZE = 1000

patterns = ["bull", "bear", "seasonal", "pump_dump", "stable", "volatile"]

for idx, product in enumerate(products):
    pattern = random.choice(patterns)
    base_price = product.get("price", 100.0)
    if base_price < 10: base_price = 100.0
    
    p_id = str(product.get("productId") or product.get("_id"))
    shop_id = product.get("shopId")
    name = product.get("name")
    category = product.get("category", "General")
    
    print(f"[{idx+1}/{len(products)}] Seeding '{name}' with pattern '{pattern}'")
    
    current_price = base_price
    
    for day_offset in range(180):
        t = day_offset
        date = start_date + datetime.timedelta(days=day_offset)
        
        # --- Mathematical Pattern Logic ---
        if pattern == "bull":
            # Linear Growth + 40% over 180 days
            trend = 1 + (0.002 * t) 
            noise = random.uniform(0.95, 1.05)
            daily_price = base_price * trend * noise
            
        elif pattern == "bear":
            # Slow decline
            trend = 1 - (0.0015 * t)
            noise = random.uniform(0.95, 1.05)
            daily_price = base_price * trend * noise
            
        elif pattern == "seasonal":
            # Sine wave (period 30 days)
            trend = 1 + 0.15 * math.sin(t * (2 * math.pi / 30))
            noise = random.uniform(0.98, 1.02)
            daily_price = base_price * trend * noise
            
        elif pattern == "pump_dump":
            # Spike at day 150-160
            if 150 <= t <= 160:
                trend = 2.0
            elif t > 160:
                trend = 0.8 # Crash below baseline
            else:
                trend = 1.0 + (0.001 * t)
            noise = random.uniform(0.9, 1.1)
            daily_price = base_price * trend * noise

        elif pattern == "stable":
            # Low volatility
            daily_price = base_price * random.uniform(0.99, 1.01)

        elif pattern == "volatile":
            # Crypto style
            noise = random.uniform(0.85, 1.15)
            daily_price = base_price * noise
            
        # Bounds check
        if daily_price < 5: daily_price = 5.0
        
        # 30% chance of NO sale on a given day (realistic sparseness)
        if random.random() > 0.7:
            continue
            
        # Create Bill
        qty = random.randint(1, 4)
        bill = {
            "shopId": shop_id,
            "customerName": "Auto Seeder",
            "date": date,
            "createdAt": date, # Dual fields for safety
            "totalAmount": daily_price * qty,
            "items": [
                {
                    "productId": p_id,
                    "name": name,
                    "category": category,
                    "quantity": qty,
                    "qty": qty, # Legacy support
                    "price": daily_price
                }
            ]
        }
        bills_batch.append(bill)
        
        if len(bills_batch) >= BATCH_SIZE:
            db.bills.insert_many(bills_batch)
            bills_batch = []
            print("  -> Flushed batch to DB")

# Flush remaining
if bills_batch:
    db.bills.insert_many(bills_batch)
    print(f"  -> Flushed final {len(bills_batch)} records.")

print("Ultra Seeding Complete.")
