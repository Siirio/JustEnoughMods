package com.siirio.jemlightingsandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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

    private static final Pattern WIND_COLOR_ENTRY = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+=#[a-fA-F0-9]{6}");
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue WIND;
    private static final ForgeConfigSpec.BooleanValue RIVER_MIST;
    private static final ForgeConfigSpec.BooleanValue SNOW_GUST;
    private static final ForgeConfigSpec.BooleanValue CAVE_AIR;
    private static final ForgeConfigSpec.BooleanValue CANOPY_LEAVES;
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
        ENABLED = builder.comment("Enable JEM Lighting and its atmosphere effects.").define("enabled", true);
        WIND = builder.comment("Show outdoor wind trails.").define("wind", true);
        RIVER_MIST = builder.comment("Show low mist over rivers and other bodies of water.").define("riverMist", true);
        SNOW_GUST = builder.comment("Show drifting gusts of snow near the ground.").define("snowGust", true);
        CAVE_AIR = builder.comment("Show airborne dust in caves.").define("caveAir", true);
        CANOPY_LEAVES = builder.comment("Show leaves falling from tree canopies.").define("canopyLeaves", true);
        MODE = builder.comment("How quickly loaded sections update their lighting and shadow geometry.")
                .defineEnum("mode", DEFAULT_MODE);
        LIGHT_DISTANCE = builder.comment("Maximum distance of a local light source, in blocks.")
                .defineInList("lightDistance", DEFAULT_DISTANCE, LIGHT_DISTANCES);
        LIGHTING_CHUNKS = builder.comment("Radius in chunks for detailed local lighting in manual mode.")
                .defineInRange("lightingChunks", DEFAULT_CHUNK_DISTANCE, MIN_LIGHTING_CHUNKS, MAX_LIGHTING_CHUNKS);
        FOLLOW_RENDER_DISTANCE = builder.comment("Use the effective Minecraft render distance for lighting.")
                .define("followRenderDistance", DEFAULT_FOLLOW_RENDER_DISTANCE);
        ATMOSPHERE = builder.comment("Enable biome fog, sky tint and weather effects.")
                .define("atmosphere", DEFAULT_ATMOSPHERE);
        RAIN_DENSITY = builder.comment("Particle density in ordinary precipitation across all biomes, in percent.")
                .defineInRange("rainDensity", DEFAULT_RAIN_DENSITY, MIN_WEATHER_DENSITY, MAX_WEATHER_DENSITY);
        STORM_DENSITY = builder.comment("Particle density during thunderstorms across all biomes, in percent.")
                .defineInRange("stormDensity", DEFAULT_STORM_DENSITY, MIN_WEATHER_DENSITY, MAX_WEATHER_DENSITY);
        DARK_CAMPFIRE_SMOKE = builder.comment("Use darker campfire smoke. Otherwise keep the native smoke color.")
                .define("darkCampfireSmoke", false);
        SHADOW_RESOLUTION = builder.comment("Resolution of celestial shadows and quality of block light shadows.")
                .defineInList("shadowResolution", DEFAULT_SHADOW_RESOLUTION, SHADOW_RESOLUTIONS);
        WIND_COLORS = builder.comment("Optional storm wind colors: biome_id=#RRGGBB. Exact biome IDs override defaults.")
                .defineListAllowEmpty("windColors", List.<String>of(), value -> value instanceof String entry
                        && WIND_COLOR_ENTRY.matcher(entry).matches());
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

    public static boolean atmosphereConfigured() {
        return configured(ATMOSPHERE, DEFAULT_ATMOSPHERE);
    }

    public static boolean atmosphereEnabled() {
        return enabled() && atmosphereConfigured();
    }

    public static boolean windEnabled() {
        return atmosphereEnabled() && configured(WIND, true);
    }

    public static boolean riverMistEnabled() {
        return atmosphereEnabled() && configured(RIVER_MIST, true);
    }

    public static boolean snowGustEnabled() {
        return atmosphereEnabled() && configured(SNOW_GUST, true);
    }

    public static boolean caveAirEnabled() {
        return atmosphereEnabled() && configured(CAVE_AIR, true);
    }

    public static boolean canopyLeavesEnabled() {
        return atmosphereEnabled() && configured(CANOPY_LEAVES, true);
    }

    private static boolean configured(ForgeConfigSpec.BooleanValue setting, boolean defaultValue) {
        return SPEC.isLoaded() ? setting.get() : defaultValue;
    }

    static int weatherDensity(boolean storm) {
        return SPEC.isLoaded() ? (storm ? STORM_DENSITY : RAIN_DENSITY).get()
                : storm ? DEFAULT_STORM_DENSITY : DEFAULT_RAIN_DENSITY;
    }

    public static boolean darkCampfireSmoke() {
        return SPEC.isLoaded() && DARK_CAMPFIRE_SMOKE.get();
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

    static List<String> parseWindColors(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        var entries = new ArrayList<String>();
        for (String text : value.split(",", -1)) {
            String entry = text.strip();
            if (!WIND_COLOR_ENTRY.matcher(entry).matches()) {
                return null;
            }
            entries.add(entry);
        }
        return List.copyOf(entries);
    }

    static final class Options {
        boolean enabled = LightingSettings.enabled();
        boolean atmosphere = atmosphereConfigured();
        boolean wind = configured(WIND, true);
        boolean riverMist = configured(RIVER_MIST, true);
        boolean snowGust = configured(SNOW_GUST, true);
        boolean caveAir = configured(CAVE_AIR, true);
        boolean canopyLeaves = configured(CANOPY_LEAVES, true);
        Mode mode = mode();
        int reachIndex = LIGHT_DISTANCES.indexOf(lightDistance());
        int chunkDistance = requestedChunkDistance();
        boolean followRenderDistance = followsRenderDistance();
        int shadowIndex = SHADOW_RESOLUTIONS.indexOf(shadowResolution());
        int rainDensity = weatherDensity(false);
        int stormDensity = weatherDensity(true);
        boolean darkSmoke = darkCampfireSmoke();
        String windColors = SPEC.isLoaded() ? String.join(", ", WIND_COLORS.get()) : "";

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
            ATMOSPHERE.set(atmosphere);
            WIND.set(wind);
            RIVER_MIST.set(riverMist);
            SNOW_GUST.set(snowGust);
            CAVE_AIR.set(caveAir);
            CANOPY_LEAVES.set(canopyLeaves);
            MODE.set(mode);
            LIGHT_DISTANCE.set(LIGHT_DISTANCES.get(reachIndex));
            LIGHTING_CHUNKS.set(chunkDistance);
            FOLLOW_RENDER_DISTANCE.set(followRenderDistance);
            SHADOW_RESOLUTION.set(SHADOW_RESOLUTIONS.get(shadowIndex));
            RAIN_DENSITY.set(rainDensity);
            STORM_DENSITY.set(stormDensity);
            DARK_CAMPFIRE_SMOKE.set(darkSmoke);
            var entries = parseWindColors(windColors);
            if (entries != null) {
                WIND_COLORS.set(entries);
            }
            SPEC.save();
        }
    }
}
