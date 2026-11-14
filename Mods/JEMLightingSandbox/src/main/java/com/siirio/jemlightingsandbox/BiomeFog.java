package com.siirio.jemlightingsandbox;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

final class BiomeFog {
    private static ClientLevel previousLevel;
    private static Profile previous;
    private static long previousFrame;
    private static ClientLevel windLevel;
    private static Vec3 drift = Vec3.ZERO;
    private static double lastWindTick;

    private BiomeFog() {
    }

    static Medium sample(ClientLevel level, Vec3 camera, float partialTick, float weather, int densityPercent) {
        BlockPos position = BlockPos.containing(camera);
        Holder<Biome> biome = level.getBiome(position);
        int roof = level.hasChunkAt(position)
                ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, position.getX(), position.getZ())
                : level.getSeaLevel();
        float altitude = (float) (camera.y - roof);
        Profile target = profile(level, position, biome, altitude);
        long now = System.nanoTime();
        if (previousLevel != level || previous == null) {
            previousLevel = level;
            previous = target;
        } else {
            float elapsed = Mth.clamp((now - previousFrame) * 1e-9F, 0F, 0.10F);
            float blend = 1F - (float) Math.exp(-elapsed * 5F);
            previous = new Profile(previous.color().lerp(target.color(), blend),
                    Mth.lerp(blend, previous.extinction(), target.extinction()),
                    Mth.lerp(blend, previous.enclosure(), target.enclosure()));
        }
        previousFrame = now;

        float daylight = Mth.clamp((level.getSkyDarken(partialTick) - 0.2F) / 0.8F, 0F, 1F);
        float illumination = Mth.lerp(previous.enclosure(), 0.67F + 0.33F * daylight, 0.86F);
        float density = densityPercent / 100F;
        // The upper half of the density control compresses visibility to a few blocks beyond the start.
        float extinction = previous.extinction() * density * (1F + 5F * Math.max(0F, density - 1F))
                * (1F + weather * 0.55F * (1F - previous.enclosure()));
        return new Medium(previous.color().scale(illumination), extinction, altitude, previous.enclosure());
    }

    static void clear() {
        previousLevel = null;
        previous = null;
        windLevel = null;
        drift = Vec3.ZERO;
    }

    static Vec3 windOffset(ClientLevel level, double tick, Vec3 wind) {
        if (windLevel != level) {
            windLevel = level;
            drift = Vec3.ZERO;
            lastWindTick = tick;
        }
        double elapsed = Mth.clamp(tick - lastWindTick, 0.0, 2.0);
        lastWindTick = tick;
        drift = drift.add(wind.scale(elapsed * 0.18));
        return drift;
    }

    private static Profile profile(ClientLevel level, BlockPos position, Holder<Biome> biome, float altitude) {
        Vec3 nativeColor = Vec3.fromRGB24(biome.value().getFogColor());
        if (biome.unwrapKey().map(key -> key.location().getNamespace().equals("alexscaves")).orElse(false)) {
            return new Profile(nativeColor, 0.036F, 1F);
        }
        if (biome.is(Biomes.LUSH_CAVES)) {
            return new Profile(Vec3.fromRGB24(0x667860), 0.037F, 1F);
        }

        float skyLight = level.getBrightness(LightLayer.SKY, position);
        float enclosure = biome.is(Tags.Biomes.IS_CAVE) || biome.is(Biomes.DEEP_DARK) || biome.is(Biomes.DRIPSTONE_CAVES)
                ? 1F : Mth.clamp((-altitude - 5F) / 14F, 0F, 1F) * Mth.clamp((12F - skyLight) / 8F, 0F, 1F);
        int sand = PrecipitationRenderer.sandColor(biome);
        Vec3 surface;
        float extinction;
        if (sand != 0) {
            surface = Vec3.fromRGB24(sand);
            extinction = 0.023F;
        } else if (biome.is(Tags.Biomes.IS_SNOWY) || biome.value().getPrecipitationAt(position) == Biome.Precipitation.SNOW) {
            surface = Vec3.fromRGB24(0xE2EBF3);
            extinction = 0.027F;
        } else if (biome.is(Biomes.CHERRY_GROVE)) {
            surface = Vec3.fromRGB24(0xE9B9CD);
            extinction = 0.022F;
        } else if (biome.is(Tags.Biomes.IS_SWAMP)) {
            surface = Vec3.fromRGB24(0x7CAF94);
            extinction = 0.033F;
        } else if (biome.is(BiomeTags.IS_JUNGLE)) {
            surface = Vec3.fromRGB24(0x9FBEAC);
            extinction = 0.027F;
        } else {
            surface = nativeColor.lerp(Vec3.fromRGB24(0xCBD4DE), 0.70);
            extinction = 0.018F;
        }

        return new Profile(surface.lerp(Vec3.fromRGB24(0x666974), enclosure),
                Mth.lerp(enclosure, extinction, 0.033F), enclosure);
    }

    record Medium(Vec3 color, float extinction, float altitude, float enclosure) {}

    private record Profile(Vec3 color, float extinction, float enclosure) {}
}
