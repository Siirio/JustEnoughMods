package com.siirio.jemlightingsandbox;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

final class BiomeFog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Range CLEAR = new Range(24, 112, 0);
    private static final Range CHERRY_GROVE = new Range(14, 112, 0.79F);
    private static final Range DESERT = new Range(20, 140, 0.84F);
    private static final Range DEEP_DARK = new Range(6, 52, 0.87F);
    private static final Range DRIPSTONE_CAVES = new Range(8, 70, 0.84F);

    private static Vec3 deepDarkColor;
    private static Vec3 dripstoneColor;
    private static Vec3 terrainColor;
    private static ClientLevel previousLevel;
    private static Medium previous;
    private static long previousFrame;

    private BiomeFog() {
    }

    static void reload(ResourceManager resources) {
        deepDarkColor = readCaveColor(resources, Biomes.DEEP_DARK);
        dripstoneColor = readCaveColor(resources, Biomes.DRIPSTONE_CAVES);
        clear();
    }

    private static Vec3 readCaveColor(ResourceManager resources, ResourceKey<Biome> biome) {
        var id = biome.location();
        var resource = new ResourceLocation(id.getNamespace(), "fog_definitions/biome/" + id.getPath() + ".json");
        try (var reader = resources.openAsReader(resource)) {
            var definition = GsonHelper.convertToJsonObject(JsonParser.parseReader(reader), "fog definition");
            var color = GsonHelper.getAsString(GsonHelper.getAsJsonObject(definition, "colors"), "night");
            if (!color.matches("#[a-fA-F0-9]{6}")) {
                throw new JsonSyntaxException("Expected a #RRGGBB night fog color");
            }
            return Vec3.fromRGB24(Integer.parseInt(color.substring(1), 16));
        } catch (IOException | JsonParseException exception) {
            LOGGER.warn("Could not read the JEM cave fog color from {}", resource, exception);
            return null;
        }
    }

    static void captureTerrainColor() {
        // The normal Voxy pipeline disables close terrain fog with this pair of distances.
        if (!ModList.get().isLoaded("voxy") || !ModList.get().isLoaded("fog")
                || RenderSystem.getShaderFogStart() != Float.MAX_VALUE
                || RenderSystem.getShaderFogEnd() != Float.MAX_VALUE) {
            terrainColor = null;
            return;
        }
        float[] color = RenderSystem.getShaderFogColor();
        terrainColor = new Vec3(color[0], color[1], color[2]);
    }

    static Vec3 takeTerrainColor() {
        Vec3 color = terrainColor;
        terrainColor = null;
        return color;
    }

    static Medium sample(ClientLevel level, BlockPos position, Vec3 color) {
        var biome = level.getBiome(position);
        Range range = CLEAR;
        Vec3 caveColor = null;
        if (biome.is(Biomes.CHERRY_GROVE)) {
            range = CHERRY_GROVE;
        } else if (biome.is(Biomes.DESERT)) {
            range = DESERT;
        } else if (biome.is(Biomes.DEEP_DARK)) {
            range = DEEP_DARK;
            caveColor = deepDarkColor;
        } else if (biome.is(Biomes.DRIPSTONE_CAVES)) {
            range = DRIPSTONE_CAVES;
            caveColor = dripstoneColor;
        }
        // Keep some biome color where Fog blends the native result toward cave black.
        var target = new Medium(range, caveColor == null ? color : color.lerp(caveColor, 0.68));
        long now = System.nanoTime();
        if (previous == null || previousLevel != level || now - previousFrame > 1_000_000_000L) {
            previous = target;
        } else {
            double elapsed = Math.min((now - previousFrame) * 1e-9, 0.1);
            float blend = (float) (1 - Math.exp(-4 * elapsed));
            Range from = previous.range();
            previous = new Medium(new Range(Mth.lerp(blend, from.start(), range.start()),
                    Mth.lerp(blend, from.end(), range.end()), Mth.lerp(blend, from.opacity(), range.opacity())),
                    previous.color().lerp(target.color(), blend));
        }
        previousFrame = now;
        previousLevel = level;
        return previous.range().opacity() < 0.002F ? null : previous;
    }

    static void clear() {
        terrainColor = null;
        previousLevel = null;
        previous = null;
        previousFrame = 0;
    }

    record Range(float start, float end, float opacity) {}

    record Medium(Range range, Vec3 color) {}
}
