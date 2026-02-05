from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()
client = MongoClient(os.getenv("MONGO_URI"))
db = client.get_database()

shop_id = "691edbe52388451c9a800cc8"
product_id = "696f8ab823a6d1e22f7a9103"

print(f"Checking for Shop: {shop_id}, Product: {product_id}")

count = db.bills.count_documents({
    "shopId": shop_id,
    "items.productId": product_id
})

print(f"Bills found: {count}")

print("Sample bill:")
print(db.bills.find_one({"shopId": shop_id, "items.productId": product_id}))
