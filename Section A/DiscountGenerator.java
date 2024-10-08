package com.example.panacea;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that encapsulates all the reverse-engineered discounting algorithms
 *
 * @version 1.0
 *
 */
public class DiscountGenerator {

    private static Map<String, String> lidlDiscounts = new HashMap<>();

    static {
        // TODO: Find sample of 5p discount
        lidlDiscounts.put("20p", "0035255");
        lidlDiscounts.put("70p", "0041461");
        lidlDiscounts.put("90p", "0035378");
        lidlDiscounts.put("£1.50", "0035248");
        lidlDiscounts.put("30% Off", null);
    }

    static List<String> getAllLidlDiscounts() {
        return new ArrayList<>(lidlDiscounts.keySet());
    }

    /**
     * Calculates the check digit using alternating weightings and a modulo 10 division.
     *
     * @param barcode The data to calculate a check digit for. If it already has a check
     *                digit at the end, it will be treated as normal data.
     * @param evenWeighting The multiplier used for digits in even indices, including 0.
     * @param oddWeighting The multiplier used for digits in odd positions.
     * @return A modulo 10 remainder of the weighted checksum.
     */
    private static int calculateCheckDigit(String barcode, int evenWeighting, int oddWeighting) {
        int checkSum = 0;
        for (int i = 0; i < barcode.length(); i++) {
            int currentDigit = barcode.charAt(i) - '0';
            if (i % 2 == 0)
                checkSum += currentDigit * evenWeighting;
            else
                checkSum += currentDigit * oddWeighting;
        }
        return checkSum % 10;
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Sainsbury's stores
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     */
    static String sainsburysDiscount(String productBarcode, int newPrice) {
        StringBuilder stringBuilder = new StringBuilder("91");
        String zeroPaddedProduct = String.format("%013d", new BigInteger(productBarcode));
        stringBuilder.append(zeroPaddedProduct);
        String zeroPaddedPrice = String.format("%06d", newPrice);
        stringBuilder.append(zeroPaddedPrice);
        int checkDigit = calculateCheckDigit(stringBuilder.toString(), 7, 9);
        stringBuilder.append(checkDigit);
        return stringBuilder.toString();
    }


    /**
     * Implementation of the reverse-engineered discounting algorithm for Tesco stores.
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @param discountReasonCode A digit encoding why the discount exists, any digit 0-9 produces
     *                           a valid discount code that is accepted by the POS
     * @param discountIteration Used to show if the product had already been discounted
     *                          before the current discount, typical values are 0, 1, 2 or 3
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #tescoDiscount(String, int, int) Same method, but defaults to discount iteration 3
     */
    static String tescoDiscount(String productBarcode, int newPrice, int discountReasonCode, int discountIteration) {
        StringBuilder stringBuilder = new StringBuilder("971");
        String zeroPaddedProduct = String.format("%013d", new BigInteger(productBarcode));
        stringBuilder.append(zeroPaddedProduct);
        stringBuilder.append(discountReasonCode);
        String zeroPaddedPrice = String.format("%05d", newPrice);
        stringBuilder.append(zeroPaddedPrice);
        stringBuilder.append(discountIteration);
        int checkDigit = calculateCheckDigit(stringBuilder.toString(), 7, 9);
        stringBuilder.append(checkDigit);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Tesco stores.
     * This method will set the discount iteration to 3 everytime
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @param mysteryDigit Purpose of the digit is unknown, but any digit 0-9 produces
     *                     a valid discount code that is accepted by the POS
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #tescoDiscount(String, int, int, int) Same method, except allows changing the discount iteration
     */
    static String tescoDiscount(String productBarcode, int newPrice, int mysteryDigit) {
        return tescoDiscount(productBarcode, newPrice, mysteryDigit,  3);
    }

    /**
     * These discounts for Lidl are not dynamically calculated based on the original item.
     * Waste Not reductions can only take certain values, namely 20p, 70p, 90p, and £1.50.
     * All reductions to the same price will have the same barcode, as data of the original
     * item doesn't get encoded in the discount.
     *
     * @param discountName A set discount price, chosen from the in-app list
     * @return Contents to be encoded in an EAN-8 barcode for a new discount
     */
    static String lidlWasteNotDiscount(String discountName) {
        return lidlDiscounts.get(discountName);
    }

    /**

     *
     * Implementation of the reverse-engineered discounting algorithm for Lidl's 30% off discounts.
     * Aside from the fixed discounts provided by Waste Not reductions, this is the only alternative.
     * These discounts will always register on the POS as 30% cheaper than the base price.
     *
     * @param productBarcode A set discount price, chosen from the in-app list
     * @return Contents to be encoded in a Code-128 barcode for a 30% discount
     */
    static String lidl30PercentDiscount(String productBarcode) {
        StringBuilder stringBuilder = new StringBuilder("55");
        String zeroPaddedProduct = String.format("%013d", new BigInteger(productBarcode));
        stringBuilder.append(zeroPaddedProduct);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Waitrose stores.
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     */
    static String waitroseDiscount(String productBarcode, int newPrice) {
        StringBuilder stringBuilder = new StringBuilder("10");
        String zeroPaddedProduct = String.format("%013d", new BigInteger(productBarcode));
        stringBuilder.append(zeroPaddedProduct);
        String zeroPaddedPrice = String.format("%05d", newPrice);
        stringBuilder.append(zeroPaddedPrice);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Cooperative stores.
     * This discount algorithm doesn't include date information, which is how some Cooperatives
     * implement their discounts
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #cooperativeDiscountLong(String, int) Implementation that also encodes date information
     * @see #cooperativeDiscountLong(String, int, String) Also encodes date information
     */
    static String cooperativeDiscountShort(String productBarcode, int newPrice) {
        String zeroPaddedProduct = String.format("%013d", new BigInteger(productBarcode));
        StringBuilder stringBuilder = new StringBuilder(zeroPaddedProduct);
        stringBuilder.append("01");
        String zeroPaddedPrice = String.format("%04d", newPrice);
        stringBuilder.append(zeroPaddedPrice);
        int checkDigit = calculateCheckDigit(stringBuilder.toString(), 7, 9);
        stringBuilder.append(checkDigit);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Cooperative stores.
     * This discount algorithm does include date information, which is how some Cooperatives
     * implement their discounts
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @param mysteryDigit Purpose of the digit is unknown, but either digit 0 or 1 produces
     *                     a valid discount code that is accepted by the POS
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #cooperativeDiscountLong(String, int)  Same method, except defaults to mysteryDigit value of 0
     */
    static String cooperativeDiscountLong(String productBarcode, int newPrice, String mysteryDigit) {
        String zeroPaddedProduct = String.format("%013d", new BigInteger(productBarcode));
        StringBuilder stringBuilder = new StringBuilder(zeroPaddedProduct);
        stringBuilder.append(mysteryDigit);
        stringBuilder.append("01");
        String zeroPaddedPrice = String.format("%04d", newPrice);
        stringBuilder.append(zeroPaddedPrice);
        LocalDate fromDate = LocalDate.of(2000,01,01);
        LocalDate toDate = LocalDate.now();
        long daysSinceY2K = ChronoUnit.DAYS.between(fromDate, toDate);
        stringBuilder.append(daysSinceY2K);
        int checkDigit = calculateCheckDigit(stringBuilder.toString(), 9, 7);
        stringBuilder.append(checkDigit);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Cooperative stores.
     * This discount algorithm does include date information, which is how some Cooperatives
     * implement their discounts
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #cooperativeDiscountLong(String, int, String)  Same method, except allows customisation of mysteryDigit
     */
    static String cooperativeDiscountLong(String productBarcode, int newPrice) {
        return cooperativeDiscountLong(productBarcode, newPrice, "0");
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Marks and Spencer stores.
     *
     * @param productBarcode Unaltered EAN-8 barcode as it appears on the item's original packaging.
     *                       Doesn't support the discounting of EAN-13 barcodes.
     * @param oldPrice The regular MSRP price that the item would sell for had it not been discounted.
     *                 If the item is being discounted multiple times, this is NOT the price
     *                 that was on the previous discount, this is the original price.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @param discountIteration Used to show if the product had already been discounted
     *                          before the current discount, typical values are 1, 2, or 3.
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #marksAndSpencerDiscount(String, int, int) Calls this function, but with default discountIteration 1
     */
    static String marksAndSpencerDiscount(String productBarcode, int oldPrice, int newPrice, String discountIteration) {
        StringBuilder stringBuilder = new StringBuilder("82");
        stringBuilder.append(discountIteration);
        String zeroPaddedProduct = String.format("%08d", new BigInteger(productBarcode));
        stringBuilder.append(zeroPaddedProduct);
        String zeroPaddedOldPrice = String.format("%05d", oldPrice);
        stringBuilder.append(zeroPaddedOldPrice);
        String zeroPaddedNewPrice = String.format("%05d", newPrice);
        stringBuilder.append(zeroPaddedNewPrice);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Marks and Spencer stores.
     * This method will set a default value for the discount iteration of 1
     *
     * @param productBarcode Unaltered EAN-8 barcode as it appears on the item's original packaging.
     *                       Doesn't support the discounting of EAN-13 barcodes.
     * @param oldPrice The regular MSRP price that the item would sell for had it not been discounted.
     *                 If the item is being discounted multiple times, this is NOT the price
     *                 that was on the previous discount, this is the original price.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #marksAndSpencerDiscount(String, int, int) Allows for customisation of the discount iteration
     */
    static String marksAndSpencerDiscount(String productBarcode, int oldPrice, int newPrice) {
        return marksAndSpencerDiscount(productBarcode, oldPrice, newPrice, "1");
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Iceland stores.
     * This method has NOT BEEN TESTED in the real world, so be warned.
     *
     * @param productBarcode Unaltered EAN-8 barcode as it appears on the item's original packaging.
     *                       Doesn't support the discounting of EAN-13 barcodes.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @return Contents to be encoded in a EAN-13 barcode for a new discount
     */
    static String icelandDiscount(String productBarcode, int newPrice) {
        String zeroPaddedProduct = String.format("%08d", new BigInteger(productBarcode));
        StringBuilder stringBuilder = new StringBuilder(zeroPaddedProduct);
        String zeroPaddedPrice = String.format("%04d", newPrice);
        stringBuilder.append(zeroPaddedPrice);
        int checkDigit = calculateCheckDigit(stringBuilder.toString(), 9, 7);
        stringBuilder.append(checkDigit);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Morrisons stores.
     * This method has NOT BEEN TESTED in the real world, so be warned.
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @param mysteryDigit Purpose and effect of the mystery digit is unknown, may take any value 0-9
     * @param discountIteration Used to show if the product had already been discounted
     *                          before the current discount, typical values are 1, 2, or 3.
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #morrisonsDiscount(String, int, int)  Same method, with default value 1 for discountIteration
     */
    static String morrisonsDiscount(String productBarcode, int newPrice, int mysteryDigit, int discountIteration) {
        StringBuilder stringBuilder = new StringBuilder("92");
        String zeroPaddedProduct = String.format("%013d", new BigInteger(productBarcode));
        stringBuilder.append(zeroPaddedProduct);
        String zeroPaddedPrice = String.format("%05d", newPrice);
        stringBuilder.append(zeroPaddedPrice);
        stringBuilder.append(mysteryDigit);
        stringBuilder.append("000");
        stringBuilder.append(discountIteration);
        int checkDigit = calculateCheckDigit(stringBuilder.toString(), 9, 7);
        stringBuilder.append(checkDigit);
        return stringBuilder.toString();
    }

    /**
     * Implementation of the reverse-engineered discounting algorithm for Morrisons stores.
     * This method has NOT BEEN TESTED in the real world, so be warned.
     *
     * @param productBarcode Unaltered EAN-13 or EAN-8 barcode as it
     *                       appears on the item's original packaging.
     * @param newPrice The desired price that will appear on the
     *                 POS when the new discount is scanned, in pence.
     * @param mysteryDigit Purpose and effect of the mystery digit is unknown, may take any value 0-9
     * @return Contents to be encoded in a Code-128 barcode for a new discount
     *
     * @see #morrisonsDiscount(String, int, int)  Same method, but allows customisation of the discountIteration
     */
    static String morrisonsDiscount(String productBarcode, int newPrice, int mysteryDigit) {
        return tescoDiscount(productBarcode, newPrice, mysteryDigit,  1);
    }
}
