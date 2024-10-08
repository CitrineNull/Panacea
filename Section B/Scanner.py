from sys import argv
import hmac
from Discounter import Discount
import requests
import os
import cv2


# Global Variable
API_PORT = 5000


# Takes the path for an image with a QR code in it, and returns the data encoded in the QR code
# Uses OpenCV's reader, which is very robust
def readQRCode(imagePath: str):
    img = cv2.imread(imagePath)
    detector = cv2.QRCodeDetector()
    data, bbox, straight_qrcode = detector.detectAndDecode(img)
    if bbox is not None:
        return data
    else:
        raise Exception("Can't read QR code")


# Takes the contents of a secure QR code, and breaks it down into the discount, HMAC and key ID
def decodeSecureQR(rawQRCodeContent: str):
    if len(rawQRCodeContent) != 82 + int(rawQRCodeContent[0:2]):
        raise Exception("Invalid Secure QR Code")

    sha1HMAC = rawQRCodeContent[-40::]
    keyID = rawQRCodeContent[-48:-40]
    serialisedDiscount = rawQRCodeContent[:-48]
    return serialisedDiscount, keyID, sha1HMAC


# Load a key given the key ID
def getKey(keyID: str):
    with open(os.path.join("keys", f"{keyID}.key"), "rb") as keyFile:
        keyFile.seek(4)
        key = keyFile.read(32)
    return key


# Takes the path to a QR code, asserts authenticity, and checks if the discount hasn't been used before.
def lookupQRCode(qrImageName: str):
    data = readQRCode(qrImageName)
    serialisedDiscount, keyID, decodedHMAC = decodeSecureQR(data)
    discount = Discount.deserialise(serialisedDiscount)

    result = requests.get(f"http://localhost:{API_PORT}/verify-nonce/{discount.nonce}")
    if result.status_code != 200:
        return False

    key = getKey(keyID)
    sha1HMAC = hmac.new(key=key, msg=discount.serialise().encode(), digestmod="sha1")
    if hmac.compare_digest(decodedHMAC, sha1HMAC.hexdigest().upper()):
        return True
    else:
        return False


def main():
    return lookupQRCode(argv[1])


if __name__ == "__main__":
    if len(argv) != 2:
        print("Program Usage: python Scanner.py qrcode.png")
    else:
        main()
