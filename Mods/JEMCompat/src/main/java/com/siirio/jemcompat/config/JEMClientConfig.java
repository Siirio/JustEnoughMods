package com.siirio.jemcompat.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JEMClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_MOD_NAMES;
    public static final ForgeConfigSpec.BooleanValue VARIED_VILLAGER_OUTFITS;
    public static final ForgeConfigSpec.BooleanValue SHOW_TIME_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_SEASON_HUD;
    public static final ForgeConfigSpec.IntValue TIME_HUD_OFFSET_X;
    public static final ForgeConfigSpec.IntValue TIME_HUD_OFFSET_Y;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SHOW_MOD_NAMES = builder.define("showModNames", false);
        VARIED_VILLAGER_OUTFITS = builder.comment("Show all seven native villager outfit types in every biome. Appearance only; professions, trades and saved villager types stay native.")
                .define("variedVillagerOutfits", true);
        builder.push("timeHud");
        SHOW_TIME_HUD = builder.comment("Show the approved day/night dial while player inventory is open. This inventory setting replaces the retired unfinished HUD toggle.")
                .define("showInventoryDayNight", true);
        SHOW_SEASON_HUD = builder.comment("Show the approved native seasonal dial while player inventory is open, when Serene Seasons is installed.")
                .define("showInventorySeasons", true);
        TIME_HUD_OFFSET_X = builder.comment("Distance from the right edge of the open player inventory screen, in GUI pixels.")
                .defineInRange("horizontalOffset", 8, 0, 4096);
        TIME_HUD_OFFSET_Y = builder.comment("Distance from the top edge of the open player inventory screen, in GUI pixels.")
                .defineInRange("verticalOffset", 8, 0, 4096);
        builder.pop();
        SPEC = builder.build();
    }

    private JEMClientConfig() {
    }
}
