package com.siirio.jemadaptiveculling.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class JEMConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENTITY_OCCLUSION;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_OCCLUSION;
    public static final ForgeConfigSpec.IntValue RAY_BUDGET;
    public static final ForgeConfigSpec.IntValue TIME_BUDGET_MICROS;
    public static final ForgeConfigSpec.IntValue MAX_DISTANCE;
    public static final ForgeConfigSpec.EnumValue<CompatibilityMode> COMPATIBILITY_MODE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_BLOCK_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_OCCLUDERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ENTITY_OCCLUSION = builder.define("entityOcclusion", true);
        BLOCK_ENTITY_OCCLUSION = builder.define("blockEntityOcclusion", true);
        RAY_BUDGET = builder.comment("Maximum center-ray certificate searches per 50 ms; deferred objects stay visible.")
                .defineInRange("raysPerWindow", 48, 9, 144);
        TIME_BUDGET_MICROS = builder.defineInRange("microsecondsPerWindow", 500, 100, 2000);
        MAX_DISTANCE = builder.defineInRange("maxOcclusionDistance", 64, 16, 128);
        COMPATIBILITY_MODE = builder.defineEnum("compatibilityMode", CompatibilityMode.AUTOMATIC);
        EXCLUDED_ENTITIES = builder.comment("Entity type IDs whose extended models must remain visible.")
                .defineListAllowEmpty(List.of("excludedEntityTypes"), List.of(), value -> value instanceof String);
        EXCLUDED_BLOCK_ENTITIES = builder.comment("Block entity type IDs whose renderer bounds cannot safely be culled.")
                .defineListAllowEmpty(List.of("excludedBlockEntityTypes"), List.of(), value -> value instanceof String);
        EXCLUDED_OCCLUDERS = builder.comment("Block IDs with resource-pack holes or visual opacity unlike their native occlusion shape.")
                .defineListAllowEmpty(List.of("excludedOccluderBlocks"), List.of(), value -> value instanceof String);
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
