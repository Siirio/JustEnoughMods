package com.siirio.jemeconomy.client;

/** Standalone Java 17 contract test; no Minecraft initialization required. */
public final class CoinAmountsContractTest {
    public static void main(String[] args) {
        long[] values = {0, 1, 8, 9, 10, 20, 99, 100, 300, 999, 1000, 1500, 9999, 10000, 12345, Long.MAX_VALUE};
        for (long value : values) {
            long restored = 0, previous = Long.MAX_VALUE;
            var parts = CoinAmounts.parts(value);
            for (var part : parts) {
                if (part.count() < 0 || part.denomination().value() >= previous) throw new AssertionError("Invalid ordering/count: " + value);
                previous = part.denomination().value();
                restored = Math.addExact(restored, Math.multiplyExact(part.count(), part.denomination().value()));
            }
            if (restored != value) throw new AssertionError("Lost remainder: " + value);
        }
        if (CoinAmounts.parts(1500).size() != 2 || CoinAmounts.parts(20).get(0).count() != 2) throw new AssertionError("Wrong canonical denominations");
        try { CoinAmounts.parts(-1); throw new AssertionError("Negative value accepted"); } catch (IllegalArgumentException expected) { }
        System.out.println("Coin denomination contracts passed (including Long.MAX_VALUE)");
    }
}
