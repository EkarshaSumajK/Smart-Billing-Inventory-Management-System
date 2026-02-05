import requests
from pymongo import MongoClient
import os
from dotenv import load_dotenv
import pprint

load_dotenv()
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/smart_retail")
client = MongoClient(MONGO_URI)
db = client.get_database()

# 1. Find the Laptop Product
product = db.products.find_one({"name": {"$regex": "laptop", "$options": "i"}})
if not product:
    print("❌ Could not find 'Laptop' product!")
    exit()

p_id = str(product.get("productId") or product.get("_id"))
shop_id = product.get("shopId")

print(f"Checking Analytics for: {product['name']}")
print(f"Product ID: {p_id}")
print(f"Shop ID: {shop_id}")

# 2. Check Bills Count
bill_count = db.bills.count_documents({"items.productId": p_id})
print(f"Total Bills for this product: {bill_count}")

# 3. Hit the Endpoint
url = f"http://localhost:5001/analytics/inventory-intelligence?shopId={shop_id}&productId={p_id}&currentStock=5"
print(f"Hitting: {url}")

try:
    res = requests.get(url)
    print(f"Status Code: {res.status_code}")
    print("Response:")
    pprint.pprint(res.json())
except Exception as e:
    print(f"❌ Request failed: {e}")
