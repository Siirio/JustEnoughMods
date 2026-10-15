package com.siirio.jemroles.client.vfx;

public final class RaceVfxPalettes {
    public static final int ANCESTOR_DARK = 0x5A401F;
    public static final int ANCESTOR = 0xE8C878;
    public static final int ANCESTOR_LIGHT = 0xFFF1C2;
    public static final int VOID_DARK = 0x241236;
    public static final int VOID = 0x8F47D8;
    public static final int VOID_LIGHT = 0x62E5E7;
    public static final int BLOOM_DARK = 0x24451F;
    public static final int BLOOM = 0x62B84D;
    public static final int BLOOM_LIGHT = 0xE8D96B;
    public static final int SCULK_DARK = 0x071D23;
    public static final int SCULK = 0x16A6A8;
    public static final int SCULK_LIGHT = 0x78F4DD;
    public static final int NETHER_DARK = 0x3A0B08;
    public static final int NETHER = 0xD73A16;
    public static final int NETHER_LIGHT = 0xFFB12B;

    private RaceVfxPalettes() {}

    public static int mix(int first, int second, double amount) {
        double weight = Math.max(0.0D, Math.min(1.0D, amount));
        int red = (int) ((first >> 16 & 255) * (1.0D - weight) + (second >> 16 & 255) * weight);
        int green = (int) ((first >> 8 & 255) * (1.0D - weight) + (second >> 8 & 255) * weight);
        int blue = (int) ((first & 255) * (1.0D - weight) + (second & 255) * weight);
        return red << 16 | green << 8 | blue;
    }
}
