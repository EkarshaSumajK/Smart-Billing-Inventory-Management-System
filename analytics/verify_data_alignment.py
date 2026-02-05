from pymongo import MongoClient
import os

# FORCE LOCALHOST
MONGO_URI = "mongodb://localhost:27017/smart_retail"
client = MongoClient(MONGO_URI)
db = client.get_database()

print("🔍 DIAGNOSTICS:")

# 1. Check User (The one likely logged in)
user = db.users.find_one({}) # Just get the first user to see
print(f"👤 User found: {user.get('email')} | ShopID: {user.get('shopId')}")

# 2. Check Product
product = db.products.find_one({"name": {"$regex": "laptop", "$options": "i"}})
if product:
    print(f"💻 Product: {product.get('name')} | ShopID: {product.get('shopId')}")
else:
    print("❌ No Laptop product found.")

# 3. Check Bills
bill_count = db.bills.count_documents({"shopId": "shop_12345"})
print(f"🧾 Bills for shop_12345: {bill_count}")

bill_null = db.bills.count_documents({"shopId": None})
print(f"🧾 Bills with NULL shopId: {bill_null}")

# 4. Check Bills for the specifics
if product:
    p_id = str(product.get("productId") or product.get("_id"))
    bills_for_prod = db.bills.count_documents({"items.productId": p_id})
    print(f"🧾 Total Bills for Laptop: {bills_for_prod}")
