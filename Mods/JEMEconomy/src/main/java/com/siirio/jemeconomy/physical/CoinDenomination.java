package com.siirio.jemeconomy.physical;

import java.util.Arrays;
import java.util.Optional;

public enum CoinDenomination {
    COPPER("copper_coin", 1L),
    SILVER("silver_coin", 10L),
    GOLD("gold_coin", 100L),
    AETHER("aether_coin", 1_000L),
    ARCANE("arcane_coin", 10_000L);

    private final String id;
    private final long value;

    CoinDenomination(String id, long value) {
        this.id = id;
        this.value = value;
    }

    public String id() {
        return id;
    }

    public long value() {
        return value;
    }

    /** Whole backed coins consumed and issued by a physical denomination change. */
    public Conversion conversionTo(CoinDenomination target, int requested) {
        if (target == null || target == this || requested <= 0 || requested > 4_096) return Conversion.NONE;
        long a = value, b = target.value;
        while (b != 0L) { long remainder = a % b; a = b; b = remainder; }
        long sourceStep = target.value / a;
        long targetStep = value / a;
        long batches = Math.min(requested / sourceStep, 4_096L / targetStep);
        long consumed = batches * sourceStep;
        long issued = batches * targetStep;
        return batches == 0L ? Conversion.NONE : new Conversion((int) consumed, (int) issued, consumed * value);
    }

    public record Conversion(int sourceCount, int targetCount, long value) {
        public static final Conversion NONE = new Conversion(0, 0, 0L);
        public boolean possible() { return sourceCount > 0 && targetCount > 0; }
    }

    public static CoinDenomination[] descending() {
        return new CoinDenomination[]{ARCANE, AETHER, GOLD, SILVER, COPPER};
    }

    public static Optional<CoinDenomination> byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }
}
