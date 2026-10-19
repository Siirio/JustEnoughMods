package com.siirio.jemsettlements.happiness;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-owned housing and immediate safety conditions. */
public final class HousingConditionConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue BED_RADIUS, FLOOR_PER_RESIDENT, CROWD_MAX;
    public static final ForgeConfigSpec.IntValue LIGHT_GOOD, LIGHT_MILD, LIGHT_MILD_PENALTY, LIGHT_SEVERE_PENALTY, LIGHT_INTERVAL;
    public static final ForgeConfigSpec.IntValue FEAR_RADIUS, FEAR_INTERVAL, FEAR_VISIBLE, FEAR_GLASS, FEAR_MAX, FEAR_RECOVERY;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("housing");
        BED_RADIUS = builder.comment("Horizontal personal bed spacing radius within the same room.").defineInRange("bedRadius", 2, 1, 8);
        FLOOR_PER_RESIDENT = builder.comment("Useful reachable interior floor blocks required per resident.").defineInRange("floorPerResident", 9, 1, 256);
        CROWD_MAX = builder.defineInRange("maximumCrowdingPenalty", 30, 0, 100);
        LIGHT_GOOD = builder.comment("Minimum block light for a well-lit home; sky light is excluded.").defineInRange("goodBlockLight", 8, 0, 15);
        LIGHT_MILD = builder.defineInRange("mildBlockLight", 4, 0, 15);
        LIGHT_MILD_PENALTY = builder.defineInRange("mildLightPenalty", 5, 0, 100);
        LIGHT_SEVERE_PENALTY = builder.defineInRange("severeLightPenalty", 10, 0, 100);
        LIGHT_INTERVAL = builder.defineInRange("lightIntervalTicks", 600, 20, 24000);
        FEAR_RADIUS = builder.defineInRange("hostileRadius", 16, 1, 64);
        FEAR_INTERVAL = builder.defineInRange("fearIntervalTicks", 100, 20, 24000);
        FEAR_VISIBLE = builder.comment("Penalty while a hostile is visible nearby.").defineInRange("visibleHostileFear", 10, 0, 100);
        FEAR_GLASS = builder.comment("Small fear when a hostile is visible through glass without a confirmed open route.").defineInRange("glassHostileFear", 5, 0, 100);
        FEAR_MAX = builder.comment("Fear after direct hostile damage.").defineInRange("attackedFear", 20, 0, 100);
        FEAR_RECOVERY = builder.comment("Fear removed per completed interval without a visible threat.").defineInRange("fearRecovery", 1, 0, 100);
        builder.pop(); SPEC = builder.build();
    }
    private HousingConditionConfig() { }
}
