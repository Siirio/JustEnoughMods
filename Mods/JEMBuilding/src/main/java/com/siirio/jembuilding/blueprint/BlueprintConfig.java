package com.siirio.jembuilding.blueprint;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BlueprintConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_CAPTURE_BLOCKS;
    public static final ForgeConfigSpec.IntValue MAX_AXIS;
    public static final ForgeConfigSpec.IntValue MAX_MOVE_DISTANCE;
    public static final ForgeConfigSpec.IntValue WORK_PER_TICK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("blueprint_roll");
        MAX_CAPTURE_BLOCKS = builder.comment("Maximum non-air blocks stored by one blueprint")
                .defineInRange("maxCaptureBlocks", 131_072, 1_024, 1_000_000);
        MAX_AXIS = builder.comment("Maximum inclusive selection length on each axis")
                .defineInRange("maxAxis", 256, 16, 512);
        MAX_MOVE_DISTANCE = builder.comment("Maximum Survival Move distance from the original selection")
                .defineInRange("maxMoveDistance", 256, 16, 4096);
        WORK_PER_TICK = builder.comment("Maximum blueprint world mutations per server tick")
                .defineInRange("workPerTick", 512, 32, 4096);
        builder.pop();
        SPEC = builder.build();
    }

    private BlueprintConfig() { }
}
