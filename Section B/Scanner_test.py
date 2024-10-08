import Scanner


# Unit tests written for use with pytest, although they'll work without it too


# Checks that the QR code reader is working as expected
def test_read():
    testFileName = "discounts/test.png"
    data = Scanner.readQRCode(testFileName)

    assert data == "1134534535434000454165181320040302ADD670A0655ACE8436279213C3C9166E7DCB0D9481D21C7DC671FD2556E"


# Checks the scanner is correctly breaking down the content of secure QR codes
def test_decode():
    serialisedDiscount, keyID, sha1HMAC = Scanner.decodeSecureQR("1134534535434000454165181320040302ADD670A0655ACE8436279213C3C9166E7DCB0D9481D21C7DC671FD2556E")

    assert serialisedDiscount == "1134534535434000454165181320040302ADD670A0655"
    assert keyID == "ACE84362"
    assert sha1HMAC == "79213C3C9166E7DCB0D9481D21C7DC671FD2556E"

if __name__ == "__main__":
    test_read()
    test_decode()
