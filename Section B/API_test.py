import requests
import API
from datetime import datetime, timedelta


# Unit tests written for use with pytest, although they'll work without it too


# Sanity check, just to demonstrate how interacting with a real world API might look like
def test_api_available():
    response = requests.get(f"http://localhost:{API.PORT}/get-details/5003125323653")
    content = response.json()
    assert response.status_code == 200
    assert "application/json" in response.headers.get('Content-Type','')
    assert response.json().get("long-name") == "Supermarket Own Brand Tomato Soup"


# Tests if the discount database is working as intended
def test_nonce():
    tomorrow = datetime.now() + timedelta(days=1)
    discountData = {"nonce": "0123456789ABCDEF", "expires": int(tomorrow.timestamp()), "key-id": "ABCDEF12"}

    check1Response = requests.get(f"http://localhost:{API.PORT}/verify-nonce/{discountData['nonce']}")
    assert check1Response.status_code != 200

    putResponse = requests.put(f"http://localhost:{API.PORT}/submit-nonce",data=discountData)
    assert putResponse.status_code == 200

    check1Response = requests.get(f"http://localhost:{API.PORT}/verify-nonce/{discountData['nonce']}")
    assert check1Response.status_code == 200

    deleteResponse = requests.delete(f"http://localhost:{API.PORT}/delete-nonce/{discountData['nonce']}")
    assert deleteResponse.status_code == 200

    check2Response = requests.get(f"http://localhost:{API.PORT}/verify-nonce/{discountData['nonce']}")
    assert check2Response.status_code != 200


if __name__ == "__main__":
    test_api_available()
    test_nonce()