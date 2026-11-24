package com.siirio.jematmosphere;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class AtmosphereSettings {
    private static final Pattern WIND_COLOR_ENTRY = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+=#[a-fA-F0-9]{6}");
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue WIND;
    private static final ForgeConfigSpec.BooleanValue FOG;
    private static final ForgeConfigSpec.BooleanValue SKY_COLORS;
    private static final ForgeConfigSpec.BooleanValue RIVER_MIST;
    private static final ForgeConfigSpec.BooleanValue SNOW_GUST;
    private static final ForgeConfigSpec.BooleanValue CAVE_AIR;
    private static final ForgeConfigSpec.BooleanValue CANOPY_LEAVES;
    private static final ForgeConfigSpec.BooleanValue CAMPFIRE_SMOKE;
    private static final ForgeConfigSpec.BooleanValue DARK_CAMPFIRE_SMOKE;
    private static final ForgeConfigSpec.BooleanValue AURORA;
    private static final ForgeConfigSpec.BooleanValue RAINBOW;
    private static final ForgeConfigSpec.BooleanValue UNDERWATER_HAZE;
    private static final ForgeConfigSpec.BooleanValue HEAT_SHIMMER;
    private static final ForgeConfigSpec.BooleanValue DISTANT_LIGHTNING;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> WIND_COLORS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.comment("Enable JEM atmosphere effects.").define("enabled", true);
        WIND = builder.comment("Show outdoor wind trails.").define("wind", true);
        FOG = builder.comment("Show biome-aware distance fog and haze.").define("fog", true);
        SKY_COLORS = builder.comment("Apply biome and weather sky colors.").define("skyColors", true);
        RIVER_MIST = builder.comment("Show low mist over rivers and other bodies of water.").define("riverMist", true);
        SNOW_GUST = builder.comment("Show drifting gusts of snow near the ground.").define("snowGust", true);
        CAVE_AIR = builder.comment("Show airborne dust in caves.").define("caveAir", true);
        CANOPY_LEAVES = builder.comment("Show leaves falling from tree canopies.").define("canopyLeaves", true);
        CAMPFIRE_SMOKE = builder.comment("Apply wind and plume behavior to campfire smoke.").define("campfireSmoke", true);
        DARK_CAMPFIRE_SMOKE = builder.comment("Use darker campfire smoke.").define("darkCampfireSmoke", false);
        AURORA = builder.comment("Show auroras in cold biomes at night.").define("aurora", true);
        RAINBOW = builder.comment("Show a fading rainbow after rain.").define("rainbow", true);
        UNDERWATER_HAZE = builder.comment("Show underwater haze and subtle refraction.").define("underwaterHaze", true);
        HEAT_SHIMMER = builder.comment("Show heat shimmer in hot air and near heat sources.").define("heatShimmer", true);
        DISTANT_LIGHTNING = builder.comment("Show distant atmospheric lightning flashes.").define("distantLightning", true);
        WIND_COLORS = builder.comment("Optional wind colors: biome_id=#RRGGBB. Exact biome IDs override defaults.")
                .defineListAllowEmpty("windColors", List.<String>of(), value -> value instanceof String entry
                        && WIND_COLOR_ENTRY.matcher(entry).matches());
        SPEC = builder.build();
    }

    private AtmosphereSettings() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    public static boolean atmosphereEnabled() {
        return configured(ENABLED, true);
    }

    public static boolean windEnabled() {
        return atmosphereEnabled() && configured(WIND, true);
    }

    public static boolean fogEnabled() {
        return atmosphereEnabled() && configured(FOG, true);
    }

    public static boolean skyColorsEnabled() {
        return atmosphereEnabled() && configured(SKY_COLORS, true);
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

    public static boolean campfireSmokeEnabled() {
        return atmosphereEnabled() && configured(CAMPFIRE_SMOKE, true);
    }

    public static boolean darkCampfireSmoke() {
        return campfireSmokeEnabled() && configured(DARK_CAMPFIRE_SMOKE, false);
    }

    public static boolean auroraEnabled() {
        return atmosphereEnabled() && configured(AURORA, true);
    }

    public static boolean rainbowEnabled() {
        return atmosphereEnabled() && configured(RAINBOW, true);
    }

    public static boolean underwaterHazeEnabled() {
        return atmosphereEnabled() && configured(UNDERWATER_HAZE, true);
    }

    public static boolean heatShimmerEnabled() {
        return atmosphereEnabled() && configured(HEAT_SHIMMER, true);
    }

    public static boolean distantLightningEnabled() {
        return atmosphereEnabled() && configured(DISTANT_LIGHTNING, true);
    }

    private static boolean configured(ForgeConfigSpec.BooleanValue setting, boolean defaultValue) {
        return SPEC.isLoaded() ? setting.get() : defaultValue;
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
        boolean enabled = atmosphereEnabled();
        boolean wind = configured(WIND, true);
        boolean fog = configured(FOG, true);
        boolean skyColors = configured(SKY_COLORS, true);
        boolean riverMist = configured(RIVER_MIST, true);
        boolean snowGust = configured(SNOW_GUST, true);
        boolean caveAir = configured(CAVE_AIR, true);
        boolean canopyLeaves = configured(CANOPY_LEAVES, true);
        boolean campfireSmoke = configured(CAMPFIRE_SMOKE, true);
        boolean darkSmoke = configured(DARK_CAMPFIRE_SMOKE, false);
        boolean aurora = configured(AURORA, true);
        boolean rainbow = configured(RAINBOW, true);
        boolean underwaterHaze = configured(UNDERWATER_HAZE, true);
        boolean heatShimmer = configured(HEAT_SHIMMER, true);
        boolean distantLightning = configured(DISTANT_LIGHTNING, true);
        String windColors = SPEC.isLoaded() ? String.join(", ", WIND_COLORS.get()) : "";

        void setEnabled(boolean value) {
            enabled = value;
            if (SPEC.isLoaded()) {
                ENABLED.set(value);
            }
        }

        void save() {
            if (!SPEC.isLoaded()) {
                return;
            }
            WIND.set(wind);
            FOG.set(fog);
            SKY_COLORS.set(skyColors);
            RIVER_MIST.set(riverMist);
            SNOW_GUST.set(snowGust);
            CAVE_AIR.set(caveAir);
            CANOPY_LEAVES.set(canopyLeaves);
            CAMPFIRE_SMOKE.set(campfireSmoke);
            DARK_CAMPFIRE_SMOKE.set(darkSmoke);
            AURORA.set(aurora);
            RAINBOW.set(rainbow);
            UNDERWATER_HAZE.set(underwaterHaze);
            HEAT_SHIMMER.set(heatShimmer);
            DISTANT_LIGHTNING.set(distantLightning);
            var entries = parseWindColors(windColors);
            if (entries != null) {
                WIND_COLORS.set(entries);
            }
            SPEC.save();
        }
    }
}
