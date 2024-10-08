import requests
import Discounter
import cv2
import os
import Scanner


# Global Variables
API_PORT = 5000


# Generates a key using the operating system's CSPRNG, a cross-platform implementation.
def keyGenerator():
    key = os.urandom(32)
    keyID = os.urandom(4)
    with open(os.path.join("keys", f"{keyID.hex().upper()}.key"), "wb") as keyFile:
        keyFile.write(keyID)
        keyFile.write(key)
    print(f"Created new key file: {keyID.hex().upper()}.key")
    return keyID.hex().upper()


# Just a helper function that will display the QR code once it's generated
def showImage(imagePath: str):
    image = cv2.imread(imagePath)
    cv2.imshow("Secure QR Code: Press any key to close", image)
    cv2.waitKey(0)
    cv2.destroyAllWindows()


# A complete reference demonstration on how to produce a secure discount QR code.
def main():
    # Generate a new shared secret key for the HMAC-SHA1
    keyID = keyGenerator()
    keyPath = os.path.join("keys", f"{keyID}.key")

    # Generate a secure discount code
    newNonce = Discounter.main(keyPath, quantityNeeded=1)[0]
    imagePath = os.path.join("discounts", f"{newNonce}.png")

    # Show the secure QR code
    print("Showing image in new window")
    showImage(imagePath)

    # Decode the secure QR code as the barcode scanner would
    # and check if the discount is still valid (unused & in-date)
    validDiscount = Scanner.lookupQRCode(imagePath)
    print("Valid Discount?:", validDiscount)

    # Pretend customer bought item, and the discount has been consumed
    input("Press enter to use the discount")
    requests.delete(f"http://localhost:{API_PORT}/delete-nonce/{newNonce}")

    # Check the discount has been consumed and no longer works
    validDiscount = Scanner.lookupQRCode(imagePath)
    print("Valid Discount?:", validDiscount)


if __name__ == '__main__':
    main()