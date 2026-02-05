import requests
import json

# Use the shopId we fixed earlier
SHOP_ID = "shop_12345"
PRODUCT_ID = "p12345678" # Laptop ID (mock) - wait, I need a real one.
# Actually, I'll search for one first.

try:
    from pymongo import MongoClient
    client = MongoClient("mongodb://localhost:27017/smart_retail")
    db = client.get_database()
    prod = db.products.find_one()
    if prod:
        PRODUCT_ID = str(prod.get("productId") or prod.get("_id"))
        SHOP_ID = prod.get("shopId")
        print(f"found product: {prod.get('name')}, shop: {SHOP_ID}, id: {PRODUCT_ID}")
except:
    pass

url = f"http://localhost:5001/analytics/inventory-intelligence?shopId={SHOP_ID}&productId={PRODUCT_ID}&currentStock=5"
print(f"Hitting: {url}")

try:
    resp = requests.get(url)
    print(f"Status: {resp.status_code}")
    print(json.dumps(resp.json(), indent=2))
except Exception as e:
    print(f"Error: {e}")
