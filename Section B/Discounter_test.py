from Discounter import Discount
from datetime import datetime, timedelta


# Unit tests written for use with pytest, although they'll work without it too


# Check if the serialisation works as expected
def test_serialisation():
    itemBarcode = "500145326431"
    newPrice = 150
    validUntil = datetime(2022, 5, 3) + timedelta(days = 1, hours = 6)
    nonce = "e7deebb58b2d0534"
    discount = Discount(itemBarcode, newPrice, int(validUntil.timestamp()), nonce)
    serialised = discount.serialise()

    assert serialised == "125001453264310001501651640400e7deebb58b2d0534"


# Check if the deserialisation works as expected
def test_deserialisation():
    serialised = "1134534535434000454165181320040302ADD670A0655"
    decodedDiscount = Discount.deserialise(serialised)

    assert decodedDiscount.itemIdentifier == "34534535434"
    assert decodedDiscount.newPrice == 454
    assert decodedDiscount.validUntil == 1651813200
    assert decodedDiscount.nonce == "40302ADD670A0655"


if __name__ == "__main__":
    test_serialisation()
    test_deserialisation()