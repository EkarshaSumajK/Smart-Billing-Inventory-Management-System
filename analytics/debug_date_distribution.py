from pymongo import MongoClient
import pandas as pd
import os

# FORCE LOCALHOST
MONGO_URI = "mongodb://localhost:27017/smart_retail"
client = MongoClient(MONGO_URI)
db = client.get_database()

product = db.products.find_one({"name": {"$regex": "laptop", "$options": "i"}})
if not product:
    print("❌ No laptop found")
    exit()

p_id = str(product.get("productId") or product.get("_id"))
shop_id = product.get("shopId")

print(f"Checking distribution for: {product['name']} (ID: {p_id}, Shop: {shop_id})")

pipeline = [
    { "$match": { "shopId": shop_id, "items.productId": p_id } },
    { "$unwind": "$items" },
    { "$match": { "items.productId": p_id } },
    {
        "$group": {
            "_id": { "date": { "$dateToString": { "format": "%Y-%m-%d", "date": { "$ifNull": ["$createdAt", "$date"] } } } },
            "count": { "$sum": 1 },
            "totalQty": { "$sum": { "$ifNull": ["$items.quantity", "$items.qty"] } }
        }
    },
    { "$sort": { "_id.date": 1 } }
]

results = list(db.bills.aggregate(pipeline))

print(f"{'Date':<15} | {'Count':<5} | {'Qty':<5}")
print("-" * 30)
if not results:
    print("❌ No bills found for this product/shop combo!")

for r in results:
    print(f"{r['_id']['date']:<15} | {r['count']:<5} | {r['totalQty']:<5}")

print(f"\nTotal Unique Days: {len(results)}")
