package com.siirio.jemroles.vfx;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RaceVfxClientConfig {
    public enum Quality { LOW, MEDIUM, HIGH, ULTRA }

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.EnumValue<Quality> QUALITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("raceVfx");
        QUALITY = builder.comment("Decorative race VFX density. Critical telegraphs always render.")
                .defineEnum("quality", Quality.HIGH);
        builder.pop();
        SPEC = builder.build();
    }

    private RaceVfxClientConfig() {}

    public static Quality quality() { return QUALITY.get(); }

    public static int decorativeParticleLimit() {
        return switch (QUALITY.get()) {
            case LOW -> 96;
            case MEDIUM -> 192;
            case HIGH -> 384;
            case ULTRA -> 640;
        };
    }

    public static int trailSamples() {
        return switch (QUALITY.get()) {
            case LOW -> 4;
            case MEDIUM -> 6;
            case HIGH -> 9;
            case ULTRA -> 12;
        };
    }

    public static double decorativeDistance() {
        return switch (QUALITY.get()) {
            case LOW -> 24.0D;
            case MEDIUM -> 40.0D;
            case HIGH -> 64.0D;
            case ULTRA -> 96.0D;
        };
    }
}
