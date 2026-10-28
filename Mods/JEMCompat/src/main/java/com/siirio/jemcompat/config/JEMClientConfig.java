package com.siirio.jemcompat.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JEMClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_MOD_NAMES;
    public static final ForgeConfigSpec.BooleanValue VARIED_VILLAGER_OUTFITS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SHOW_MOD_NAMES = builder.define("showModNames", false);
        VARIED_VILLAGER_OUTFITS = builder.comment("Show all seven native villager outfit types in every biome. Appearance only; professions, trades and saved villager types stay native.")
                .define("variedVillagerOutfits", true);
        SPEC = builder.build();
    }

    private JEMClientConfig() {
    }
}
