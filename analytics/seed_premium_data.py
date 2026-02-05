from pymongo import MongoClient
import datetime
import random
import math
import os
from dotenv import load_dotenv

load_dotenv()

# Configuration
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/smart_retail")
SHOP_ID = "691edbe52388451c9a800cc8"  # The shop we are testing with

# Connect
client = MongoClient(MONGO_URI)
db = client.get_database()
print(f"Connected to {db.name}")

# Demo Products to Create
demos = [
    {
        "name": "Tech Corp Stock (Growth)",
        "category": "Electronics",
        "pattern": "growth",
        "base_price": 1000.0,
        "volatility": 0.05
    },
    {
        "name": "Crypto Coin (Crash)",
        "category": "Digital",
        "pattern": "crash",
        "base_price": 5000.0,
        "volatility": 0.15
    },
    {
        "name": "Commodity (Stable)",
        "category": "Raw Materials",
        "pattern": "sine",
        "base_price": 500.0,
        "volatility": 0.02
    }
]

# Create Products and Generate Bills
bills_batch = []
start_date = datetime.datetime.now() - datetime.timedelta(days=90)

print("Creating Demo Products and History...")

for demo in demos:
    # 1. Create Product
    product_data = {
        "name": demo["name"],
        "category": demo["category"],
        "price": demo["base_price"], # Current price
        "quantity": 100,
        "shopId": SHOP_ID,
        "description": "Premium Demo Product for Analytics"
    }
    
    # Check if exists to avoid duplicates, or just insert new
    # For demo purity, let's insert new and get ID
    result = db.products.insert_one(product_data)
    p_id = str(result.inserted_id)
    print(f"Created {demo['name']} with ID: {p_id}")
    
    # 2. Generate 90 Days of History
    current_price = demo["base_price"]
    
    for day_offset in range(90):
        date = start_date + datetime.timedelta(days=day_offset)
        
        # Calculate Price based on Pattern
        t = day_offset
        if demo["pattern"] == "growth":
            # Linear growth + random noise
            trend = 1 + (0.01 * t) # 1% growth per day
            noise = random.uniform(1 - demo["volatility"], 1 + demo["volatility"])
            daily_price = demo["base_price"] * trend * noise
            
        elif demo["pattern"] == "crash":
            # Pump then Dump
            if t < 45:
                trend = 1 + (0.02 * t)
            else:
                trend = 1.9 - (0.04 * (t - 45)) # Crash fast
            noise = random.uniform(1 - demo["volatility"], 1 + demo["volatility"])
            daily_price = demo["base_price"] * trend * noise
            
        elif demo["pattern"] == "sine":
            # Sine wave
            trend = 1 + 0.2 * math.sin(t * 0.2)
            noise = random.uniform(1 - demo["volatility"], 1 + demo["volatility"])
            daily_price = demo["base_price"] * trend * noise
            
        # Ensure non-negative
        daily_price = max(10.0, daily_price)
        
        # Generate 1-5 bills per day per product
        num_sales = random.randint(1, 4)
        for _ in range(num_sales):
            qty = random.randint(1, 2)
            bill = {
                "shopId": SHOP_ID,
                "customerName": "Demo Trader",
                "customerPhone": "555-0199",
                "totalAmount": daily_price * qty,
                "date": date,
                "items": [
                    {
                        "productId": p_id,
                        "name": demo["name"],
                        "category": demo["category"],
                        "quantity": qty,
                        "price": daily_price, # Historical price
                        "subTotal": daily_price * qty
                    }
                ]
            }
            bills_batch.append(bill)

# Bulk Insert Bills
if bills_batch:
    db.bills.insert_many(bills_batch)
    print(f"Successfully inserted {len(bills_batch)} historical bills covering 90 days.")
else:
    print("No bills generated.")

print("Seed Complete.")
