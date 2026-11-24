package com.siirio.jematmosphere;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;

final class BiomeFog {
    private static ClientLevel previousLevel;
    private static Medium previous;
    private static Medium frame;
    private static long previousFrame;

    private BiomeFog() {
    }

    static void prepare(ViewportEvent.ComputeFogColor event) {
        frame = null;
        var level = Minecraft.getInstance().level;
        var camera = event.getCamera();
        if (level == null || !AtmosphereRenderer.canCompositeFog() || !AtmosphereSettings.fogEnabled()
                || AtmosphereEnvironment.isShaderPackActive()
                || camera.getFluidInCamera() != FogType.NONE || hasRestrictedVision(camera)) {
            return;
        }

        var position = camera.getBlockPosition();
        var biome = level.getBiome(position);
        var profile = AtmospherePalette.profile(biome);
        var original = new Vec3(event.getRed(), event.getGreen(), event.getBlue());
        var color = AtmospherePalette.fogColor(biome, profile);
        float start = profile.start();
        float end = profile.end();
        float opacity = profile.opacity();
        boolean preservedCave = biome.is(Biomes.DEEP_DARK) || biome.is(Biomes.DRIPSTONE_CAVES);
        boolean openDimension = level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
        float enclosure = 0F;
        float partialTick = (float) event.getPartialTick();
        if (preservedCave) {
            color = original.lerp(color, 0.68);
        } else if (profile.sky() == 0 || !openDimension) {
            color = original.lerp(color, 0.69);
        } else {
            enclosure = enclosure(level, position);
            float daylight = Mth.clamp((level.getSkyDarken(partialTick) - 0.2F) / 0.8F, 0F, 1F);
            color = color.scale(0.32 + 0.68 * daylight);
            color = AtmospherePalette.weatherSky(color, level.getRainLevel(partialTick), level.getThunderLevel(partialTick), daylight);
            var cave = AtmospherePalette.ordinaryCave();
            color = color.lerp(AtmospherePalette.fogColor(biome, cave), enclosure * 0.78F);
            start = Mth.lerp(enclosure, start, cave.start());
            end = Mth.lerp(enclosure, end, cave.end());
            opacity = Mth.lerp(enclosure, opacity, cave.opacity());
        }
        frame = blend(level, new Medium(start, end, opacity, color));
        // Retain the native horizon color for the two approved caves and special dimensions.
        if (!preservedCave && profile.sky() != 0 && openDimension) {
            Vec3 sky = level.getSkyColor(camera.getPosition(), partialTick);
            Vec3 horizon = sky.lerp(original.lerp(frame.color(), 0.65), enclosure);
            event.setRed((float) horizon.x);
            event.setGreen((float) horizon.y);
            event.setBlue((float) horizon.z);
        }
    }

    private static float enclosure(ClientLevel level, BlockPos position) {
        int skyLight = level.getBrightness(LightLayer.SKY, position);
        if (skyLight >= 12 || !level.hasChunkAt(position)) {
            return 0;
        }
        int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, position.getX(), position.getZ());
        return Mth.clamp((floor - position.getY() - 4F) / 10F, 0F, 1F) * (12F - skyLight) / 12F;
    }

    private static Medium blend(ClientLevel level, Medium target) {
        long now = System.nanoTime();
        if (previous == null || previousLevel != level || now - previousFrame > 1_000_000_000L) {
            previous = target;
        } else {
            double elapsed = Math.min((now - previousFrame) * 1e-9, 0.1);
            float blend = (float) (1 - Math.exp(-4 * elapsed));
            previous = new Medium(Mth.lerp(blend, previous.start(), target.start()),
                    Mth.lerp(blend, previous.end(), target.end()), Mth.lerp(blend, previous.opacity(), target.opacity()),
                    previous.color().lerp(target.color(), blend));
        }
        previousFrame = now;
        previousLevel = level;
        return previous;
    }

    static Medium takeFrame() {
        Medium current = frame;
        frame = null;
        return current;
    }

    static void keepTerrainEdge(ViewportEvent.RenderFog event) {
        if (frame == null || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getType() != FogType.NONE) {
            return;
        }
        var level = Minecraft.getInstance().level;
        var position = event.getCamera().getBlockPosition();
        if (level == null || level.effects().isFoggyAt(position.getX(), position.getZ())
                || Minecraft.getInstance().gui.getBossOverlay().shouldCreateWorldFog()) {
            return;
        }
        float limit = Minecraft.getInstance().gameRenderer.getRenderDistance();
        event.setNearPlaneDistance(Math.max(event.getNearPlaneDistance(), limit * 0.88F));
        event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance(), limit));
        event.setCanceled(true);
    }

    static boolean hasRestrictedVision(Camera camera) {
        return camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS));
    }

    static void clear() {
        frame = null;
        previousLevel = null;
        previous = null;
        previousFrame = 0;
    }

    record Medium(float start, float end, float opacity, Vec3 color) {}
}
