package com.siirio.jemsettlements.happiness;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server balance for evidence-derived town needs, independent of currency. */
public final class SettlementMetricsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue GUARD_AREA, RESIDENT_AREA, ANIMAL_TYPES, SCAN_BUDGET, REPEAT_CAP, CATEGORY_CAP, SOCIAL_BASE_CAP, ANIMAL_BONUS, COMFORT_BONUS;
    static {
        var b = new ForgeConfigSpec.Builder();
        GUARD_AREA = b.comment("Square blocks protected by one living owned recruit holding position in town. Initial 97x97 town requires 5 guards.").defineInRange("guardCoverageArea", 2048, 64, 262144);
        RESIDENT_AREA = b.comment("Square blocks per social resident; at least two residents are required. Initial town requires 10.").defineInRange("residentCoverageArea", 1024, 64, 262144);
        ANIMAL_TYPES = b.comment("Distinct peaceful Animal entity types for full animal score; hostile/targeting animals do not count.").defineInRange("animalDiversityTarget", 4, 1, 32);
        SCAN_BUDGET = b.comment("Maximum actual block observations per server tick across all towns; no forced chunks.").defineInRange("comfortScanBudget", 1024, 64, 8192);
        REPEAT_CAP = b.comment("Maximum identical registry block contributions citywide; state/color variants are not extra IDs.").defineInRange("comfortRepeatCap", 3, 1, 16);
        CATEGORY_CAP = b.comment("Maximum contribution per city comfort category. Four categories can reach 100.").defineInRange("comfortCategoryCap", 25, 1, 100);
        SOCIAL_BASE_CAP = b.comment("Maximum happiness at zero social coverage; coverage linearly raises this ceiling to 100.").defineInRange("socialBaseHappinessCap", 40, 0, 99);
        ANIMAL_BONUS = b.comment("Maximum animal happiness addition, still bounded by social coverage.").defineInRange("animalHappinessBonus", 10, 0, 30);
        COMFORT_BONUS = b.comment("Additive personal resident mood from full city comfort; stored food/damage/pat stimuli remain separate.").defineInRange("residentComfortMoodBonus", 10, 0, 30);
        SPEC = b.build();
    }
    private SettlementMetricsConfig() { }
}
