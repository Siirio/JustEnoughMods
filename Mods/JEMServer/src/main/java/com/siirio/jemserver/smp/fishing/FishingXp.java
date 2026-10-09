package com.siirio.jemserver.smp.fishing;

public final class FishingXp {
    public static int calculate(int minimum, int maximum, int rarity, double difficulty,
                                double rodQuality, double conditions, double multiplier, boolean cap) {
        int low = Math.min(minimum, maximum), high = Math.max(minimum, maximum);
        double score = .45 * clamp(rarity / 4.0) + .35 * clamp(difficulty)
                + .15 * clamp(rodQuality) + .05 * clamp(conditions);
        return award(low, high, score, multiplier, cap);
    }
    public static int[] bounds(int minimum, int maximum, double multiplier, boolean cap) {
        int low = Math.min(minimum, maximum), high = Math.max(minimum, maximum);
        return new int[]{award(low, high, 0, multiplier, cap), award(low, high, 1, multiplier, cap)};
    }
    private static int award(int low, int high, double score, double multiplier, boolean cap) {
        int xp = (int) Math.round((low + (high - low) * score) * multiplier);
        return cap ? Math.min(high, xp) : xp;
    }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private FishingXp() {}
}
