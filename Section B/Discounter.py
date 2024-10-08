import hmac
from datetime import datetime, timedelta
import os
from sys import argv
from io import StringIO
import qrcode
import requests

# Global variables
API_PORT = 5000
KEY: str
KEY_ID: str


class Discount:
    itemIdentifier: str  # Preferably numeric, must be <100 characters
    newPrice: int  # In pence, 6 digits should allow £9999.99
    validUntil: int  # POSIX timestamp in output, 10 digits long
    nonce: str  # 64 bits from a CSPRNG, as 16 character hex string

    def __init__(self, itemIdentifier: str, newPrice: int, validUntil: int, nonce: str = None):
        self.itemIdentifier = itemIdentifier
        self.identifierLength = len(itemIdentifier)
        self.newPrice = newPrice
        self.validUntil = validUntil

        if not nonce:
            self.nonce = os.urandom(8).hex().upper()
        else:
            self.nonce = nonce

    #  Serialise a discount into an alphanumeric string, uppercase letters and numbers only
    #  Structure:  ll bbbbbbbb... pppppp dddddddddd nnnnnnnnnnnnnnnn
    #  Content:  barcodeLength + barcode + newPrice + expiryDate + nonce
    #  Length:     2 + barcodeLength + 4 + 6 + 10 + 16
    def serialise(self):
        content = str(len(self.itemIdentifier)).zfill(2)
        content += self.itemIdentifier
        content += str(self.newPrice).zfill(6)
        content += str(self.validUntil)
        content += self.nonce
        return content

    # Deserialise back into a discount
    @staticmethod
    def deserialise(serialisedDiscount: str):
        stream = StringIO(serialisedDiscount)
        identifierLength = int(stream.read(2))

        if len(serialisedDiscount) != 34 + identifierLength:
            return None

        productIdentifier = stream.read(identifierLength)
        newPrice = int(stream.read(6))
        validUntil = int(stream.read(10))
        nonce = stream.read(16).upper()

        decodedDiscount = Discount(productIdentifier, newPrice, validUntil, nonce)
        return decodedDiscount


def generateHMAC(message: str, key: str, hashAlgorithm: str = "sha1"):
    sha1HMAC = hmac.new(key=key.encode(), msg=message.encode(), digestmod=hashAlgorithm)
    return sha1HMAC.digest()


# Open a keyfile, should be a 4 byte key ID followed by a 32 byte key
def loadKey(keyFilePath: str):
    with open(keyFilePath, "rb") as keyFile:
        keyFileContents = keyFile.read()

    if len(keyFileContents) != 36:
        print("Invalid keyfile")

    global KEY_ID, KEY
    KEY_ID = keyFileContents[0:4].hex().upper()
    KEY = keyFileContents[4::]



def generateDiscount(itemBarcode: str, newPrice: int, expiryDate: int):
    # Encapsulate the details of the barcode, and produce a unique nonce
    discount = Discount(itemBarcode, newPrice, expiryDate)

    # Register the nonce
    response = requests.put(f"http://localhost:{API_PORT}/submit-nonce",
                            data={"nonce": discount.nonce, "expires": expiryDate, "key-id": KEY_ID})
    if response.status_code != 200:
        raise Exception("Failed to register nonce, invalid key?")

    # Serialise and append the HMAC
    serialisedDiscount = discount.serialise()
    sha1HMAC = hmac.new(key=KEY, msg=serialisedDiscount.encode(), digestmod="sha1")
    qrCodeContent = serialisedDiscount + KEY_ID + sha1HMAC.hexdigest()

    # Produce a QR code with the contents
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_Q)
    qr.add_data(qrCodeContent.upper())
    qr.make()
    image = qr.make_image()
    image.save(os.path.join("discounts", f"{discount.nonce}.png"))
    print(f"Created QR Code: {discount.nonce}.png")
    return discount


# Collect user input to build some discounts, and return a list of the nonces registered
def main(keyFilePath: str, quantityNeeded: int = None):
    loadKey(keyFilePath)
    itemBarcode = input("Enter item barcode: ")
    newPrice = int(input(f"Enter new price (in pence): "))
    expiryDay = int(input("Expiry day for discount (as an integer): "))
    expiryMonth = int(input("Expiry month for discount (as an integer): "))
    expiryYear = int(input("Expiry year for discount (as an integer): "))

    # Expires at 6am the day after
    expiryDate = datetime(expiryYear, expiryMonth, expiryDay) + timedelta(days=1, hours=6)
    expiryTimestamp = int(expiryDate.timestamp())

    # Often times, you want to discount the entire batch
    if not quantityNeeded:
        quantityNeeded = int(input("Quantity needed: "))

    noncesProduced = []
    for i in range(quantityNeeded):
        discount = generateDiscount(itemBarcode, newPrice, expiryTimestamp)
        noncesProduced.append(discount.nonce)

    return noncesProduced


if __name__ == "__main__":
    if len(argv) != 2:
        print("Program Usage: python Discounter.py key_file.key")
    else:
        main(argv[1])
