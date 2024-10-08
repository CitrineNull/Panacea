# Name Your Price - Securing Exploitable Inventory Systems

All major British supermarket chains have a way of applying discounts to their goods,
often used for perishables that are about to expire, damaged goods or items designated for clearance.
The act of discounting is often done by generating a barcode that encodes details about the original item and its new discount price,
which is then processed by the Point-Of-Sales system during the customer’s checkout.
Although most supermarkets use EAN-13 and EAN-8 barcodes for most their products,
they all have their own schema’s for how discounts are produced and applied.

Stores featuring self-checkout allow the customer to select the barcode scanned, which introduces
vulnerability as an attacker can use a barcode of their choice to manipulate the POS system, and control the
pricing for the items they “buy”.

This is a hacking project I worked on as part of my University dissertation.
I outlined the weaknesses of existing POS systems by demonstrating exploitation of this vulnerability across most major UK supermarkets.
I also developed a program that is extensible to any given store with a similar discounting model,
as well as a versatile solution that is applicable to stores under their existing constraints to resolve this vulnerability.

**This project exists for educational purposes only, and I strongly discourage real world exploitation of this vulnerability**

# Technical Details

I discuss the full technical details of this vulnerability in the [Report](Report.pdf).

The project is divided into two sections.
Section A shows the vulnerability and how it would be exploited.
Section B shows how a more secure system can be implemented to fix the issues that exist in the current.

## Section A - Showcasing the Exploit

I created an Android application named Panacea to demonstrate how easily custom barcodes can be generated,
but for obvious ethical reasons I won't be putting it online.

The problem is pretty blatant when you take a close look at a typical discount barcode.
For example, here is a discount barcode from Sainsbury's:

![img.png](Section%20A/discount.png)

The barcode is split up into 4 sections:

1. A GS1 Application Identifier (this is always `91` for Sainsbury's)
2. The original barcode of the item before it was discounted
   - There are 13 characters since they are usually EAN-13 barcodes
   - It is simply zero-padded on the left if it's a shorter EAN-8 barcode
3. The new price of the item, in pence (up to £9999.99 maximum)
4. A standard barcode check-digit

There's no client-side or server side checks to validate the authenticity of these barcodes,
nor is there much security through obscurity (not that it would be a valid strategy either).
Some stores will have extra data, encoding things such as when it was created, the original price, why it was discounted, etc.,
but for the most part they all have the same issue in that they're trivial to produce by anybody.

I just went off the Top 10 supermarkets in the UK according to [statista](https://www.statista.com/statistics/280208/grocery-market-share-in-the-united-kingdom-uk/),
and of those I created working proof of concepts for:

- Tesco
- Sainsbury's
- Morrisons
- Co-op
- Waitrose
- Iceland
- Marks and Spencer
- Lidl

The [Report](./Report.pdf) contains a nice visual and mathematical explanation for the algorithms that were reverse engineered,
as well as screenshots of the application on page 15.
The algorithms themselves can be found in [DiscountGenerator.java](Section%20A/DiscountGenerator.java),
which contains a snippet of the code used in the application to generate the barcodes.

## Section B - A Better Alternative

Again, the [Report](./Report.pdf) contains a detailed explanation of the solution, but TL-DR:

There are two main issues in the existing system:

1. **Authenticity** - Attackers should not be able to generate their own barcodes and have them accepted by the POS.
2. **Replay Attacks** - Attackers should not be able to use an old barcode to buy a new item at a previous price.

The solution uses QR codes instead of barcodes, and solves the issues like so:

1. **Authenticity** - The QR codes are appended with a HMAC, using a symmetric key can be kept on a TPM or easily be rotated.
2. **Replay Attacks** - Each QR code contains a nonce, where the API can invalidate them once they've been consumed.

I chose a HMAC instead of a full digital signature with an asymmetric key since the codes can be kept much smaller,
but the *QR Code* aspect of this solution can easily be substituted with a PDF417 code or RFID tag to fit the business' contraints.

The QR codes are quite small, even with plenty of error correcting.
This example QR code contains the item data, price, a nonce, HMAC, and 25% error correction:

![test.png](Section%20B/discounts/test.png)

### Demo

The demo showcases how a single use authenticated QR code can be generated and consumed.
To run the Python demo, first install the dependencies (I used Python 3.7, but it *should* work with newer):

```shell
pip install -r requirements.txt
```

Then launch the API on a port of your choice, (`5000` is the API port the demo expects):

```
python3 API.py 5000
```

Then you can run the demo:

```shell
python3 Demo.py
```

The demo does the following:

1. A key for the HMAC are generated and placed where both the scanner and discounter can access them.
2. The discounter is called to produce 1 new discount, taking input from the user
3. It shows the QR code that was generated by opening a window
4. The scanner confirms the discount is valid by consulting the API
5. The demo sends a delete request to destroy the discount
6. The scanner checks to see if the discount is still valid