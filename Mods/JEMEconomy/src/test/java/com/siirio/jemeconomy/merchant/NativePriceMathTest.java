package com.siirio.jemeconomy.merchant;

/** Runs without Forge/JUnit: exercises the production methods used by NativeTradePricing. */
public final class NativePriceMathTest {
    private static int assertions;
    private static void equal(long expected, long actual) {
        assertions++;
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static void overflow(Runnable action) {
        assertions++;
        try { action.run(); throw new AssertionError("Expected checked arithmetic rejection"); }
        catch (ArithmeticException expected) { }
    }
    public static void main(String[] args) {
        equal(8, NativePriceMath.demandAdjusted(5, 2, 3));
        equal(5, NativePriceMath.demandAdjusted(100, 20, 1));
        equal(31, NativePriceMath.minimumPurchase(40, 20, 1));
        equal(501, NativePriceMath.minimumPurchase(1001, 1, 1));
        equal(640, NativePriceMath.minimumPurchase(1280, 4, 64));
        equal(31, NativePriceMath.afterDiscount(31, 0));
        equal(31, NativePriceMath.afterDiscount(31, -100));
        equal(31, NativePriceMath.afterDiscount(31, 1));
        equal(99, NativePriceMath.afterDiscount(31, 99));
        // Exhaustive low-unit spans include commodity prices, output batches and every representative cure ratio.
        for (int sell = 1; sell <= 100; sell++) for (int count = 1; count <= 64; count++) {
            long base = (sell * 2L + 1) * count;
            long floor = NativePriceMath.minimumPurchase(base, sell, count);
            for (int discount = 0; discount <= 100; discount += 5) {
                long charged = NativePriceMath.afterDiscount(floor, base * discount / 100);
                long payout = (sell * 150L / 100) * count;
                assertions++;
                if (charged <= payout || charged <= 0) throw new AssertionError("Profitable or nonpositive discounted buyback");
            }
        }
        overflow(() -> NativePriceMath.demandAdjusted(Long.MAX_VALUE, 1, 2));
        overflow(() -> NativePriceMath.demandAdjusted(10, 0, 1));
        overflow(() -> NativePriceMath.minimumPurchase(10, Long.MAX_VALUE, 1));
        overflow(() -> NativePriceMath.minimumPurchase(10, 1, 0));
        overflow(() -> NativePriceMath.afterDiscount(0, 10));
        equal(1, NativePriceMath.batchSale(1, 64, 100, 64));
        equal(2, NativePriceMath.batchSale(1, 65, 100, 64));
        equal(1, NativePriceMath.batchSale(1, 32, 150, 64));
        equal(2, NativePriceMath.batchSale(1, 64, 150, 64));
        equal(1, NativePriceMath.batchSale(1, 1, 100, Integer.MAX_VALUE));
        equal(2, NativePriceMath.batchSale(1, 128, 100, 64));
        equal(32, NativePriceMath.minimumPurchaseBudget(64, NativePriceMath.batchSale(1, 64, 150, 64)));
        equal(3, NativePriceMath.minimumPurchaseBudget(2, NativePriceMath.batchSale(1, 64, 150, 64)));
        // BigInteger is an independent exact-rational oracle for indivisible batch ceiling.
        for (int raw = 1; raw <= 8; raw++) for (int quantity = 1; quantity <= 64; quantity++)
            for (int units = 1; units <= 128; units++) for (int multiplier : new int[] {100, 125, 150}) {
                java.math.BigInteger numerator = java.math.BigInteger.valueOf(raw).multiply(java.math.BigInteger.valueOf(quantity))
                        .multiply(java.math.BigInteger.valueOf(multiplier));
                java.math.BigInteger denominator = java.math.BigInteger.valueOf(100L * units);
                long oracle = numerator.add(denominator).subtract(java.math.BigInteger.ONE).divide(denominator).longValueExact();
                equal(Math.max(1, oracle), NativePriceMath.batchSale(raw, quantity, multiplier, units));
            }
        equal(java.math.BigInteger.valueOf(Long.MAX_VALUE).add(java.math.BigInteger.valueOf(99)).divide(java.math.BigInteger.valueOf(100)).longValueExact(),
                NativePriceMath.batchSale(Long.MAX_VALUE, 1, 1, 1));
        overflow(() -> NativePriceMath.batchSale(Long.MAX_VALUE, 2, 100, 64));
        overflow(() -> NativePriceMath.batchSale(1, 0, 100, 64));
        overflow(() -> NativePriceMath.batchSale(1, 1, 0, 64));
        overflow(() -> NativePriceMath.batchSale(1, 1, 100, 0));
        overflow(() -> NativePriceMath.minimumPurchaseBudget(1, Long.MAX_VALUE));
        System.out.println("NativePriceMathTest: " + assertions + " assertions passed");
    }
}
