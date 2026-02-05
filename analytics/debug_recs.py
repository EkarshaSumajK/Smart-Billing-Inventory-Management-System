import requests

try:
    # 1. Trigger Training (just in case)
    print("Triggering training...")
    requests.post('http://localhost:5001/analytics/train-recommendations')
    
    # 2. Test Recommendation Endpoint
    print("Testing recommendations for P_BREAD...")
    payload = {
        "shopId": "691edbe52388451c9a800cc8",
        "cart": ["P_BREAD"]
    }
    resp = requests.post('http://localhost:5001/analytics/recommend', json=payload)
    data = resp.json()
    
    print("Response Status:", resp.status_code)
    print("Response Body:", data)
    
except Exception as e:
    print("Error:", e)
