from pymongo import MongoClient
import os

# FORCE LOCALHOST - Ignore .env to avoid Atlas connection issues
MONGO_URI = "mongodb://localhost:27017/smart_retail"
client = MongoClient(MONGO_URI)
db = client.get_database()

TARGET_SHOP_ID = "shop_12345"

print(f"🔧 Normalizing Data to Shop ID: {TARGET_SHOP_ID} on {MONGO_URI}")

# 1. Update All Products
res_p = db.products.update_many(
    {},
    {"$set": {"shopId": TARGET_SHOP_ID}}
)
print(f"✅ Updated {res_p.modified_count} products.")

# 2. Update All Bills
res_b = db.bills.update_many(
    {},
    {"$set": {"shopId": TARGET_SHOP_ID}}
)
print(f"✅ Updated {res_b.modified_count} bills.")

# 3. Update All Users (Crucial for frontend token)
res_u = db.users.update_many(
    {},
    {"$set": {"shopId": TARGET_SHOP_ID}}
)
print(f"✅ Updated {res_u.modified_count} users.")

