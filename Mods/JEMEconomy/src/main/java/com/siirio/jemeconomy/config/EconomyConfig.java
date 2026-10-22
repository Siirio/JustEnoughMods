package com.siirio.jemeconomy.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Stored in each world's serverconfig; disabling preserves every existing balance and receipt. */
public final class EconomyConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("economy");
        ENABLED = builder.comment("Enable Coins, market, shipping and native Coin trades in this world.")
                .define("enabled", true);
        builder.pop();
        SPEC = builder.build();
    }
    private EconomyConfig() { }
}
