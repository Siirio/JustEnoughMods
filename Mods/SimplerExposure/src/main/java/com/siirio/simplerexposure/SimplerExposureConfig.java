package com.siirio.simplerexposure;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SimplerExposureConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue FILM_CAPACITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        FILM_CAPACITY = builder.comment("Successful photographs per film roll. Exposure 1.9.21 natively supports at most 16.")
                .defineInRange("filmCapacity", 16, 1, 16);
        SPEC = builder.build();
    }

    private SimplerExposureConfig() { }
}
