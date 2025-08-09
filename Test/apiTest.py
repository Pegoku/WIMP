import os
from dotenv import load_dotenv
import requests

API_URL = "https://api.ship24.com/public/v1/"

load_dotenv()

tracking_number = "077010000029154662"

API_KEY = os.getenv("API_KEY")

couriers = [(1,2,3,1,2,3),(),()]

headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {API_KEY}"
}

def get_couriers():
    url = f"{API_URL}couriers"
    response = requests.get(url, headers=headers)
    return response.json()

def create_tracker(tracking_number, courier_code=None, post_code=None):
    url = f"{API_URL}trackers"
    payload = {
        "trackingNumber": tracking_number,
        "courierCode": courier_code,
        "destinationPostCode": post_code
        
    }
    response = requests.post(url, json=payload, headers=headers)
    return response.json()

def get_tracker_by_trackingNumber(tracking_number):
    url = f"{API_URL}trackers/search/{tracking_number}/results"
    response = requests.get(url, headers=headers)
    return response.json()

def main():
    print(f"API_KEY = {API_KEY}")
    couriers = get_couriers()
    # print(f"Available couriers: {couriers}")
    # tracking_number = "077010000029798418"
    print(f"Tracking number: {create_tracker("2025300788730135820601", "seur-es")}")
    print(f"Tracking status: {get_tracker_by_trackingNumber("2025300788730135820601")}")

if __name__ == "__main__":
    main()