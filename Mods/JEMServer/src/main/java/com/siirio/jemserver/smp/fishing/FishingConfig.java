package com.siirio.jemserver.smp.fishing;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FishingConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue OVERWORLD_MIN, OVERWORLD_MAX, NETHER_MIN, NETHER_MAX, END_MIN, END_MAX;
    public static final ForgeConfigSpec.DoubleValue VOID_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue CAP_VOID;
    static {
        var b = new ForgeConfigSpec.Builder();
        OVERWORLD_MIN = b.defineInRange("overworldMinXp", 63, 0, 10000);
        OVERWORLD_MAX = b.defineInRange("overworldMaxXp", 112, 0, 10000);
        NETHER_MIN = b.defineInRange("netherMinXp", 150, 0, 10000);
        NETHER_MAX = b.defineInRange("netherMaxXp", 200, 0, 10000);
        END_MIN = b.defineInRange("endMinXp", 200, 0, 10000);
        END_MAX = b.defineInRange("endMaxXp", 350, 0, 10000);
        VOID_MULTIPLIER = b.defineInRange("voidXpMultiplier", 1.2, 1, 10);
        CAP_VOID = b.define("capVoidAtDimensionMaximum", false);
        SPEC = b.build();
    }
    private FishingConfig() {}
}
