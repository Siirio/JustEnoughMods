package com.siirio.jemcompat.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JEMClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_MOD_NAMES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SHOW_MOD_NAMES = builder.define("showModNames", false);
        SPEC = builder.build();
    }

    private JEMClientConfig() {
    }
}
