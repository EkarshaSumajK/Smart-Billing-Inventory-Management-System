from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/smart_retail")
client = MongoClient(MONGO_URI)
db = client.get_database()

# Find laptop
product = db.products.find_one({"name": {"$regex": "laptop", "$options": "i"}})

if product:
    print(f"Found product: {product['name']}")
    # Update image
    result = db.products.update_one(
        {"_id": product["_id"]},
        {"$set": {"imageUrl": "/gemini_banana.png"}}
    )
    print(f"Updated imageUrl to '/gemini_banana.png'. Modified count: {result.modified_count}")
else:
    print("No product found matching 'laptop'.")
