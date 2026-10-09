package com.siirio.jemadaptiveculling.client;

import com.siirio.jemadaptiveculling.config.JEMConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VisibilityCache {
    private static final long WINDOW_NANOS = 50_000_000L;
    private static final long VISIBLE_NANOS = 500_000_000L;
    private static final long NANOS_PER_MICROSECOND = 1000L;
    private static final double ENTITY_BOUNDS_MARGIN = 0.5D;
    private static final double MAX_EXTENT = 16.0D;
    private static final int MAX_ENTRIES = 4096;
    private static final int CORNER_COUNT = 8;
    private static final Int2ObjectOpenHashMap<Entry> ENTITIES = new Int2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Entry> BLOCK_ENTITIES = new Long2ObjectOpenHashMap<>();
    private static ClientLevel observedLevel;
    private static long window;
    private static long spentNanos;
    private static int rays;

    private VisibilityCache() {
    }

    public static boolean shouldRenderEntity(int id, AABB bounds) {
        updateLevel();
        Entry entry = ENTITIES.get(id);
        if (entry == null) {
            if (ENTITIES.size() >= MAX_ENTRIES) ENTITIES.clear();
            entry = new Entry();
            ENTITIES.put(id, entry);
        }
        return check(bounds, entry, ENTITY_BOUNDS_MARGIN);
    }

    public static boolean shouldRenderBlockEntity(BlockEntity blockEntity, BlockEntityRenderer<?> renderer, float partialTick) {
        updateLevel();
        long position = blockEntity.getBlockPos().asLong();
        Entry entry = BLOCK_ENTITIES.get(position);
        if (entry == null || entry.blockEntity != blockEntity) {
            if (BLOCK_ENTITIES.size() >= MAX_ENTRIES) BLOCK_ENTITIES.clear();
            entry = new Entry();
            entry.blockEntity = blockEntity;
            BLOCK_ENTITIES.put(position, entry);
        }
        AABB bounds = entry.chestBounds(renderer, partialTick);
        return check(bounds == null ? blockEntity.getRenderBoundingBox() : bounds, entry, 0);
    }

    private static void updateLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != observedLevel) {
            ENTITIES.clear();
            BLOCK_ENTITIES.clear();
            observedLevel = level;
        }
    }

    private static boolean check(AABB bounds, Entry entry, double margin) {
        if (observedLevel == null || invalid(bounds)) return true;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = (bounds.minX + bounds.maxX) * 0.5D - camera.x;
        double dy = (bounds.minY + bounds.maxY) * 0.5D - camera.y;
        double dz = (bounds.minZ + bounds.maxZ) * 0.5D - camera.z;
        double limit = JEMConfig.MAX_DISTANCE.get();
        if (dx * dx + dy * dy + dz * dz > limit * limit || bounds.contains(camera)) return true;
        long now = System.nanoTime();
        if (entry.complete && entry.visible && now < entry.expiresAt) return true;
        if (!camera.equals(entry.camera) || !bounds.equals(entry.bounds) || entry.revision != GeometryTracker.revision()
                || entry.complete && entry.visible) {
            entry.camera = camera;
            entry.bounds = bounds;
            entry.revision = GeometryTracker.revision();
            entry.corner = 0;
            entry.complete = false;
            entry.visible = true;
        }
        if (entry.complete) return entry.visible;
        long currentWindow = now / WINDOW_NANOS;
        if (currentWindow != window) {
            window = currentWindow;
            spentNanos = 0;
            rays = 0;
        }
        long budget = JEMConfig.TIME_BUDGET_MICROS.get() * NANOS_PER_MICROSECOND;
        if (rays >= JEMConfig.RAY_BUDGET.get() || spentNanos >= budget) return true;
        long started = System.nanoTime();
        long deadline = started + budget - spentNanos;
        while (entry.corner <= CORNER_COUNT && rays < JEMConfig.RAY_BUDGET.get() && System.nanoTime() < deadline) {
            int corner = entry.corner++;
            double targetX = corner == CORNER_COUNT ? (bounds.minX + bounds.maxX) * 0.5D
                    : (corner & 1) == 0 ? Math.nextUp(bounds.minX - margin) : Math.nextDown(bounds.maxX + margin);
            double targetY = corner == CORNER_COUNT ? (bounds.minY + bounds.maxY) * 0.5D
                    : (corner & 2) == 0 ? Math.nextUp(bounds.minY - margin) : Math.nextDown(bounds.maxY + margin);
            double targetZ = corner == CORNER_COUNT ? (bounds.minZ + bounds.maxZ) * 0.5D
                    : (corner & 4) == 0 ? Math.nextUp(bounds.minZ - margin) : Math.nextDown(bounds.maxZ + margin);
            rays++;
            if (!OpaqueOcclusion.blocked(observedLevel, camera, new Vec3(targetX, targetY, targetZ))) {
                entry.complete = true;
                entry.visible = true;
                entry.expiresAt = now + VISIBLE_NANOS;
                break;
            }
        }
        spentNanos += System.nanoTime() - started;
        if (!entry.complete && entry.corner > CORNER_COUNT) {
            entry.complete = true;
            entry.visible = false;
        }
        return entry.visible;
    }

    private static boolean invalid(AABB bounds) {
        return !Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY) || !Double.isFinite(bounds.minZ)
                || !Double.isFinite(bounds.maxX) || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)
                || bounds.getXsize() <= 0 || bounds.getYsize() <= 0 || bounds.getZsize() <= 0
                || bounds.getXsize() > MAX_EXTENT || bounds.getYsize() > MAX_EXTENT || bounds.getZsize() > MAX_EXTENT;
    }

    private static final class Entry {
        private boolean visible = true;
        private boolean complete;
        private long expiresAt;
        private Vec3 camera;
        private AABB bounds;
        private long revision;
        private int corner;
        private BlockEntity blockEntity;
        private BlockState chestState;
        private AABB closedChestBounds;
        private BlockPos connectedChest;

        private AABB chestBounds(BlockEntityRenderer<?> renderer, float partialTick) {
            BlockState state = blockEntity.getBlockState();
            if (renderer.getClass() != ChestRenderer.class || !(blockEntity instanceof ChestBlockEntity chest)
                    || !state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST)) return null;
            if (chestState != state) {
                chestState = state;
                BlockPos position = blockEntity.getBlockPos();
                closedChestBounds = new AABB(position);
                connectedChest = state.getValue(ChestBlock.TYPE) == ChestType.SINGLE ? null
                        : position.relative(ChestBlock.getConnectedDirection(state));
                if (connectedChest != null) closedChestBounds = closedChestBounds.minmax(new AABB(connectedChest));
            }
            if (chest.getOpenNess(partialTick) > 0) return null;
            if (connectedChest != null && (observedLevel == null
                    || !(observedLevel.getBlockEntity(connectedChest) instanceof ChestBlockEntity connected)
                    || connected.getOpenNess(partialTick) > 0)) return null;
            return closedChestBounds;
        }
    }
}
