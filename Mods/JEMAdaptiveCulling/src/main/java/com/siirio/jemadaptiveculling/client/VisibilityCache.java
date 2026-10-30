package com.siirio.jemadaptiveculling.client;

import com.siirio.jemadaptiveculling.config.JEMConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
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
    private static final long MAX_FRAME_NANOS = 250_000L;
    private static final double ENTITY_BOUNDS_MARGIN = 0.5D;
    private static final double MAX_EXTENT = 16.0D;
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_PENDING = MAX_ENTRIES * 2;
    private static final int MAX_ADMISSIONS_PER_FRAME = 256;
    private static final Int2ObjectLinkedOpenHashMap<Entry> ENTITIES = new Int2ObjectLinkedOpenHashMap<>();
    private static final Long2ObjectLinkedOpenHashMap<Entry> BLOCK_ENTITIES = new Long2ObjectLinkedOpenHashMap<>();
    private static final ArrayDeque<Entry> PENDING = new ArrayDeque<>();
    private static WeakReference<ClientLevel> observedLevel = new WeakReference<>(null);
    private static long frame;
    private static long window;
    private static long spentNanos;
    private static int queries;

    private VisibilityCache() {
    }

    public static boolean shouldRenderEntity(Entity entity, AABB bounds) {
        ClientLevel level = synchronizeLevel();
        if (entity.level() != level || !eligible(level, bounds)) return true;
        if (!(Math.abs(entity.xOld - entity.getX()) <= ENTITY_BOUNDS_MARGIN
                && Math.abs(entity.yOld - entity.getY()) <= ENTITY_BOUNDS_MARGIN
                && Math.abs(entity.zOld - entity.getZ()) <= ENTITY_BOUNDS_MARGIN)) return true;
        Entry entry = ENTITIES.getAndMoveToLast(entity.getId());
        if (entry == null || entry.owner.get() != entity) {
            if (entry != null) entry.active = false;
            if (entry == null && ENTITIES.size() >= MAX_ENTRIES) ENTITIES.removeFirst().active = false;
            entry = new Entry(entity);
            ENTITIES.putAndMoveToLast(entity.getId(), entry);
        }
        return check(bounds, entry, ENTITY_BOUNDS_MARGIN);
    }

    public static boolean shouldRenderBlockEntity(BlockEntity blockEntity, BlockEntityRenderer<?> renderer, float partialTick) {
        ClientLevel level = synchronizeLevel();
        if (level == null || blockEntity.getLevel() != level) return true;
        long position = blockEntity.getBlockPos().asLong();
        Entry entry = BLOCK_ENTITIES.getAndMoveToLast(position);
        if (entry == null || entry.owner.get() != blockEntity) {
            if (entry != null) entry.active = false;
            if (entry == null && BLOCK_ENTITIES.size() >= MAX_ENTRIES) BLOCK_ENTITIES.removeFirst().active = false;
            entry = new Entry(blockEntity);
            BLOCK_ENTITIES.putAndMoveToLast(position, entry);
        }
        AABB bounds = entry.chestBounds(blockEntity, renderer, partialTick, level);
        if (bounds == null) bounds = blockEntity.getRenderBoundingBox();
        return !eligible(level, bounds) || check(bounds, entry, 0);
    }

    public static void remove(Entity entity) {
        if (entity.level() != observedLevel.get()) return;
        Entry entry = ENTITIES.get(entity.getId());
        if (entry != null && entry.owner.get() == entity) {
            ENTITIES.remove(entity.getId());
            entry.active = false;
        }
    }

    public static void removeChunk(ChunkPos chunk) {
        var entries = BLOCK_ENTITIES.long2ObjectEntrySet().fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            long position = entry.getLongKey();
            if (BlockPos.getX(position) >> 4 == chunk.x && BlockPos.getZ(position) >> 4 == chunk.z) {
                entry.getValue().active = false;
                entries.remove();
            }
        }
    }

    public static boolean unload(ClientLevel level) {
        if (observedLevel.get() != level) return false;
        clear();
        return true;
    }

    public static void clear() {
        ENTITIES.clear();
        BLOCK_ENTITIES.clear();
        PENDING.clear();
        observedLevel.clear();
        window = 0;
        spentNanos = 0;
        queries = 0;
        GeometryTracker.clear();
        ShapeClassifier.clear();
    }

    public static void endFrame() {
        ClientLevel level = synchronizeLevel();
        if (level == null || PENDING.isEmpty()) {
            frame++;
            return;
        }
        long started = System.nanoTime();
        long currentWindow = started / WINDOW_NANOS;
        if (currentWindow != window) {
            window = currentWindow;
            spentNanos = 0;
            queries = 0;
        }
        long remaining = JEMConfig.TIME_BUDGET_MICROS.get() * 1000L - spentNanos;
        long deadline = started + Math.min(MAX_FRAME_NANOS, Math.max(0, remaining));
        int admissions = 0;
        while (!PENDING.isEmpty() && queries < JEMConfig.RAY_BUDGET.get()
                && admissions++ < MAX_ADMISSIONS_PER_FRAME && System.nanoTime() < deadline) {
            Entry entry = PENDING.removeFirst();
            entry.queued = false;
            if (!entry.active || entry.owner.get() == null || entry.lastSeenFrame != frame) continue;
            queries++;
            AABB solid = OpaqueOcclusion.findOccluder(level, entry.camera, entry.bounds, entry.margin, deadline);
            if (solid == null) {
                entry.visibleUntil = System.nanoTime() + (System.nanoTime() >= deadline ? WINDOW_NANOS : VISIBLE_NANOS);
            } else {
                entry.solid = solid;
                entry.epoch = GeometryTracker.epoch();
                entry.stamp = GeometryTracker.stamp(solid);
                entry.revision = GeometryTracker.revision();
            }
        }
        spentNanos += System.nanoTime() - started;
        frame++;
    }

    private static ClientLevel synchronizeLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != observedLevel.get()) {
            clear();
            observedLevel = new WeakReference<>(level);
        }
        return level;
    }

    private static boolean eligible(ClientLevel level, AABB bounds) {
        if (level == null || invalid(bounds)) return false;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (bounds.contains(camera)) return false;
        double dx = (bounds.minX + bounds.maxX) * 0.5D - camera.x;
        double dy = (bounds.minY + bounds.maxY) * 0.5D - camera.y;
        double dz = (bounds.minZ + bounds.maxZ) * 0.5D - camera.z;
        double limit = JEMConfig.MAX_DISTANCE.get();
        return dx * dx + dy * dy + dz * dz <= limit * limit;
    }

    private static boolean check(AABB bounds, Entry entry, double margin) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        entry.lastSeenFrame = frame;
        entry.camera = camera;
        entry.bounds = bounds;
        entry.margin = margin;
        if (entry.solid != null) {
            if (entry.epoch != GeometryTracker.epoch()) {
                entry.solid = null;
            } else if (entry.revision != GeometryTracker.revision()) {
                if (entry.stamp != GeometryTracker.stamp(entry.solid)) entry.solid = null;
                entry.revision = GeometryTracker.revision();
            }
            if (entry.solid != null) {
                if (OpaqueOcclusion.covers(entry.solid, camera, bounds, margin)) return false;
                entry.solid = null;
            }
        }
        if (System.nanoTime() >= entry.visibleUntil && !entry.queued && PENDING.size() < MAX_PENDING) {
            entry.queued = true;
            PENDING.addLast(entry);
        }
        return true;
    }

    private static boolean invalid(AABB bounds) {
        return bounds == null || !Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY) || !Double.isFinite(bounds.minZ)
                || !Double.isFinite(bounds.maxX) || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)
                || bounds.getXsize() <= 0 || bounds.getYsize() <= 0 || bounds.getZsize() <= 0
                || bounds.getXsize() > MAX_EXTENT || bounds.getYsize() > MAX_EXTENT || bounds.getZsize() > MAX_EXTENT;
    }

    private static final class Entry {
        private final WeakReference<Object> owner;
        private boolean active = true;
        private boolean queued;
        private long lastSeenFrame;
        private long visibleUntil;
        private Vec3 camera;
        private AABB bounds;
        private double margin;
        private AABB solid;
        private long epoch;
        private long stamp;
        private long revision;
        private BlockState chestState;
        private AABB closedChestBounds;
        private BlockPos connectedChest;

        private Entry(Object owner) {
            this.owner = new WeakReference<>(owner);
        }

        private AABB chestBounds(BlockEntity blockEntity, BlockEntityRenderer<?> renderer, float partialTick, ClientLevel level) {
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
            if (connectedChest != null && (!(level.getBlockEntity(connectedChest) instanceof ChestBlockEntity connected)
                    || connected.getOpenNess(partialTick) > 0)) return null;
            return closedChestBounds;
        }
    }
}
