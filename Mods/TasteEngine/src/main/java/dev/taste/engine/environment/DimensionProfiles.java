package dev.taste.engine.environment;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.taste.engine.render.PipelineParameters;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Resolves local profiles, biome fog and optional server cosmetics into one frame atmosphere. */
public final class DimensionProfiles {
    public record Profile(boolean sky, float red, float green, float blue, float fogDensity,
            float cloudBase, float cloudTop, float starStrength) {
        public Profile {
            finite(red, 0, 1);
            finite(green, 0, 1);
            finite(blue, 0, 1);
            finite(fogDensity, 0, 0.2F);
            finite(cloudBase, -2048, 2048);
            finite(cloudTop, cloudBase, 2048);
            finite(starStrength, 0, 4);
        }

        private static void finite(float value, float low, float high) {
            if (!Float.isFinite(value) || value < low || value > high)
                throw new IllegalArgumentException("Invalid dimension profile");
        }
    }

    public record Atmosphere(float fogDensity, Vector3f fogColor, boolean allowOwnedSky, boolean endStars) { }

    private record Fog(float density, Vector3f color) { }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROFILE_PATH = "taste/dimensions";
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final String END = "minecraft:the_end";
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private Map<String, Profile> resourceProfiles = Map.of();
    private final Profile fallback = new Profile(false, 0.08F, 0.08F, 0.10F, 0.02F, 128.0F, 192.0F, 0.4F);
    private final EnvironmentZoneResolver zones = new EnvironmentZoneResolver();
    private final SeasonalAppearance seasons = new SeasonalAppearance();
    private ClientLevel lastLevel;
    private ResourceLocation lastDimension;
    private Fog lastFog;
    private Fog from;
    private long transitionStart;
    private ClientLevel decorationLevel;
    private long decorationTick = Long.MIN_VALUE;

    public DimensionProfiles() {
        profiles.put(OVERWORLD, new Profile(true, 0.48F, 0.68F, 1.0F, 0.006F, 112.0F, 196.0F, 1.0F));
        profiles.put(NETHER, new Profile(false, 0.45F, 0.08F, 0.03F, 0.035F, 0.0F, 0.0F, 0.0F));
        profiles.put(END, new Profile(false, 0.16F, 0.10F, 0.28F, 0.012F, 0.0F, 0.0F, 1.5F));
    }

    public void register(String dimension, Profile profile) {
        if (ResourceLocation.tryParse(dimension) == null || !dimension.contains(":"))
            throw new IllegalArgumentException("Invalid dimension profile ID");
        profiles.put(dimension, Objects.requireNonNull(profile));
    }

    public Profile resolve(String dimension) {
        return resourceProfiles.getOrDefault(dimension, profiles.getOrDefault(dimension, fallback));
    }

    public void load(ResourceManager resources) {
        try {
            var files = resources.listResources(PROFILE_PATH, id -> id.getPath().endsWith(".json"));
            if (files.size() > 64) throw new IllegalArgumentException("Too many dimension profiles");
            Map<String, Profile> next = new LinkedHashMap<>();
            for (var entry : files.entrySet()) {
                String file = entry.getKey().getPath();
                String dimension = new ResourceLocation(entry.getKey().getNamespace(),
                        file.substring(PROFILE_PATH.length() + 1, file.length() - 5)).toString();
                try (var input = entry.getValue().open()) {
                    byte[] bytes = input.readNBytes(8193);
                    if (bytes.length > 8192) throw new IllegalArgumentException("Dimension profile is too large: " + dimension);
                    var value = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                    String rawColor = value.get("fogColor").getAsString();
                    if (!rawColor.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Invalid fog color in " + dimension);
                    Vector3f color = linear(Integer.parseInt(rawColor.substring(1), 16));
                    Profile base = profiles.getOrDefault(dimension, fallback);
                    next.put(dimension, new Profile(value.has("sky") ? value.get("sky").getAsBoolean() : base.sky(),
                            color.x, color.y, color.z, value.get("fogDensity").getAsFloat(),
                            base.cloudBase(), base.cloudTop(), base.starStrength()));
                }
            }
            resourceProfiles = Map.copyOf(next);
        } catch (IOException | RuntimeException failure) {
            LOGGER.warn("Taste retained the previous dimension profiles. {}", failure.getMessage());
        }
    }

    public Atmosphere resolve(ClientLevel level, Vec3 position, PipelineParameters settings) {
        ResourceLocation key = level.dimension().location();
        String id = key.toString();
        boolean useProfiles = enabled(settings, "dimensions.useProfiles");
        boolean overworld = id.equals(OVERWORLD);
        boolean nether = id.equals(NETHER);
        boolean end = id.equals(END);
        Profile packProfile = useProfiles ? resourceProfiles.get(id) : null;
        Profile profile = packProfile != null ? packProfile : useProfiles ? profiles.get(id) : null;
        boolean vanillaOverworldSky = overworld && level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL
                && level.effects().getClass().getEnclosingClass() == DimensionSpecialEffects.class;
        boolean customSky = !overworld && !nether && !end && profile != null && profile.sky()
                && settings.value("dimensions.unknownSky").getAsString().equals("profile");
        Vector3f fogColor = linear(Integer.parseInt(settings.value("fog.color").getAsString().substring(1), 16));
        float density = number(settings, "fog.density");
        if (profile != null) {
            if (packProfile != null) density = packProfile.fogDensity();
            else if (nether) density = number(settings, "dimensions.netherFog");
            else if (end) density = number(settings, "dimensions.endHaze");
            else if (!overworld) density = profile.fogDensity();
            if (packProfile != null) fogColor.set(packProfile.red(), packProfile.green(), packProfile.blue());
            else if (!overworld) fogColor.lerp(new Vector3f(profile.red(), profile.green(), profile.blue()), 0.45F);
        }
        if (useProfiles && (overworld || nether || end || profile != null)) {
            int radius = settings.value("dimensions.biomeBlendRadius").getAsInt();
            Vector3f biome = biomeFog(level, position, radius);
            if (biome != null) fogColor.lerp(biome, 0.25F);
        }
        if (useProfiles && overworld)
            seasons.tint(level, BlockPos.containing(position.x, position.y, position.z),
                    fogColor, number(settings, "dimensions.seasonBlend"));

        long now = System.nanoTime();
        if (level != lastLevel) {
            from = lastDimension != null && !lastDimension.equals(key) ? lastFog : null;
            transitionStart = now;
            lastDimension = key;
            lastLevel = level;
            zones.clear();
        }
        float duration = number(settings, "general.dimensionBlendSeconds");
        if (from != null && duration > 0) {
            float amount = Math.min(1, (now - transitionStart) * 1.0e-9F / duration);
            amount = amount * amount * (3 - 2 * amount);
            density = from.density() + (density - from.density()) * amount;
            fogColor = new Vector3f(from.color()).lerp(fogColor, amount);
            if (amount >= 1) from = null;
        } else from = null;
        lastFog = new Fog(density, new Vector3f(fogColor));
        var tinted = zones.resolve(key, position, level.getGameTime(), settings, density, fogColor);
        return new Atmosphere(tinted.density(), tinted.color(), vanillaOverworldSky || customSky,
                useProfiles && end && enabled(settings, "dimensions.endStars"));
    }

    private static Vector3f biomeFog(ClientLevel level, Vec3 camera, int radius) {
        BlockPos center = BlockPos.containing(camera.x, camera.y, camera.z);
        if (!level.hasChunkAt(center)) return null;
        Vector3f color = linear(level.getBiome(center).value().getFogColor()).mul(4);
        float weight = 4;
        if (radius > 0) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos sample = center.offset(x * radius, 0, z * radius);
                    if (!level.hasChunkAt(sample)) continue;
                    float contribution = x == 0 || z == 0 ? 2 : 1;
                    color.fma(contribution, linear(level.getBiome(sample).value().getFogColor()));
                    weight += contribution;
                }
            }
        }
        return color.div(weight);
    }

    public void decorate(ClientLevel level, Vec3 camera, PipelineParameters settings) {
        if (!level.dimension().equals(Level.NETHER) || !enabled(settings, "dimensions.useProfiles")
                || !enabled(settings, "dimensions.netherEmbers") || !enabled(settings, "particles.enabled")
                || enabled(settings, "accessibility.reducedMotion")) return;
        long tick = level.getGameTime();
        if (tick % 4 != 0 || level == decorationLevel && tick == decorationTick) return;
        decorationLevel = level;
        decorationTick = tick;
        BlockPos origin = BlockPos.containing(camera.x, camera.y, camera.z);
        Random random = new Random(settings.value("general.seed").getAsLong() ^ tick * 0x9E3779B97F4A7C15L ^ origin.asLong());
        if (random.nextFloat() > 0.55F) return;
        BlockPos point = origin.offset(random.nextInt(15) - 7, random.nextInt(6) - 2, random.nextInt(15) - 7);
        if (!level.hasChunkAt(point) || !level.isEmptyBlock(point)) return;
        var biome = level.getBiome(point);
        if (!biome.is(BiomeTags.IS_NETHER)) return;
        var particle = biome.is(Biomes.SOUL_SAND_VALLEY) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME;
        level.addParticle(particle, point.getX() + random.nextDouble(), point.getY() + random.nextDouble(),
                point.getZ() + random.nextDouble(), 0, 0.014 + random.nextDouble() * 0.012, 0);
    }

    private static Vector3f linear(int color) {
        return new Vector3f(linearChannel(color >> 16), linearChannel(color >> 8), linearChannel(color));
    }

    private static float linearChannel(int channel) {
        float display = (channel & 255) / 255.0F;
        return display <= 0.04045F ? display / 12.92F : (float) Math.pow((display + 0.055F) / 1.055F, 2.4);
    }

    private static boolean enabled(PipelineParameters settings, String id) { return settings.value(id).getAsBoolean(); }
    private static float number(PipelineParameters settings, String id) { return settings.value(id).getAsFloat(); }

    public void clear() {
        lastLevel = null;
        lastDimension = null;
        lastFog = null;
        from = null;
        decorationLevel = null;
        decorationTick = Long.MIN_VALUE;
        zones.clear();
    }
}
