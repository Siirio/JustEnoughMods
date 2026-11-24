package com.siirio.jemlightingsandbox;

import java.util.List;
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
    private static final int DEFAULT_CHUNK_DISTANCE = 8;
    private static final boolean DEFAULT_FOLLOW_RENDER_DISTANCE = true;
    static final List<Integer> SHADOW_RESOLUTIONS = List.of(256, 512, 1024, 2048);
    private static final int DEFAULT_SHADOW_RESOLUTION = 256;
    static final List<Integer> LIGHT_DISTANCES = List.of(8, 12, 15, 20);
    static final int MIN_LIGHTING_CHUNKS = 2;
    static final int MAX_LIGHTING_CHUNKS = 32;

    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.EnumValue<Mode> MODE;
    private static final ForgeConfigSpec.ConfigValue<Integer> LIGHT_DISTANCE;
    private static final ForgeConfigSpec.IntValue LIGHTING_CHUNKS;
    private static final ForgeConfigSpec.BooleanValue FOLLOW_RENDER_DISTANCE;
    private static final ForgeConfigSpec.ConfigValue<Integer> SHADOW_RESOLUTION;

    static {
        var builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.comment("Enable JEM Lighting terrain light and shadow experiments.").define("enabled", true);
        MODE = builder.comment("How quickly loaded sections update their lighting and shadow geometry.")
                .defineEnum("mode", DEFAULT_MODE);
        LIGHT_DISTANCE = builder.comment("Maximum distance of a local light source, in blocks.")
                .defineInList("lightDistance", DEFAULT_DISTANCE, LIGHT_DISTANCES);
        LIGHTING_CHUNKS = builder.comment("Radius in chunks for detailed local lighting in manual mode.")
                .defineInRange("lightingChunks", DEFAULT_CHUNK_DISTANCE, MIN_LIGHTING_CHUNKS, MAX_LIGHTING_CHUNKS);
        FOLLOW_RENDER_DISTANCE = builder.comment("Use the effective Minecraft render distance for lighting.")
                .define("followRenderDistance", DEFAULT_FOLLOW_RENDER_DISTANCE);
        SHADOW_RESOLUTION = builder.comment("Resolution of celestial shadows and quality of block light shadows.")
                .defineInList("shadowResolution", DEFAULT_SHADOW_RESOLUTION, SHADOW_RESOLUTIONS);
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

    public static int requestedChunkDistance() {
        return SPEC.isLoaded() ? LIGHTING_CHUNKS.get() : DEFAULT_CHUNK_DISTANCE;
    }

    public static boolean followsRenderDistance() {
        return SPEC.isLoaded() ? FOLLOW_RENDER_DISTANCE.get() : DEFAULT_FOLLOW_RENDER_DISTANCE;
    }

    public static int lightingChunks(int effectiveRenderDistance) {
        return followsRenderDistance()
                ? Math.max(MIN_LIGHTING_CHUNKS, Math.min(MAX_LIGHTING_CHUNKS, effectiveRenderDistance))
                : requestedChunkDistance();
    }

    public static boolean enabled() {
        return configured(ENABLED, true);
    }

    private static boolean configured(ForgeConfigSpec.BooleanValue setting, boolean defaultValue) {
        return SPEC.isLoaded() ? setting.get() : defaultValue;
    }

    public static int shadowResolution() {
        return SPEC.isLoaded() ? SHADOW_RESOLUTION.get() : DEFAULT_SHADOW_RESOLUTION;
    }

    public static int pointShadowSamples() {
        return switch (shadowResolution()) {
            case 2048 -> 5;
            case 1024 -> 4;
            case 512 -> 3;
            default -> 2;
        };
    }

    static final class Options {
        boolean enabled = LightingSettings.enabled();
        Mode mode = mode();
        int reachIndex = LIGHT_DISTANCES.indexOf(lightDistance());
        int chunkDistance = requestedChunkDistance();
        boolean followRenderDistance = followsRenderDistance();
        int shadowIndex = SHADOW_RESOLUTIONS.indexOf(shadowResolution());

        void setEnabled(boolean value) {
            enabled = value;
            if (SPEC.isLoaded()) {
                // Embeddium reloads the renderer before it persists option storage.
                ENABLED.set(value);
            }
        }

        void save() {
            if (!SPEC.isLoaded()) {
                return;
            }
            MODE.set(mode);
            LIGHT_DISTANCE.set(LIGHT_DISTANCES.get(reachIndex));
            LIGHTING_CHUNKS.set(chunkDistance);
            FOLLOW_RENDER_DISTANCE.set(followRenderDistance);
            SHADOW_RESOLUTION.set(SHADOW_RESOLUTIONS.get(shadowIndex));
            SPEC.save();
        }
    }
}
