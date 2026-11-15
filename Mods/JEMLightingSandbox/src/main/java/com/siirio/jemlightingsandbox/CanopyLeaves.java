package com.siirio.jemlightingsandbox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class CanopyLeaves {
    private static final int SEARCH_RADIUS = 19;
    private static final int SEARCH_ATTEMPTS = 26;
    private static final int QUIET_INTERVAL = 42;
    private static final Direction[] EDGES = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
    static final ResourceLocation GALMIUM = ResourceLocation.tryParse("deep_dark_regrowth:galmium_leaves");
    private static final RandomSource RANDOM = RandomSource.create();
    private static ClientLevel currentLevel;
    private static long nextSearch;
    private static long lastEmission;
    private static BlockPos lastSource;
    private static Release release;

    private CanopyLeaves() {
    }

    static void tick(Minecraft client) {
        var level = client.level;
        if (!enabled(level)) {
            clear();
            return;
        }
        if (client.isPaused()) {
            return;
        }
        var camera = client.gameRenderer.getMainCamera();
        if (level != currentLevel) {
            clear();
            currentLevel = level;
        }
        long time = level.getGameTime();
        if (release != null) {
            if (time >= release.next()) {
                emit(client, level, time);
            }
            return;
        }
        if (time < nextSearch) {
            return;
        }
        nextSearch = time + QUIET_INTERVAL;
        Vec3 wind = SandboxWeather.wind(level);
        Direction edge = downwindEdge(wind);
        var position = camera.getBlockPosition();
        if (!level.hasChunkAt(position)) {
            return;
        }
        boolean sheltered = level.getBrightness(LightLayer.SKY, position) < 9;
        for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
            int x = position.getX() + RANDOM.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = position.getZ() + RANDOM.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            BlockPos column = new BlockPos(x, position.getY(), z);
            if (!level.hasChunkAt(column)) {
                continue;
            }

            int crownTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            int top = Math.min(crownTop, position.getY() + (sheltered ? 8 : 20));
            int bottom = Math.max(position.getY() - 7, top - 7);
            for (int y = top; y >= bottom; y--) {
                BlockPos source = new BlockPos(x, y, z);
                if (!canRelease(level, source, edge)) {
                    continue;
                }
                var state = level.getBlockState(source);
                var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                boolean snowy = level.getBiome(source).value().getPrecipitationAt(source) == Biome.Precipitation.SNOW;
                boolean gust = wind.lengthSqr() > 0.22 || RANDOM.nextInt(5) == 0;
                int count = client.options.particles().get() == ParticleStatus.DECREASED
                        ? (gust ? 3 : 1) : (gust ? 7 : 3);
                release = new Release(source, id, edge, wind, gust, snowy, count, time);
                emit(client, level, time);
                return;
            }
        }
    }

    private static void emit(Minecraft client, ClientLevel level, long time) {
        var current = release;
        if (!canRelease(level, current.source(), current.edge())) {
            release = null;
            return;
        }
        var source = current.source();
        var state = level.getBlockState(source);
        if (!current.block().equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()))) {
            release = null;
            return;
        }
        var edge = current.edge();
        double cross = (RANDOM.nextDouble() - 0.5) * 0.76;
        double x = source.getX() + 0.5 + edge.getStepX() * 0.57 + Math.abs(edge.getStepZ()) * cross;
        double z = source.getZ() + 0.5 + edge.getStepZ() * 0.57 + Math.abs(edge.getStepX()) * cross;
        double y = source.getY() + 0.4 + RANDOM.nextDouble() * 0.5;
        if (!CanopyLeafParticle.spawn(client, level, source, state, new Vec3(x, y, z),
                current.wind(), current.gust(), current.snowy())) {
            release = null;
            nextSearch = time + QUIET_INTERVAL;
            return;
        }
        lastEmission = time;
        lastSource = source;
        int remaining = current.remaining() - 1;
        int delay = (current.gust() ? 4 : 10) + RANDOM.nextInt(current.gust() ? 5 : 8);
        release = remaining == 0 ? null : new Release(source, current.block(), edge, current.wind(),
                current.gust(), current.snowy(), remaining, time + delay);
        if (release == null) {
            nextSearch = time + QUIET_INTERVAL + RANDOM.nextInt(38);
        }
    }

    private static boolean canRelease(ClientLevel level, BlockPos source, Direction edge) {
        BlockPos exposed = source.relative(edge);
        if (level.isOutsideBuildHeight(source) || !level.hasChunkAt(source) || !level.hasChunkAt(exposed)
                || !level.isEmptyBlock(exposed)) {
            return false;
        }
        BlockState state = level.getBlockState(source);
        if (!isCanopy(state) || state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
            return false;
        }
        return level.getBrightness(LightLayer.SKY, source) >= 9
                || GALMIUM.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    private static boolean isCanopy(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && !id.getPath().contains("fallen_leaves")
                && (state.getBlock() instanceof LeavesBlock || id.equals(GALMIUM)
                || state.is(BlockTags.LEAVES) && id.getPath().endsWith("_leaves"));
    }

    public static boolean replacesFallingLeaf(Level level, BlockPos pos, BlockState state) {
        return level == currentLevel && currentLevel != null && pos.equals(lastSource) && isCanopy(state)
                && currentLevel.getGameTime() - lastEmission <= 20 && enabled(currentLevel);
    }

    private static boolean enabled(ClientLevel level) {
        if (level == null || !LightingSettings.canopyLeavesEnabled() || !SandboxWeather.hasOutdoorWeather(level)) {
            return false;
        }
        var client = Minecraft.getInstance();
        var camera = client.gameRenderer.getMainCamera();
        return client.options.particles().get() != ParticleStatus.MINIMAL && camera.isInitialized()
                && camera.getFluidInCamera() == FogType.NONE && !BiomeFog.hasRestrictedVision(camera);
    }

    private static Direction downwindEdge(Vec3 wind) {
        return EDGES[Math.abs(wind.x) >= Math.abs(wind.z) ? (wind.x >= 0 ? 0 : 1) : (wind.z >= 0 ? 2 : 3)];
    }

    static void clear() {
        currentLevel = null;
        release = null;
        nextSearch = 0;
        lastEmission = 0;
        lastSource = null;
    }

    private record Release(BlockPos source, ResourceLocation block, Direction edge, Vec3 wind, boolean gust,
                           boolean snowy, int remaining, long next) {}
}
