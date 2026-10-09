package com.siirio.jemadaptiveculling.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JEMConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENTITY_OCCLUSION;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_OCCLUSION;
    public static final ForgeConfigSpec.IntValue RAY_BUDGET;
    public static final ForgeConfigSpec.IntValue TIME_BUDGET_MICROS;
    public static final ForgeConfigSpec.IntValue MAX_DISTANCE;
    public static final ForgeConfigSpec.EnumValue<CompatibilityMode> COMPATIBILITY_MODE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ENTITY_OCCLUSION = builder.define("entityOcclusion", true);
        BLOCK_ENTITY_OCCLUSION = builder.define("blockEntityOcclusion", true);
        RAY_BUDGET = builder.defineInRange("raysPerWindow", 48, 9, 144);
        TIME_BUDGET_MICROS = builder.defineInRange("microsecondsPerWindow", 500, 100, 2000);
        MAX_DISTANCE = builder.defineInRange("maxOcclusionDistance", 64, 16, 128);
        COMPATIBILITY_MODE = builder.defineEnum("compatibilityMode", CompatibilityMode.AUTOMATIC);
        SPEC = builder.build();
    }

    private JEMConfig() {
    }

    public enum CompatibilityMode {
        AUTOMATIC,
        CONSERVATIVE,
        NEVER_CULL
    }
}
