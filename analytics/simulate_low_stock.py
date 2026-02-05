from pymongo import MongoClient

# FORCE LOCALHOST
MONGO_URI = "mongodb://localhost:27017/smart_retail"
client = MongoClient(MONGO_URI)
db = client.get_database()

print("🔧 Simulating Low Stock for Demo...")

target_products = ["Banana", "Laptop", "Apple", "Headphones"]
count = 0

for name_part in target_products:
    res = db.products.update_one(
        {"name": {"$regex": name_part, "$options": "i"}},
        {"$set": {"quantity": 2}} # CRITICAL LOW STOCK
    )
    if res.modified_count:
        print(f"✅ Set '{name_part}' stock to 2.")
        count += 1
    else:
        print(f"⚠️ Could not find product matching '{name_part}'")

# Set one to HEALTHY for contrast
db.products.update_one(
    {"name": {"$regex": "Watch", "$options": "i"}},
    {"$set": {"quantity": 50}}
)
print("✅ Set 'Watch' stock to 50 (Healthy).")
