package com.siirio.jemluxium.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LuxiumClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue HAS_PREVIOUS_OCULUS_STATE;
    public static final ForgeConfigSpec.BooleanValue PREVIOUS_SHADERS_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Personal Oculus state remembered while the Luxium shortcut is active.");
        HAS_PREVIOUS_OCULUS_STATE = builder.define("hasPreviousOculusState", false);
        PREVIOUS_SHADERS_ENABLED = builder.define("previousShadersEnabled", false);
        SPEC = builder.build();
    }

    private LuxiumClientConfig() {
    }

    public static void remember(boolean shadersEnabled) {
        PREVIOUS_SHADERS_ENABLED.set(shadersEnabled);
        HAS_PREVIOUS_OCULUS_STATE.set(true);
        SPEC.save();
    }

    public static void clearRememberedState() {
        HAS_PREVIOUS_OCULUS_STATE.set(false);
        PREVIOUS_SHADERS_ENABLED.set(false);
        SPEC.save();
    }
}
