package com.siirio.jemclaims;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClaimsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> TOOL_NAME;
    public static final ForgeConfigSpec.IntValue NAME_LENGTH;
    public static final ForgeConfigSpec.BooleanValue SHOW_NAME;
    public static final ForgeConfigSpec.BooleanValue SHOW_OWNER;

    public static final ForgeConfigSpec.IntValue MIN_AREA, MAX_TERRITORIES, BORDER_TICKS, BORDER_RADIUS, MAP_COLOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        TOOL_NAME = builder.define("claimToolName", "Claim Tool");
        NAME_LENGTH = builder.defineInRange("maxNameLength", 48, 1, 64);
        SHOW_NAME = builder.define("showClaimNameDefault", true);
        SHOW_OWNER = builder.define("showClaimOwnerDefault", true);
        MIN_AREA = builder.defineInRange("minimumClaimArea", 2, 2, Integer.MAX_VALUE);
        MAX_TERRITORIES = builder.defineInRange("maximumTerritories", 5, 1, 128);
        BORDER_TICKS = builder.defineInRange("borderPreviewTicks", 20, 1, 1200);
        BORDER_RADIUS = builder.defineInRange("borderPreviewRadius", 64, 1, 128);
        MAP_COLOR = builder.defineInRange("claimMapColor", 0x70B8DC, 0, 0xFFFFFF);
        SPEC = builder.build();
    }

    private ClaimsConfig() {}
}
