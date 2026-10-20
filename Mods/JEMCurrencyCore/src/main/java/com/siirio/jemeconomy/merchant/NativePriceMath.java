package com.siirio.jemeconomy.merchant;

/** Checked integer arithmetic shared by real native quotes and native quote and authoring arithmetic. */
public final class NativePriceMath {
    private NativePriceMath() { }
    public static long demandAdjusted(long base, int original, int current) {
        if (base <= 0 || original <= 0 || current <= 0) throw new ArithmeticException("Nonpositive native price");
        return Math.floorDiv(Math.addExact(Math.multiplyExact(base, current), original - 1L), original);
    }
    public static long minimumPurchase(long base, long sellUnit, int count) {
        if (base <= 0 || sellUnit <= 0 || count <= 0) throw new ArithmeticException("Nonpositive native merchandise");
        long maximumBuyback = Math.addExact(Math.multiplyExact(Math.floorDiv(Math.multiplyExact(sellUnit, 150L), 100L), count), 1L);
        return Math.max(maximumBuyback, Math.floorDiv(Math.addExact(base, 1L), 2L));
    }
    /** Ceiling is applied once to the whole offer, preserving indivisible native stack quantities. */
    public static long batchSale(long rawSell, int quantity, int multiplier, int sellUnits) {
        if (rawSell <= 0 || quantity <= 0 || multiplier <= 0 || sellUnits <= 0)
            throw new ArithmeticException("Nonpositive native sale terms");
        long numerator = Math.multiplyExact(Math.multiplyExact(rawSell, quantity), multiplier);
        long denominator = Math.multiplyExact(100L, sellUnits);
        long quotient = numerator / denominator;
        // No numerator + denominator - 1 intermediate overflow for otherwise valid large quotes.
        return Math.max(1L, Math.addExact(quotient, numerator % denominator == 0 ? 0L : 1L));
    }
    public static long minimumPurchaseBudget(long base, long maximumBuyback) {
        if (base <= 0 || maximumBuyback < 0) throw new ArithmeticException("Nonpositive native budget");
        return Math.max(Math.addExact(maximumBuyback, 1L), Math.floorDiv(Math.addExact(base, 1L), 2L));
    }
    public static long afterDiscount(long floor, long discounted) {
        if (floor <= 0) throw new ArithmeticException("Nonpositive native price floor");
        return Math.max(floor, discounted);
    }
}
