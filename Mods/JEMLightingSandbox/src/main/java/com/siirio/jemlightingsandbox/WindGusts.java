package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

public final class WindGusts {
    private static final int LIMIT = 24;
    private static final double RANGE = 33.0;
    private static final double INV_ATLAS_WIDTH = 1.0 / 512.0;
    private static final double INV_ATLAS_HEIGHT = 1.0 / 96.0;
    private static final int[] FRAME_START = {
            11, 11, 11, 11, 11, 11, 11, 11, 13, 20, 29, 37,
            5, 6, 7, 9, 10, 13, 16, 18, 21, 23, 24, 27, 33, 39, 46, 50, 56, 59, 60
    };
    private static final int[] FRAME_END = {
            12, 15, 21, 27, 33, 40, 49, 53, 53, 53, 53, 53,
            7, 11, 14, 16, 19, 23, 28, 34, 37, 39, 45, 48, 51, 54, 57, 59, 61, 62, 61
    };
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryParse(JemLightingSandbox.ID + ":textures/weather/wind_gusts.png");
    private static final RandomSource RANDOM = RandomSource.create();
    private static final ArrayList<Gust> GUSTS = new ArrayList<>(LIMIT);
    private static ClientLevel currentLevel;
    private static Vec3 lastCamera;

    private WindGusts() {
    }

    static int defaultTint(Holder<Biome> biome) {
        int sand = PrecipitationRenderer.sandColor(biome);
        if (sand == 0) {
            return 0xFFFFFF;
        }
        return sand;
    }

    static void tick(Minecraft client) {
        var camera = client.gameRenderer.getMainCamera();
        ClientLevel level = client.level;
        ParticleStatus quality = client.options.particles().get();
        if (level == null || !LightingSettings.atmosphereEnabled() || !SandboxWeather.hasOutdoorWeather(level)
                || TerrainLighting.isShaderPackActive() || quality == ParticleStatus.MINIMAL
                || !camera.isInitialized() || camera.getFluidInCamera() != FogType.NONE) {
            clear();
            return;
        }
        if (client.isPaused()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        if (level != currentLevel || lastCamera == null || lastCamera.distanceToSqr(eye) > RANGE * RANGE) {
            clear();
            currentLevel = level;
        }
        lastCamera = eye;
        long time = level.getGameTime();
        GUSTS.removeIf(gust -> time - gust.start >= gust.type.lifetime
                || gust.position(time - gust.start).distanceToSqr(eye) > RANGE * RANGE);
        Vec3 wind = SandboxWeather.wind(level);
        boolean storm = level.getThunderLevel(1F) >= 0.4F;
        boolean strong = storm || wind.lengthSqr() >= 0.30;
        int interval = strong ? 5 : 22;
        if (quality == ParticleStatus.DECREASED) {
            interval *= 2;
        }
        if (time % interval != 0 || GUSTS.size() >= LIMIT) {
            return;
        }
        spawn(level, eye, wind, strong, time);
    }

    static void clear() {
        currentLevel = null;
        lastCamera = null;
        GUSTS.clear();
    }

    private static void spawn(ClientLevel level, Vec3 eye, Vec3 wind, boolean strong, long time) {
        var cursor = new BlockPos.MutableBlockPos();
        Vec3 forward = wind.normalize();
        for (int attempt = 0; attempt < 6; attempt++) {
            double x = eye.x + RANDOM.nextDouble() * 34 - 17;
            double z = eye.z + RANDOM.nextDouble() * 34 - 17;
            if (!level.hasChunkAt(cursor.set(x, eye.y, z))) {
                continue;
            }
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cursor.getX(), cursor.getZ());
            Type type = !strong ? Type.CALM : RANDOM.nextBoolean() ? Type.GROUND : Type.SLANT;
            double y = switch (type) {
                case CALM -> surface + 0.65 + RANDOM.nextDouble() * 1.6;
                case GROUND -> surface + 0.09;
                case SLANT -> surface + 1.0 + RANDOM.nextDouble() * 2.2;
            };
            if (Math.abs(y - eye.y) > 9 || !isOpen(level, cursor.set(x, y, z))) {
                continue;
            }
            double width = switch (type) {
                case CALM -> 1.5;
                case GROUND -> RANDOM.nextInt(16) == 0 ? 9 : 3 + RANDOM.nextDouble();
                case SLANT -> 5 + RANDOM.nextDouble();
            };
            GUSTS.add(new Gust(type, time, new Vec3(x, y, z), forward, width));
            return;
        }
    }

    public static void draw(Minecraft client, Vec3 camera, float partialTick) {
        ClientLevel level = client.level;
        if (level == null || level != currentLevel || GUSTS.isEmpty()
                || client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        long time = level.getGameTime();
        var cursor = new BlockPos.MutableBlockPos();
        for (Gust gust : GUSTS) {
            gust.draw(buffer, level, cursor, camera, time - gust.start + partialTick);
        }
        buffer.setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN);
        var vertices = buffer.endOrDiscardIfEmpty();
        if (vertices != null) {
            PrecipitationRenderer.drawBatch(vertices, GameRenderer.getPositionTexColorShader(), TEXTURE);
        }
    }

    private static boolean isOpen(ClientLevel level, BlockPos position) {
        return SandboxWeather.outdoors(level, position) && level.isEmptyBlock(position);
    }

    private enum Type {
        CALM(32, 0.038, 0.48F),
        GROUND(21, 0.34, 0.65F),
        SLANT(40, 0.18, 0.61F);

        final int lifetime;
        final double speed;
        final float opacity;

        Type(int lifetime, double speed, float opacity) {
            this.lifetime = lifetime;
            this.speed = speed;
            this.opacity = opacity;
        }
    }

    private record Gust(Type type, long start, Vec3 origin, Vec3 forward, double length) {
        Vec3 position(double age) {
            Vec3 advance = origin.add(forward.scale(age * type.speed));
            return type == Type.GROUND ? advance : advance.add(0, age * 0.018, 0);
        }

        void draw(BufferBuilder buffer, ClientLevel level, BlockPos.MutableBlockPos cursor, Vec3 eye, double age) {
            double progress = Mth.clamp(age / type.lifetime, 0.0, 0.99999);
            Vec3 center = position(age);
            if (!level.hasChunkAt(cursor.set(center.x, center.y, center.z))) {
                return;
            }
            int tint = 0xFFFFFF;
            if (type != Type.CALM) {
                Holder<Biome> biome = level.getBiome(cursor);
                var biomeId = biome.unwrapKey().map(key -> key.location()).orElse(null);
                int override = LightingSettings.windColor(biomeId);
                tint = override >= 0 ? override : defaultTint(biome);
            }
            float alpha = type.opacity * (float) (Mth.clamp(progress * 8, 0, 1) * Mth.clamp((1 - progress) * 5, 0, 1)
                    * Mth.clamp((RANGE - center.distanceTo(eye)) / 7, 0, 1));
            if (alpha < 0.012F) {
                return;
            }
            int frame = type == Type.CALM ? (int) (progress * 12) : 12 + (int) (progress * 19);
            double u = (frame % 8) * 64;
            double v = (frame / 8) * 24;
            // Preserve travel within the sprite and emit only its occupied horizontal span.
            double firstPixel = FRAME_START[frame];
            double lastPixel = FRAME_END[frame];
            double blockPerPixel = length / (type == Type.CALM ? 42.0 : 21.0);
            double width = (lastPixel - firstPixel) * blockPerPixel;
            double offset = ((firstPixel + lastPixel) * 0.5 - 32) * blockPerPixel;
            int segments = Mth.ceil(width / 0.45);
            Vec3 sideways = new Vec3(-forward.z, 0, forward.x);
            Vec3 axis = type == Type.GROUND ? sideways : forward;
            center = center.add(axis.scale(offset));
            if (type == Type.SLANT) {
                center = center.add(0, offset * 0.22, 0);
            }
            Vec3 height = switch (type) {
                case CALM -> new Vec3(0, 0.83, 0);
                case GROUND -> forward.scale(1.2);
                case SLANT -> new Vec3(0, 1.6, 0);
            };
            for (int i = 0; i < segments; i++) {
                double from = (double) i / segments;
                double to = (double) (i + 1) / segments;
                Vec3 a = point(level, cursor, center, axis, width, from);
                Vec3 b = point(level, cursor, center, axis, width, to);
                if (a == null || b == null || Math.abs(a.y - b.y) > 0.35) {
                    continue;
                }
                Vec3 upperA = a.add(height.scale(0.5));
                Vec3 lowerA = a.subtract(height.scale(0.5));
                Vec3 upperB = b.add(height.scale(0.5));
                Vec3 lowerB = b.subtract(height.scale(0.5));
                if (!isOpen(level, cursor.set(upperA.x, upperA.y, upperA.z))
                        || !isOpen(level, cursor.set(lowerA.x, lowerA.y, lowerA.z))
                        || !isOpen(level, cursor.set(upperB.x, upperB.y, upperB.z))
                        || !isOpen(level, cursor.set(lowerB.x, lowerB.y, lowerB.z))) {
                    continue;
                }
                float left = (float) ((u + Mth.lerp(from, firstPixel, lastPixel)) * INV_ATLAS_WIDTH);
                float right = (float) ((u + Mth.lerp(to, firstPixel, lastPixel)) * INV_ATLAS_WIDTH);
                float top = (float) (v * INV_ATLAS_HEIGHT);
                float bottom = (float) ((v + 24) * INV_ATLAS_HEIGHT);
                vertex(buffer, upperA.subtract(eye), left, top, tint, alpha);
                vertex(buffer, lowerA.subtract(eye), left, bottom, tint, alpha);
                vertex(buffer, lowerB.subtract(eye), right, bottom, tint, alpha);
                vertex(buffer, upperB.subtract(eye), right, top, tint, alpha);
            }
        }

        private Vec3 point(ClientLevel level, BlockPos.MutableBlockPos cursor, Vec3 center, Vec3 axis, double width,
                double position) {
            double offset = (position - 0.5) * width;
            Vec3 point = center.add(axis.scale(offset));
            if (type == Type.SLANT) {
                return point.add(0, offset * 0.22, 0);
            }
            if (type == Type.CALM) {
                return point;
            }
            if (!level.hasChunkAt(cursor.set(point.x, point.y, point.z))) {
                return null;
            }
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cursor.getX(), cursor.getZ());
            return Math.abs(surface - center.y) < 3
                    ? new Vec3(point.x, surface + 0.09, point.z) : null;
        }
    }

    private static void vertex(BufferBuilder buffer, Vec3 point, float u, float v, int tint, float alpha) {
        buffer.vertex(point.x, point.y, point.z).uv(u, v).color(tint >> 16 & 255, tint >> 8 & 255,
                tint & 255, Mth.floor(alpha * 255F)).endVertex();
    }

}
