package com.siirio.jematmosphere;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

final class GroundAtmosphere {
    private static final int SEARCH_RADIUS = 21;
    private static final int SCAN_STEP = 3;
    private static final int SCAN_TICKS = 12;
    static final int RIVER_BANKS = 3;
    private static final int SNOW_PERIOD = 220;
    private final RandomSource random = RandomSource.create();
    private ClientLevel level;
    private long nextScan;
    private long nextGrain;
    private BlockPos scanCenter;
    private List<Vec3> riverBanks = List.of();
    private Vec3 snowBank;
    private Vec3 gustDirection = Vec3.ZERO;
    private Vec3 caveBase;
    private Vec3 lastGustCenter;
    private boolean wasMistEnabled;
    private boolean wasPowderEnabled;
    private boolean wasCaveEnabled;
    private boolean riverPresent;
    private boolean cavePresent;
    private float caveHeight;
    private float riverOpacity;
    private float snowOpacity;
    private float caveOpacity;
    private long lastFrame;

    void clear() {
        level = null;
        nextScan = 0;
        nextGrain = 0;
        scanCenter = null;
        riverBanks = List.of();
        snowBank = null;
        gustDirection = Vec3.ZERO;
        caveBase = null;
        lastGustCenter = null;
        wasMistEnabled = false;
        wasPowderEnabled = false;
        wasCaveEnabled = false;
        riverPresent = false;
        cavePresent = false;
        caveHeight = 0F;
        riverOpacity = 0F;
        snowOpacity = 0F;
        caveOpacity = 0F;
        lastFrame = 0L;
    }

    Frame sample(Minecraft client, ClientLevel current, Vec3 eye, BlockPos block,
                 float partialTick, float weather, boolean openDimension, boolean underwater) {
        long time = current.getGameTime();
        if (current != level || time < nextScan - SCAN_TICKS) {
            clear();
            level = current;
        }
        if (underwater || !openDimension) {
            return Frame.EMPTY;
        }

        boolean mistEnabled = AtmosphereSettings.riverMistEnabled();
        boolean powderEnabled = AtmosphereSettings.snowGustEnabled();
        boolean caveEnabled = AtmosphereSettings.caveAirEnabled();
        boolean newlyEnabled = (mistEnabled && !wasMistEnabled) || (powderEnabled && !wasPowderEnabled)
                || (caveEnabled && !wasCaveEnabled);
        wasMistEnabled = mistEnabled;
        wasPowderEnabled = powderEnabled;
        wasCaveEnabled = caveEnabled;
        if (!mistEnabled && !powderEnabled && !caveEnabled
                && Math.max(riverOpacity, Math.max(snowOpacity, caveOpacity)) < 0.002F) {
            return Frame.EMPTY;
        }

        Vec3 wind = AtmosphereWeather.wind(current);
        float dawn = dawn(current, partialTick);
        boolean riverVisible = mistEnabled && dawn > 0.001F && weather < 0.92F;
        boolean seekingSurface = (riverVisible || powderEnabled) && current.getBrightness(LightLayer.SKY, block) >= 8;
        float gustProgress = (Math.floorMod(time, SNOW_PERIOD) + partialTick) / 116F;
        if (scanCenter == null || newlyEnabled || time >= nextScan || scanCenter.distSqr(block) > 64.0) {
            scanCenter = block;
            nextScan = time + SCAN_TICKS;
            boolean searchSnow = powderEnabled && (snowBank == null || gustProgress >= 1F);
            if (seekingSurface && (riverVisible || searchSnow)) {
                sampleSurface(current, block, eye, wind, riverVisible, searchSnow);
            }
            Vec3 nextCave = caveEnabled ? findCaveBase(current, block) : null;
            if (nextCave != null && (caveBase == null || caveOpacity < 0.002F || caveBase.distanceToSqr(nextCave) > 13 * 13)) {
                if (caveBase == null || caveOpacity < 0.002F) {
                    caveHeight = (float) nextCave.y;
                }
                caveBase = nextCave;
            } else if (nextCave != null) {
                caveBase = new Vec3(caveBase.x, nextCave.y, caveBase.z);
            }
            cavePresent = nextCave != null;
        }

        long now = System.nanoTime();
        float follow = lastFrame == 0L ? 1F : (float) (1D - Math.exp(-5D * Math.min(0.15, (now - lastFrame) * 1e-9)));
        lastFrame = now;
        float riverTarget = seekingSurface && riverVisible && riverPresent ? dawn * (1F - weather) : 0F;
        float caveTarget = caveEnabled && cavePresent ? 1F : 0F;
        float snowTarget = seekingSurface && powderEnabled && snowBank != null
                ? pulse(gustProgress) * (0.76F + 0.24F * weather) : 0F;
        riverOpacity = Mth.lerp(follow, riverOpacity, riverTarget);
        snowOpacity = Mth.lerp(follow, snowOpacity, snowTarget);
        caveOpacity = Mth.lerp(follow, caveOpacity, caveTarget);

        if (snowOpacity > 0.002F && snowBank != null && gustProgress < 1.05F && powderEnabled) {
            double travel = -5 + Mth.clamp(gustProgress, 0F, 1F) * 17;
            lastGustCenter = snowBank.add(gustDirection.scale(travel)).add(0, -0.13 * Math.max(0, travel), 0);
            emitPowder(client, current, lastGustCenter, time, gustProgress);
        }
        Vec3 cave = null;
        if (caveBase != null) {
            caveHeight = Mth.lerp(follow, caveHeight, (float) caveBase.y);
            cave = new Vec3(caveBase.x, caveHeight, caveBase.z);
        }
        return new Frame(riverBanks, riverOpacity, cave, caveOpacity, lastGustCenter,
                gustDirection, snowOpacity, wind);
    }

    private void sampleSurface(ClientLevel current, BlockPos block, Vec3 eye, Vec3 wind, boolean mist, boolean powder) {
        var water = new ArrayList<Vec3>();
        Vec3 ridge = null;
        Vec3 direction = null;
        double ridgeScore = Double.POSITIVE_INFINITY;
        var cursor = new BlockPos.MutableBlockPos();
        int gridX = Math.floorDiv(block.getX(), SCAN_STEP) * SCAN_STEP;
        int gridZ = Math.floorDiv(block.getZ(), SCAN_STEP) * SCAN_STEP;
        for (int z = gridZ - SEARCH_RADIUS; z <= gridZ + SEARCH_RADIUS; z += SCAN_STEP) {
            for (int x = gridX - SEARCH_RADIUS; x <= gridX + SEARCH_RADIUS; x += SCAN_STEP) {
                if (!current.hasChunkAt(cursor.set(x, block.getY(), z))) {
                    continue;
                }

                int terrain = current.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                int top = terrain;
                if (Math.abs(top - block.getY()) > 24 || current.isOutsideBuildHeight(cursor.set(x, top, z))) {
                    continue;
                }
                var surface = current.getBlockState(cursor);
                if (mist && surface.getFluidState().is(FluidTags.WATER) && !surface.blocksMotion()
                        && current.getBiome(cursor).is(BiomeTags.IS_RIVER) && current.canSeeSky(cursor.above())) {
                    double y = top + surface.getFluidState().getHeight(current, cursor);
                    water.add(new Vec3(x + 0.5, y, z + 0.5));
                }
                if (!powder) {
                    continue;
                }

                cursor.setY(top + 1);
                var above = current.getBlockState(cursor);
                if (above.is(BlockTags.SNOW) || above.is(Blocks.POWDER_SNOW)) {
                    surface = above;
                    top++;
                }
                cursor.setY(top);
                if (!(surface.is(BlockTags.SNOW) || surface.is(Blocks.POWDER_SNOW))
                        || !current.canSeeSky(cursor.above())) {
                    continue;
                }

                Vec3 sample = new Vec3(x + 0.5, top + snowThickness(surface) + 0.15, z + 0.5);
                double distance = Math.sqrt(eye.distanceToSqr(sample));
                if (distance < 5.0) {
                    continue;
                }
                Vec3 downslope = slope(current, x, z, terrain, wind, cursor);
                if (downslope == null) {
                    continue;
                }
                double score = distance - Math.min(3.0, downslope.y) * 1.8;
                if (score < ridgeScore) {
                    ridgeScore = score;
                    ridge = sample;
                    direction = new Vec3(downslope.x, 0, downslope.z);
                }
            }
        }

        if (mist) {
            riverPresent = !water.isEmpty();
            if (riverPresent && (riverBanks.isEmpty() || riverOpacity < 0.002F || eye.distanceToSqr(riverBanks.get(0)) > 18 * 18)) {
                water.sort(Comparator.comparingDouble(eye::distanceToSqr));
                var selected = new ArrayList<Vec3>(RIVER_BANKS);
                for (Vec3 candidate : water) {
                    if (selected.stream().noneMatch(center -> center.distanceToSqr(candidate) < 7 * 7)) {
                        selected.add(candidate);
                        if (selected.size() == RIVER_BANKS) {
                            break;
                        }
                    }
                }
                riverBanks = selected;
            }
        }
        if (powder && (ridge == null || snowBank == null || snowOpacity < 0.002F && eye.distanceToSqr(snowBank) > 14 * 14)) {
            snowBank = ridge;
            if (direction != null) {
                gustDirection = direction;
            }
        }
    }

    private static float snowThickness(BlockState state) {
        return state.hasProperty(SnowLayerBlock.LAYERS) ? Math.max(1, state.getValue(SnowLayerBlock.LAYERS) - 1) / 8F : 1F;
    }

    private static Vec3 slope(ClientLevel current, int x, int z, int top, Vec3 wind, BlockPos.MutableBlockPos cursor) {
        Vec3 best = null;
        double bestScore = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx == 0 && dz == 0) || !current.hasChunkAt(cursor.set(x + dx * 4, top, z + dz * 4))) {
                    continue;
                }
                int neighbor = current.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        cursor.getX(), cursor.getZ()) - 1;
                if (top - neighbor < 2) {
                    continue;
                }
                double normalization = dx == 0 || dz == 0 ? 1.0 : 0.7071067811865476;
                double ux = dx * normalization;
                double uz = dz * normalization;
                double score = Math.min(5, top - neighbor) + (ux * wind.x + uz * wind.z) * 2.5;
                if (score > bestScore) {
                    bestScore = score;
                    best = new Vec3(ux, top - neighbor, uz);
                }
            }
        }
        return best;
    }

    private static Vec3 findCaveBase(ClientLevel current, BlockPos block) {
        if (!current.hasChunkAt(block) || current.getBrightness(LightLayer.SKY, block) > 3
                || current.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, block.getX(), block.getZ()) - block.getY() < 5) {
            return null;
        }
        var biome = current.getBiome(block);
        boolean caveBiome = biome.is(Tags.Biomes.IS_CAVE) || biome.is(Tags.Biomes.IS_UNDERGROUND);
        var cursor = block.mutable();
        for (int depth = 1; depth <= 10; depth++) {
            cursor.setY(block.getY() - depth);
            var state = current.getBlockState(cursor);
            if (!state.blocksMotion()) {
                continue;
            }
            if (caveBiome || state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                return new Vec3(block.getX() + 0.5, cursor.getY() + 1.0, block.getZ() + 0.5);
            }
            break;
        }
        if (caveBiome || current.getBlockState(block.above(4)).is(BlockTags.BASE_STONE_OVERWORLD)) {
            return new Vec3(block.getX() + 0.5, block.getY() - 1.5, block.getZ() + 0.5);
        }
        return null;
    }

    private void emitPowder(Minecraft client, ClientLevel current, Vec3 center, long time, float progress) {
        var setting = client.options.particles().get();
        if (client.isPaused() || time < nextGrain || progress > 0.9F || setting == ParticleStatus.MINIMAL) {
            return;
        }
        nextGrain = time + (setting == ParticleStatus.DECREASED ? 5 : 2);
        double across = (random.nextDouble() - 0.5) * 12;
        double along = (random.nextDouble() - 0.5) * 8;
        double x = center.x + gustDirection.x * along - gustDirection.z * across;
        double z = center.z + gustDirection.z * along + gustDirection.x * across;
        var position = BlockPos.containing(x, center.y, z);
        if (!current.hasChunkAt(position)) {
            return;
        }
        double y = current.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, position.getX(), position.getZ())
                + 0.25 + random.nextDouble() * 0.8;
        if (Math.abs(y - center.y) > 3.0) {
            return;
        }
        double speed = 0.14 + random.nextDouble() * 0.09;
        client.particleEngine.createParticle(ParticleTypes.SNOWFLAKE, x, y, z,
                gustDirection.x * speed, (random.nextDouble() - 0.5) * 0.05, gustDirection.z * speed);
    }

    private static float dawn(ClientLevel level, float partialTick) {
        float day = Math.floorMod(level.getDayTime() + 12000, 24000) - 12000 + partialTick;
        return smooth(-1750F, -300F, day) * (1F - smooth(500F, 2300F, day));
    }

    private static float pulse(float t) {
        return smooth(0.025F, 0.23F, t) * (1F - smooth(0.67F, 1F, t));
    }

    private static float smooth(float low, float high, float value) {
        float t = Mth.clamp((value - low) / (high - low), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    record Frame(List<Vec3> rivers, float riverOpacity, Vec3 cave, float caveOpacity, Vec3 snow,
                 Vec3 snowDirection, float snowOpacity, Vec3 wind) {
        private static final Frame EMPTY = new Frame(List.of(), 0, null, 0, null, Vec3.ZERO, 0, Vec3.ZERO);

        boolean active() {
            return riverOpacity > 0.002F && !rivers.isEmpty()
                    || caveOpacity > 0.002F && cave != null || snowOpacity > 0.002F && snow != null;
        }
    }
}
