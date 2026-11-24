package com.siirio.jematmosphere;

import net.minecraft.core.Holder;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

public final class AtmospherePalette {
    private static final Map<Holder<Biome>, Profile> PROFILES = new WeakHashMap<>();
    private static final Map<Holder<Biome>, Vec3> CLEAR_SKIES = new WeakHashMap<>();
    private static final TagKey<Biome> COMMON_CAVES = TagKey.create(Registries.BIOME, new ResourceLocation("c", "caves"));
    private static final Vec3 RAIN_SKY = Vec3.fromRGB24(0x758B9E);
    private static final Vec3 THUNDER_SKY = Vec3.fromRGB24(0x424865);

    private static final Profile CHERRY = new Profile(0xB687B1, 0xE5A6AE, 0.10F, 14, 112, 0.79F);
    private static final Profile DESERT = new Profile(0xF7CC85, 0xE5BD79, 0.08F, 20, 140, 0.84F);
    private static final Profile RED_DESERT = new Profile(0xED873C, 0xE99B58, 0.06F, 16, 132, 0.83F);
    private static final Profile DEEP_DARK = new Profile(0, 0x194C57, 0, 6, 52, 0.87F);
    private static final Profile DRIPSTONE = new Profile(0, 0x765030, 0, 8, 70, 0.84F);
    private static final Profile LUSH_CAVE = new Profile(0, 0x589264, 0, 7, 66, 0.81F);
    private static final Profile CAVE = new Profile(0, 0x666C76, 0, 8, 72, 0.77F);
    private static final Profile SNOW = new Profile(0xA5C3E5, 0xC7DFED, 0.30F, 14, 118, 0.78F);
    private static final Profile SWAMP = new Profile(0x92AD92, 0x91AA7E, 0.18F, 10, 85, 0.80F);
    private static final Profile JUNGLE = new Profile(0x91C4AD, 0x9DBEA0, 0.22F, 12, 97, 0.76F);
    private static final Profile TAIGA = new Profile(0x91B9CB, 0xA2BDB7, 0.35F, 16, 118, 0.70F);
    private static final Profile FOREST = new Profile(0x91BDD1, 0xA8C5B7, 0.34F, 17, 128, 0.70F);
    private static final Profile DARK_FOREST = new Profile(0x7E9EA7, 0x81968D, 0.28F, 11, 94, 0.79F);
    private static final Profile COAST = new Profile(0x87C2E1, 0xAECFDB, 0.40F, 21, 154, 0.68F);
    private static final Profile OCEAN = new Profile(0x79AED7, 0x9DBCD5, 0.37F, 24, 172, 0.69F);
    private static final Profile MOUNTAIN = new Profile(0x90B9DF, 0xBCD0E0, 0.42F, 24, 155, 0.70F);
    private static final Profile SAVANNA = new Profile(0xD6C19D, 0xD6C39D, 0.22F, 19, 139, 0.72F);
    private static final Profile MUSHROOM = new Profile(0xAC98C4, 0xBEA7C1, 0.22F, 13, 106, 0.77F);
    private static final Profile PLAINS = new Profile(0x91BCE0, 0xB8CDDC, 0.48F, 21, 145, 0.67F);

    private AtmospherePalette() {
    }

    public static Vec3 clearSky(Holder<Biome> biome) {
        return CLEAR_SKIES.computeIfAbsent(biome, AtmospherePalette::resolveClearSky);
    }

    private static Vec3 resolveClearSky(Holder<Biome> biome) {
        Biome nativeBiome = biome.value();
        Vec3 nativeSky = Vec3.fromRGB24(nativeBiome.getSkyColor());
        Profile profile = profile(biome);
        if (profile.sky() == 0) {
            return nativeSky;
        }
        return Vec3.fromRGB24(profile.sky()).lerp(nativeSky, profile.nativeMix())
                .lerp(Vec3.fromRGB24(nativeBiome.getFoliageColor()), profile.nativeMix() * 0.15);
    }

    public static Vec3 weatherSky(Vec3 nativeSky, float rain, float thunder, float daylight) {
        float light = 0.26F + 0.74F * Mth.clamp(daylight, 0F, 1F);
        return nativeSky.lerp(RAIN_SKY.scale(light), rain * 0.30F)
                .lerp(THUNDER_SKY.scale(light), thunder * 0.66F);
    }

    static Profile profile(Holder<Biome> biome) {
        return PROFILES.computeIfAbsent(biome, AtmospherePalette::resolve);
    }

    static void clear() {
        PROFILES.clear();
        CLEAR_SKIES.clear();
    }

    private static Profile resolve(Holder<Biome> biome) {
        if (biome.is(Biomes.DEEP_DARK)) return DEEP_DARK;
        if (biome.is(Biomes.DRIPSTONE_CAVES)) return DRIPSTONE;
        if (biome.is(Biomes.LUSH_CAVES)) return LUSH_CAVE;
        if (biome.is(BiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_END)
                || biome.unwrapKey().filter(key -> key.location().getNamespace().equals("alexscaves")).isPresent()) {
            return new Profile(0, biome.value().getFogColor(), 0, 13, 100, 0.62F);
        }
        if (biome.is(Tags.Biomes.IS_CAVE) || biome.is(Tags.Biomes.IS_UNDERGROUND) || biome.is(COMMON_CAVES)) {
            return caveProfile(biome);
        }
        if (biome.is(Biomes.CHERRY_GROVE)) return CHERRY;
        if (AtmosphereWeather.hasRedSand(biome)) return RED_DESERT;
        if (AtmosphereWeather.sandColor(biome) != 0) return DESERT;
        if (biome.is(Tags.Biomes.IS_SNOWY) || biome.value().getBaseTemperature() < 0.15F) return SNOW;
        if (biome.is(Tags.Biomes.IS_SWAMP)) return SWAMP;
        if (biome.is(BiomeTags.IS_JUNGLE)) return JUNGLE;
        if (biome.is(Tags.Biomes.IS_MUSHROOM)) return MUSHROOM;
        if (biome.is(BiomeTags.IS_TAIGA)) return TAIGA;
        if (biome.is(Biomes.DARK_FOREST) || biome.is(Tags.Biomes.IS_SPOOKY)) return DARK_FOREST;
        if (biome.is(BiomeTags.IS_FOREST)) return FOREST;
        if (biome.is(BiomeTags.IS_OCEAN)) return OCEAN;
        if (biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)) return COAST;
        if (biome.is(Tags.Biomes.IS_MOUNTAIN) || biome.is(BiomeTags.IS_MOUNTAIN)) return MOUNTAIN;
        if (biome.is(BiomeTags.IS_SAVANNA) || biome.is(Tags.Biomes.IS_DRY) || !biome.value().hasPrecipitation()) return SAVANNA;
        return PLAINS;
    }

    private static Profile caveProfile(Holder<Biome> biome) {
        String path = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
        int color = switch (path) {
            case "cave/andesite_caves" -> 0x69737A;
            case "cave/crystal_caves" -> 0x62628F;
            case "cave/deep_caves" -> 0x39434D;
            case "cave/desert_caves" -> 0x977451;
            case "cave/diorite_caves" -> 0x838890;
            case "cave/frostfire_caves" -> 0x5D728B;
            case "cave/fungal_caves" -> 0x7C6682;
            case "cave/granite_caves" -> 0x88645F;
            case "cave/ice_caves" -> 0x729EB6;
            case "cave/infested_caves" -> 0x6B7551;
            case "cave/mantle_caves" -> 0x844A41;
            case "cave/thermal_caves" -> 0x8A715E;
            case "cave/tuff_caves" -> 0x676F63;
            case "cave/underground_jungle" -> 0x508766;
            default -> CAVE.fog();
        };
        return new Profile(0, color, 0.16F, CAVE.start(), CAVE.end(), CAVE.opacity());
    }

    static Vec3 fogColor(Holder<Biome> biome, Profile profile) {
        Biome nativeBiome = biome.value();
        var color = Vec3.fromRGB24(profile.fog())
                .lerp(Vec3.fromRGB24(nativeBiome.getFogColor()), profile.nativeMix() * 0.42);
        return color.lerp(Vec3.fromRGB24(nativeBiome.getFoliageColor()), profile.nativeMix() * 0.14);
    }

    static Profile ordinaryCave() {
        return CAVE;
    }

    record Profile(int sky, int fog, float nativeMix, float start, float end, float opacity) {}
}
