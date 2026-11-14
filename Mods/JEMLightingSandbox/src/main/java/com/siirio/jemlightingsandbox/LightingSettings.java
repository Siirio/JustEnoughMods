package com.siirio.jemlightingsandbox;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
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
    private static final boolean DEFAULT_ATMOSPHERE = true;
    private static final int DEFAULT_RAIN_DENSITY = 100;
    private static final int DEFAULT_STORM_DENSITY = 150;
    static final List<Integer> SHADOW_RESOLUTIONS = List.of(256, 512, 1024, 2048);
    private static final int DEFAULT_SHADOW_RESOLUTION = 256;
    static final int MIN_WEATHER_DENSITY = 25;
    static final int MAX_WEATHER_DENSITY = 200;
    static final List<Integer> LIGHT_DISTANCES = List.of(8, 12, 15, 20);
    static final int MIN_LIGHTING_CHUNKS = 2;
    static final int MAX_LIGHTING_CHUNKS = 32;

    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.EnumValue<Mode> MODE;
    private static final ForgeConfigSpec.ConfigValue<Integer> LIGHT_DISTANCE;
    private static final ForgeConfigSpec.IntValue LIGHTING_CHUNKS;
    private static final ForgeConfigSpec.BooleanValue FOLLOW_RENDER_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue ATMOSPHERE;
    private static final ForgeConfigSpec.IntValue RAIN_DENSITY;
    private static final ForgeConfigSpec.IntValue STORM_DENSITY;
    private static final ForgeConfigSpec.BooleanValue DARK_CAMPFIRE_SMOKE;
    private static final ForgeConfigSpec.ConfigValue<Integer> SHADOW_RESOLUTION;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> WIND_COLORS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        MODE = builder.comment("How frequently the local light volume is refreshed.")
                .defineEnum("mode", DEFAULT_MODE);
        LIGHT_DISTANCE = builder.comment("Maximum distance of a local light source, in blocks.")
                .defineInList("lightDistance", DEFAULT_DISTANCE, LIGHT_DISTANCES);
        LIGHTING_CHUNKS = builder.comment("Radius in chunks for detailed local lighting in manual mode.")
                .defineInRange("lightingChunks", DEFAULT_CHUNK_DISTANCE, MIN_LIGHTING_CHUNKS, MAX_LIGHTING_CHUNKS);
        FOLLOW_RENDER_DISTANCE = builder.comment("Use the effective Minecraft render distance for lighting.")
                .define("followRenderDistance", DEFAULT_FOLLOW_RENDER_DISTANCE);
        ATMOSPHERE = builder.comment("Enable light shafts and weather effects.")
                .define("atmosphere", DEFAULT_ATMOSPHERE);
        RAIN_DENSITY = builder.comment("Particle density in ordinary precipitation across all biomes, in percent.")
                .defineInRange("rainDensity", DEFAULT_RAIN_DENSITY, MIN_WEATHER_DENSITY, MAX_WEATHER_DENSITY);
        STORM_DENSITY = builder.comment("Particle density during thunderstorms across all biomes, in percent.")
                .defineInRange("stormDensity", DEFAULT_STORM_DENSITY, MIN_WEATHER_DENSITY, MAX_WEATHER_DENSITY);
        DARK_CAMPFIRE_SMOKE = builder.comment("Use darker campfire smoke. Otherwise keep the native smoke color.")
                .define("darkCampfireSmoke", false);
        SHADOW_RESOLUTION = builder.comment("Resolution of the sun and moon entity shadow map.")
                .defineInList("shadowResolution", DEFAULT_SHADOW_RESOLUTION, SHADOW_RESOLUTIONS);
        WIND_COLORS = builder.comment("Optional storm wind colors: biome_id=#RRGGBB. Exact biome IDs override defaults.")
                .defineListAllowEmpty("windColors", List.<String>of(), value -> value instanceof String entry
                        && entry.matches("[a-z0-9_.-]+:[a-z0-9_./-]+=#[a-fA-F0-9]{6}"));
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

    public static boolean atmosphereEnabled() {
        return SPEC.isLoaded() ? ATMOSPHERE.get() : DEFAULT_ATMOSPHERE;
    }

    static int weatherDensity(boolean storm) {
        return SPEC.isLoaded() ? (storm ? STORM_DENSITY : RAIN_DENSITY).get()
                : storm ? DEFAULT_STORM_DENSITY : DEFAULT_RAIN_DENSITY;
    }

    static void setWeatherDensity(boolean storm, int density) {
        saveInt(storm ? STORM_DENSITY : RAIN_DENSITY, density);
    }

    public static boolean darkCampfireSmoke() {
        return SPEC.isLoaded() && DARK_CAMPFIRE_SMOKE.get();
    }

    static void setDarkCampfireSmoke(boolean enabled) {
        DARK_CAMPFIRE_SMOKE.set(enabled);
        SPEC.save();
    }

    public static int shadowResolution() {
        return SPEC.isLoaded() ? SHADOW_RESOLUTION.get() : DEFAULT_SHADOW_RESOLUTION;
    }

    static void setShadowResolution(int resolution) {
        SHADOW_RESOLUTION.set(resolution);
        SPEC.save();
    }

    private static void saveInt(ForgeConfigSpec.IntValue setting, int value) {
        if (SPEC.isLoaded() && setting.get() != value) {
            setting.set(value);
            SPEC.save();
        }
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

    public static void setRequestedChunkDistance(int distance) {
        if (distance < MIN_LIGHTING_CHUNKS || distance > MAX_LIGHTING_CHUNKS) {
            throw new IllegalArgumentException("Unsupported lighting chunk distance: " + distance);
        }
        if (distance != requestedChunkDistance()) {
            LIGHTING_CHUNKS.set(distance);
            SPEC.save();
        }
    }

    public static void setFollowsRenderDistance(boolean follow) {
        FOLLOW_RENDER_DISTANCE.set(follow);
        SPEC.save();
    }

    public static void setAtmosphereEnabled(boolean enabled) {
        ATMOSPHERE.set(enabled);
        SPEC.save();
    }

    static int windColor(ResourceLocation biome) {
        if (biome == null || !SPEC.isLoaded()) {
            return -1;
        }
        String prefix = biome + "=#";
        for (String entry : WIND_COLORS.get()) {
            if (entry.startsWith(prefix)) {
                return Integer.parseInt(entry.substring(prefix.length()), 16);
            }
        }
        return -1;
    }

    static void setWindColor(ResourceLocation biome, int color) {
        if (!SPEC.isLoaded()) {
            return;
        }
        String prefix = biome + "=#";
        var entries = new ArrayList<String>(WIND_COLORS.get());
        entries.removeIf(entry -> entry.startsWith(prefix));
        if (color >= 0) {
            entries.add(prefix + String.format(Locale.ROOT, "%06X", color));
        }
        WIND_COLORS.set(entries);
        SPEC.save();
    }
}
