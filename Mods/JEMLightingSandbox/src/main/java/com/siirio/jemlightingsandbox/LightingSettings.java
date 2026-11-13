package com.siirio.jemlightingsandbox;

import java.util.List;
import java.util.Objects;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class LightingSettings {
    public enum Mode {
        SLOW,
        FAST,
        REALTIME
    }

    private static final Mode DEFAULT_MODE = Mode.SLOW;
    private static final int DEFAULT_DISTANCE = 15;
    private static final boolean DEFAULT_ATMOSPHERE = true;
    static final List<Integer> LIGHT_DISTANCES = List.of(8, 12, 15, 20);

    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.EnumValue<Mode> MODE;
    private static final ForgeConfigSpec.ConfigValue<Integer> LIGHT_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue ATMOSPHERE;

    static {
        var builder = new ForgeConfigSpec.Builder();
        MODE = builder.comment("How frequently the local light volume is refreshed.")
                .defineEnum("mode", DEFAULT_MODE);
        LIGHT_DISTANCE = builder.comment("Maximum distance of a local light source, in blocks.")
                .defineInList("lightDistance", DEFAULT_DISTANCE, LIGHT_DISTANCES);
        ATMOSPHERE = builder.comment("Enable sunlight shafts and distance haze.")
                .define("atmosphere", DEFAULT_ATMOSPHERE);
        SPEC = builder.build();
    }

    private LightingSettings() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    public static Mode mode() {
        return SPEC.isLoaded() ? MODE.get() : DEFAULT_MODE;
    }

    public static int lightDistance() {
        return SPEC.isLoaded() ? LIGHT_DISTANCE.get() : DEFAULT_DISTANCE;
    }

    public static boolean atmosphereEnabled() {
        return SPEC.isLoaded() ? ATMOSPHERE.get() : DEFAULT_ATMOSPHERE;
    }

    public static void setMode(Mode mode) {
        MODE.set(Objects.requireNonNull(mode));
        SPEC.save();
    }

    public static void setLightDistance(int distance) {
        if (!LIGHT_DISTANCES.contains(distance)) {
            throw new IllegalArgumentException("Unsupported light distance: " + distance);
        }
        LIGHT_DISTANCE.set(distance);
        SPEC.save();
    }

    public static void setAtmosphereEnabled(boolean enabled) {
        ATMOSPHERE.set(enabled);
        SPEC.save();
    }
}
