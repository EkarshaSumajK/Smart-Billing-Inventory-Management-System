import os
from pymongo import MongoClient
from dotenv import load_dotenv

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")
DB_NAME = os.getenv("DB_NAME")

print(f"🔌 Connecting to {DB_NAME}...")
client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
db = client[DB_NAME]

def clear_cache():
    try:
        # Confirm connection
        client.admin.command('ping')
        
        # Delete all documents in price_predictions
        result = db.price_predictions.delete_many({})
        print(f"✅ CLEANUP SUCCESS: Deleted {result.deleted_count} cached predictions.")
        
    except Exception as e:
        print(f"❌ CLEANUP FAILED: {e}")

if __name__ == "__main__":
    clear_cache()
