package com.siirio.jemseasonshud.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HudConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_TIME_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_SEASON_HUD;
    public static final ForgeConfigSpec.IntValue TIME_HUD_OFFSET_X;
    public static final ForgeConfigSpec.IntValue TIME_HUD_OFFSET_Y;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("timeHud");
        SHOW_TIME_HUD = builder.comment("Show the day and time dial while player inventory is open.")
                .define("showInventoryDayNight", true);
        SHOW_SEASON_HUD = builder.comment("Show the native seasonal dial while player inventory is open, when Serene Seasons is installed.")
                .define("showInventorySeasons", true);
        TIME_HUD_OFFSET_X = builder.comment("Horizontal offset from the top center of the open player inventory screen, in GUI pixels.")
                .defineInRange("horizontalOffset", 8, 0, 4096);
        TIME_HUD_OFFSET_Y = builder.comment("Distance from the top edge of the open player inventory screen, in GUI pixels.")
                .defineInRange("verticalOffset", 8, 0, 4096);
        builder.pop();
        SPEC = builder.build();
    }

    private HudConfig() {
    }
}
