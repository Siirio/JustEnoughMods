package com.siirio.jemlightingsandbox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.joml.Vector3f;

public final class SandboxWeather {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final DustParticleOptions WIND_DUST = dust(0.76F, 0.76F, 0.68F, 0.35F);
    private static final DustParticleOptions CAVE_DUST = dust(0.56F, 0.53F, 0.47F, 0.42F);
    private static ClientLevel heatLevel;
    private static long nextHeatScan;
    private static float heat;
    private static float targetHeat;
    private static Vec3 heatOrigin;

    private SandboxWeather() {
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        var camera = client.gameRenderer.getMainCamera();
        if (!LightingSettings.atmosphereEnabled() || level == null || !camera.isInitialized()
                || camera.getFluidInCamera() != FogType.NONE) {
            heatLevel = null;
            return;
        }
        if (client.isPaused()) {
            return;
        }
        Vec3 center = camera.getPosition();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        position.set(center.x, center.y, center.z);
        if (!level.hasChunkAt(position)) {
            heatLevel = null;
            return;
        }
        updateHeat(level, center, position);

        ParticleStatus status = client.options.particles().get();
        if (status == ParticleStatus.MINIMAL || !hasOutdoorWeather(level)) {
            return;
        }
        long time = level.getGameTime();
        Vec3 wind = wind(level);

        if (time % (status == ParticleStatus.DECREASED ? 8 : 4) == 0) {
            spawnAirDust(client, level, center, position, wind);
        }
    }

    public static float heat(Minecraft client) {
        return LightingSettings.atmosphereEnabled() && client.level != null && client.level == heatLevel ? heat : 0F;
    }

    /** Null means the warm biome supplies heat across the terrain. */
    public static Vec3 heatOrigin(Minecraft client) {
        return client.level != null && client.level == heatLevel ? heatOrigin : null;
    }

    private static void updateHeat(ClientLevel level, Vec3 center, BlockPos.MutableBlockPos cursor) {
        long time = level.getGameTime();
        if (level != heatLevel) {
            heatLevel = level;
            heat = 0F;
            heatOrigin = null;
            nextHeatScan = time;
        }
        if (time >= nextHeatScan) {
            nextHeatScan = time + 8;
            int centerX = cursor.getX();
            int centerY = cursor.getY();
            int centerZ = cursor.getZ();
            boolean desert = PrecipitationRenderer.sandColor(level.getBiome(cursor)) != 0;
            targetHeat = desert && outdoors(level, cursor) ? 0.30F * (1F - level.getRainLevel(1F)) : 0F;
            float localHeat = 0F;
            Vec3 nearbyHeatOrigin = null;
            for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                for (int x = centerX - 4; x <= centerX + 4; x++) {
                    if (!level.hasChunkAt(cursor.set(x, centerY, z))) {
                        continue;
                    }
                    for (int y = centerY - 3; y <= centerY + 1; y++) {
                        cursor.set(x, y, z);
                        if (level.isOutsideBuildHeight(cursor)) {
                            continue;
                        }
                        var state = level.getBlockState(cursor);
                        boolean lava = state.getFluidState().is(FluidTags.LAVA);
                        boolean fire = state.is(BlockTags.FIRE) || (state.is(BlockTags.CAMPFIRES)
                                && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT));
                        if (!lava && !fire) {
                            continue;
                        }
                        double distance = center.distanceToSqr(x + 0.5, y + 0.5, z + 0.5);
                        float strength = (lava ? 0.82F : 0.58F) * (1F - Mth.clamp((float) distance / 42F, 0F, 1F));
                        if (strength > localHeat) {
                            localHeat = strength;
                            nearbyHeatOrigin = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                        }
                    }
                }
            }
            if (localHeat > targetHeat) {
                targetHeat = localHeat;
                heatOrigin = nearbyHeatOrigin;
            } else if (targetHeat > 0F || heat < 0.001F) {
                heatOrigin = null;
            }
        }
        heat = Mth.lerp(0.24F, heat, targetHeat);
    }

    public static Vec3 campfireWind(ClientLevel level, Vec3 center) {
        if (!LightingSettings.atmosphereEnabled() || !hasOutdoorWeather(level)
                || !outdoors(level, BlockPos.containing(center))) {
            return Vec3.ZERO;
        }
        return wind(level).scale(0.032);
    }

    private static void spawnAirDust(Minecraft client, ClientLevel level, Vec3 center, BlockPos.MutableBlockPos position,
            Vec3 wind) {
        double x = center.x + spread(6);
        double z = center.z + spread(6);
        double y = center.y + spread(2.5);
        position.set(x, y, z);
        if (level.isOutsideBuildHeight(position) || !level.hasChunkAt(position) || !level.isEmptyBlock(position)) {
            return;
        }
        if (outdoors(level, position)) {
            Vec3 drift = wind.scale(0.05);
            if (outdoors(level, position.set(x + drift.x * 10, y, z + drift.z * 10))) {
                emit(client, WIND_DUST, x, y, z, drift, 0.003, 16);
            }
        } else if (isCave(level, position)) {
            emit(client, CAVE_DUST, x, y, z, new Vec3(spread(0.006), 0, spread(0.006)), 0.003, 29);
        }
    }

    private static boolean isCave(ClientLevel level, BlockPos position) {
        int roof = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, position.getX(), position.getZ());
        if (roof - position.getY() < 5) {
            return false;
        }
        if (level.getBiome(position).is(Tags.Biomes.IS_CAVE)) {
            return true;
        }
        return (level.getBlockState(position.below(2)).is(BlockTags.BASE_STONE_OVERWORLD)
                || level.getBlockState(position.below(4)).is(BlockTags.BASE_STONE_OVERWORLD))
                && level.getBlockState(position.above(4)).is(BlockTags.BASE_STONE_OVERWORLD);
    }

    static boolean outdoors(ClientLevel level, BlockPos position) {
        return !level.isOutsideBuildHeight(position) && level.hasChunkAt(position) && level.canSeeSky(position)
                && position.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ());
    }

    static boolean hasOutdoorWeather(ClientLevel level) {
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
    }

    static Vec3 wind(ClientLevel level) {
        double time = level.getGameTime();
        double angle = 0.85 + Math.sin(time * 0.0023) * 0.75;
        double gust = 0.71 + Math.sin(time * 0.021) * 0.20 + Math.sin(time * 0.061) * 0.09;
        double speed = (0.64 + level.getThunderLevel(1.0F) * 0.36) * gust;
        return new Vec3(Math.cos(angle) * speed, 0, Math.sin(angle) * speed);
    }

    private static void emit(Minecraft client, ParticleOptions type, double x, double y, double z, Vec3 drift,
            double vertical, int lifetime) {
        var particle = client.particleEngine.createParticle(type, x, y, z, drift.x, vertical, drift.z);
        if (particle != null) {
            particle.setParticleSpeed(drift.x, vertical, drift.z);
            particle.setLifetime(lifetime);
        }
    }

    private static double spread(double radius) {
        return (RANDOM.nextDouble() * 2 - 1) * radius;
    }

    private static DustParticleOptions dust(float red, float green, float blue, float size) {
        return new DustParticleOptions(new Vector3f(red, green, blue), size);
    }
}
