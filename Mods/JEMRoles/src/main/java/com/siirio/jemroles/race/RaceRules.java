package com.siirio.jemroles.race;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public final class RaceRules {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue GUARDIAN_COOLDOWN;
    public static final ForgeConfigSpec.IntValue LIFE_CHORUS_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SCULK_VEINS_COOLDOWN;
    public static final ForgeConfigSpec.IntValue VERDANT_DOMINION_COOLDOWN;
    public static final ForgeConfigSpec.IntValue RIFT_AMBUSH_COOLDOWN;
    public static final ForgeConfigSpec.IntValue VOID_PROJECTILE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue GRAVITY_DURATION;
    public static final ForgeConfigSpec.IntValue GRAVITY_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SECOND_BLOSSOM_COOLDOWN;
    public static final ForgeConfigSpec.IntValue LAVA_DRINK_COOLDOWN;
    public static final ForgeConfigSpec.IntValue LAVA_BREATH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> END_KIN_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> END_BOSS_ENTITIES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("cooldowns");
        GUARDIAN_COOLDOWN = b.defineInRange("ancestralGuardianTicks", 1_200, 20, 1_200);
        LIFE_CHORUS_COOLDOWN = b.defineInRange("lifeChorusTicks", 500, 20, 72_000);
        SCULK_VEINS_COOLDOWN = b.defineInRange("sculkVeinsTicks", 400, 20, 72_000);
        VERDANT_DOMINION_COOLDOWN = b.defineInRange("verdantDominionTicks", 500, 20, 72_000);
        RIFT_AMBUSH_COOLDOWN = b.defineInRange("riftAmbushTicks", 1_200, 20, 72_000);
        VOID_PROJECTILE_COOLDOWN = b.defineInRange("voidProjectileTicks", 2_400, 20, 72_000);
        GRAVITY_DURATION = b.defineInRange("gravityDenialDurationTicks", 100, 20, 1_200);
        GRAVITY_COOLDOWN = b.defineInRange("gravityDenialCooldownTicks", 1_600, 20, 72_000);
        SECOND_BLOSSOM_COOLDOWN = b.defineInRange("secondBlossomTicks", 24_000, 20, 240_000);
        LAVA_DRINK_COOLDOWN = b.defineInRange("lavaDrinkTicks", 3_600, 20, 72_000);
        b.pop();
        b.push("resources");
        LAVA_BREATH = b.defineInRange("lavaBreathTicks", 300, 20, 2_400);
        b.pop();
        b.push("compatibility");
        END_KIN_ENTITIES = b.defineListAllowEmpty(List.of("endKinEntities"),
                () -> List.of("minecraft:enderman", "minecraft:endermite", "minecraft:shulker"),
                value -> value instanceof String text && text.contains(":"));
        END_BOSS_ENTITIES = b.defineListAllowEmpty(List.of("endBossEntities"),
                () -> List.of("minecraft:ender_dragon", "minecraft:wither"),
                value -> value instanceof String text && text.contains(":"));
        b.pop();
        SPEC = b.build();
    }

    private RaceRules() {}

}
