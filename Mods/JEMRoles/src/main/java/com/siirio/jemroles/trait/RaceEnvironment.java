package com.siirio.jemroles.trait;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RaceEnvironment {
    private RaceEnvironment() {}

    public static boolean safe(ServerPlayer player, ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet) || !level.hasChunkAt(feet)) return false;
        if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)) return false;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) return false;
        AABB moved = player.getBoundingBox().move(Vec3.atBottomCenterOf(feet).subtract(player.position()));
        return level.noCollision(player, moved);
    }

    public static Optional<BlockPos> nearestSafe(ServerPlayer player, ServerLevel level, BlockPos origin, int radius, boolean dry) {
        if (radius > 16) {
            for (int r = 1; r <= radius; r++) {
                int probes = Math.max(16, Math.min(48, r * 2));
                for (int i = 0; i < probes; i++) {
                    double angle = Math.PI * 2.0D * i / probes;
                    int x = origin.getX() + (int)Math.round(Math.cos(angle) * r);
                    int z = origin.getZ() + (int)Math.round(Math.sin(angle) * r);
                    for (int y = Math.min(level.getMaxBuildHeight() - 2, origin.getY() + 6);
                         y >= Math.max(level.getMinBuildHeight() + 1, origin.getY() - 12); y--) {
                        BlockPos candidate = new BlockPos(x, y, z);
                        if (safe(player, level, candidate) && (!dry || level.getFluidState(candidate).isEmpty()))
                            return Optional.of(candidate);
                    }
                }
            }
            return Optional.empty();
        }
        for (int r = 0; r <= radius; r++) {
            for (int y = Math.min(level.getMaxBuildHeight() - 2, origin.getY() + 6); y >= Math.max(level.getMinBuildHeight() + 1, origin.getY() - 12); y--) {
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                    if (r > 0 && Math.abs(x) != r && Math.abs(z) != r) continue;
                    BlockPos candidate = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
                    if (safe(player, level, candidate) && (!dry || level.getFluidState(candidate).isEmpty())) return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    /** Enderman-style water escape: probe the real surface height instead of only a narrow band around the pool Y. */
    public static Optional<BlockPos> nearestDrySurface(ServerPlayer player, ServerLevel level, BlockPos origin, int radius) {
        for (int r = 1; r <= radius; r++) {
            int probes = Math.max(16, Math.min(64, r * 4));
            for (int i = 0; i < probes; i++) {
                double angle = Math.PI * 2.0D * i / probes;
                int x = origin.getX() + (int)Math.round(Math.cos(angle) * r);
                int z = origin.getZ() + (int)Math.round(Math.sin(angle) * r);
                BlockPos column = new BlockPos(x, origin.getY(), z);
                if (!level.hasChunkAt(column)) continue;
                BlockPos candidate = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                if (safe(player, level, candidate)) return Optional.of(candidate.immutable());
            }
        }
        return Optional.empty();
    }

    public static boolean green(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        if (biome.is(Biomes.LUSH_CAVES) || biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE)) return true;
        var climate = biome.value().getModifiedClimateSettings();
        return climate.downfall() >= 0.7F && climate.temperature() > 0.15F && climate.temperature() < 1.55F;
    }

    public static boolean naturalDaylight(ServerLevel level, BlockPos pos) {
        return level.dimensionType().hasSkyLight() && level.isDay() && level.canSeeSky(pos.above()) && level.getMaxLocalRawBrightness(pos.above()) >= 12;
    }

    public static boolean underground(ServerLevel level, BlockPos pos) {
        if (level.canSeeSky(pos.above())) return false;
        int solid = 0;
        for (int y = 2; y <= 12; y++) {
            var state = level.getBlockState(pos.above(y));
            if (!state.isAir() && state.getFluidState().isEmpty() && state.canOcclude()) solid++;
        }
        return solid >= 6;
    }
}
