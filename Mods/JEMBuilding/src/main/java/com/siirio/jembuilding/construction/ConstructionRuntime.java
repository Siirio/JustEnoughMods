package com.siirio.jembuilding.construction;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.item.BuilderHammerItem;
import com.siirio.jembuilding.registry.BuildingContent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ConstructionRuntime {
    private static final int WORLD_WORK_PER_TICK = 512;
    private static final int FILL_SCAN_PER_TICK = 256;
    private static final int EXPIRY_CHECK_INTERVAL_TICKS = 200;
    private static final long TEMPORARY_LIFETIME_TICKS = 7L * 24_000L;
    private static final Deque<RegionWork> WORLD_WORK = new ArrayDeque<>();
    private static final Map<UUID, FillWork> FILL_WORK = new HashMap<>();
    private static final Map<ServerLevel, PriorityQueue<ConstructionData.Region>> EXPIRY_QUEUES = new HashMap<>();
    private static final Set<String> CHUNK_WORK = new HashSet<>();
    private static final ThreadLocal<Boolean> NATIVE_FILL = ThreadLocal.withInitial(() -> false);

    public static boolean isNativeFill() {
        return NATIVE_FILL.get();
    }

    public static void selectMode(ServerPlayer player, InteractionHand hand, BlockPos clicked, Direction face,
                                  ConstructionShape shape, ConstructionPlane requestedPlane) {
        ItemStack hammer = player.getItemInHand(hand);
        if (!(hammer.getItem() instanceof BuilderHammerItem) || !player.serverLevel().hasChunkAt(clicked)
                || player.distanceToSqr(Vec3.atCenterOf(clicked)) > 64D || player.serverLevel().getBlockState(clicked).isAir()) return;
        BlockPos start = clicked.relative(face);
        if (!within(player.serverLevel(), start)) return;
        ConstructionPlane plane = switch (shape) {
            case WALL -> ConstructionPlane.wallFromLook(player.getLookAngle());
            case FLOOR -> ConstructionPlane.XZ;
            case COLUMN, SPHERE -> ConstructionPlane.XY;
            case CYLINDER -> ConstructionPlane.XZ;
        };
        BuilderHammerItem.setStart(hammer, start, shape, plane);
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable("message.jem_building.first_position",
                Component.translatable("screen.jem_building.mode." + shape.name().toLowerCase()), start.getX(), start.getY(), start.getZ()), true);
    }

    public static void confirm(ServerPlayer player, InteractionHand hand, BlockPos requestedEnd) {
        ItemStack hammer = player.getItemInHand(hand);
        if (!(hammer.getItem() instanceof BuilderHammerItem)) return;
        Optional<BlockPos> selected = BuilderHammerItem.start(hammer);
        Optional<ConstructionShape> selectedShape = BuilderHammerItem.shape(hammer);
        Optional<ConstructionPlane> selectedPlaneValue = BuilderHammerItem.plane(hammer);
        if (selected.isEmpty() || selectedShape.isEmpty() || selectedPlaneValue.isEmpty()) return;
        BlockPos start = selected.get();
        ConstructionShape shape = selectedShape.get();
        ConstructionPlane selectedPlane = selectedPlaneValue.get();
        BlockPos end = ConstructionGeometry.constrain(shape, selectedPlane, start, requestedEnd);
        ServerLevel level = player.serverLevel();
        if (!ConstructionGeometry.validSpan(start, end) || !within(level, start) || !within(level, end)
                || ((shape == ConstructionShape.WALL || shape == ConstructionShape.FLOOR) && !selectedPlane.contains(start, end))) return;
        if (shape == ConstructionShape.SPHERE) {
            int radius = ConstructionGeometry.sphereRadius(start, end);
            if (!within(level, start.offset(-radius, -radius, -radius)) || !within(level, start.offset(radius, radius, radius))) {
                player.displayClientMessage(Component.translatable("message.jem_building.outside_world"), true);
                return;
            }
        } else if (shape == ConstructionShape.CYLINDER) {
            int radius = ConstructionGeometry.cylinderRadius(start, end);
            if (!within(level, new BlockPos(start.getX() - radius, Math.min(start.getY(), end.getY()), start.getZ() - radius))
                    || !within(level, new BlockPos(start.getX() + radius, Math.max(start.getY(), end.getY()), start.getZ() + radius))) {
                player.displayClientMessage(Component.translatable("message.jem_building.cylinder_outside_world"), true);
                return;
            }
        }
        PriorityQueue<ConstructionData.Region> expiryQueue = expiryQueue(level);
        ConstructionData.Region region = ConstructionData.get(level).create(player.getUUID(), shape, selectedPlane, start, end,
                level.getGameTime() + TEMPORARY_LIFETIME_TICKS);
        expiryQueue.add(region);
        WORLD_WORK.addLast(new RegionWork(level, region, false));
        BuilderHammerItem.clear(hammer);
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable("message.jem_building.plan_confirmed", region.cells()), true);
    }

    public static void fill(net.minecraft.world.entity.player.Player player, InteractionHand hand, BlockPos clicked) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) return;
        ConstructionData.get(serverPlayer.serverLevel()).at(clicked).ifPresent(region -> {
            if (region.cancelled()) return;
            ItemStack material = stack.copy();
            material.setCount(1);
            if (serverPlayer.getAbilities().instabuild) {
                fillCreative(serverPlayer, hand, region, region.index(clicked), material);
                return;
            }
            FILL_WORK.put(serverPlayer.getUUID(), new FillWork(serverPlayer.serverLevel(), serverPlayer.getUUID(), hand,
                    region, region.index(clicked), material));
        });
    }

    private static void fillCreative(ServerPlayer player, InteractionHand hand, ConstructionData.Region region, long start, ItemStack material) {
        ServerLevel level = player.serverLevel();
        long cells = region.cells();
        for (long scanned = 0; scanned < cells; scanned++) {
            BlockPos pos = region.position((start + scanned) % cells);
            if (!level.hasChunkAt(pos)) continue;
            BlockState current = level.getBlockState(pos);
            if (!current.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get()) && !current.canBeReplaced()) continue;
            ItemStack held = player.getItemInHand(hand);
            if (!ItemStack.isSameItemSameTags(held, material) || !permitted(player, pos, held)) break;
            placeNative(player, hand, pos, held);
        }
        player.getInventory().setChanged();
    }

    private static void placeNative(ServerPlayer player, InteractionHand hand, BlockPos pos, ItemStack held) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        NATIVE_FILL.set(true);
        try { player.gameMode.useItemOn(player, player.serverLevel(), held, hand, hit); }
        finally { NATIVE_FILL.set(false); }
    }

    public static void cancel(net.minecraft.world.entity.player.Player player, BlockPos clicked) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ConstructionData data = ConstructionData.get(serverPlayer.serverLevel());
        data.at(clicked).ifPresent(region -> {
            if (!region.owner().equals(serverPlayer.getUUID()) && !serverPlayer.hasPermissions(2)) return;
            data.cancel(region);
            WORLD_WORK.addLast(new RegionWork(serverPlayer.serverLevel(), region, true));
            serverPlayer.displayClientMessage(Component.translatable("message.jem_building.plan_cancelled"), true);
        });
    }

    public static long remainingLifetime(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (!level.hasChunkAt(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64D
                || !level.getBlockState(pos).is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get())) return -1L;
        return ConstructionData.get(level).at(pos)
                .map(region -> Math.max(0L, region.expiresAtGameTime() - level.getGameTime()))
                .orElse(-1L);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % EXPIRY_CHECK_INTERVAL_TICKS == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) expireDue(level);
        }
        int remaining = WORLD_WORK_PER_TICK;
        int rounds = WORLD_WORK.size();
        while (remaining > 0 && rounds-- > 0 && !WORLD_WORK.isEmpty()) {
            RegionWork work = WORLD_WORK.removeFirst();
            int used = work.step(Math.min(64, remaining));
            remaining -= Math.max(1, used);
            if (!work.done()) WORLD_WORK.addLast(work);
            else work.completed();
        }
        Iterator<Map.Entry<UUID, FillWork>> fills = FILL_WORK.entrySet().iterator();
        while (fills.hasNext()) if (fills.next().getValue().step(FILL_SCAN_PER_TICK)) fills.remove();
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ChunkPos chunk = event.getChunk().getPos();
        for (ConstructionData.Region region : ConstructionData.get(level).all()) {
            if (!region.intersects(chunk)) continue;
            String key = level.dimension().location() + ":" + region.id() + ":" + chunk.toLong();
            if (CHUNK_WORK.add(key)) WORLD_WORK.addLast(RegionWork.forChunk(level, region, chunk, key));
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        WORLD_WORK.clear();
        FILL_WORK.clear();
        EXPIRY_QUEUES.clear();
        CHUNK_WORK.clear();
        NATIVE_FILL.remove();
    }

    private static boolean within(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
    }

    private static boolean permitted(ServerPlayer player, BlockPos pos, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        return player.mayBuild() && level.hasChunkAt(pos) && within(level, pos)
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, stack);
    }

    private static void apply(ServerLevel level, ConstructionData.Region region, BlockPos pos, boolean remove) {
        BlockState current = level.getBlockState(pos);
        if (remove) {
            boolean claimedByActiveRegion = ConstructionData.get(level).at(pos)
                    .filter(active -> !active.id().equals(region.id())).isPresent();
            if (current.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get()) && !claimedByActiveRegion) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(region.owner());
        if (owner == null || owner.serverLevel() != level) return;
        if (current.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get()) || !current.canBeReplaced()) return;
        ItemStack permissionStack = new ItemStack(BuildingContent.BUILDER_HAMMER.get());
        if (!permitted(owner, pos, permissionStack)) return;
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, BuildingContent.CONSTRUCTION_PLACEHOLDER.get().defaultBlockState(), Block.UPDATE_ALL)) return;
        if (ForgeEventFactory.onBlockPlace(owner, snapshot, Direction.UP)) snapshot.restore(true, false);
    }

    private static PriorityQueue<ConstructionData.Region> expiryQueue(ServerLevel level) {
        return EXPIRY_QUEUES.computeIfAbsent(level, key -> {
            ConstructionData data = ConstructionData.get(key);
            data.initializeMissingExpirations(key.getGameTime() + TEMPORARY_LIFETIME_TICKS);
            PriorityQueue<ConstructionData.Region> queue = new PriorityQueue<>(
                    java.util.Comparator.comparingLong(ConstructionData.Region::expiresAtGameTime));
            for (ConstructionData.Region region : data.all()) if (!region.cancelled()) queue.add(region);
            return queue;
        });
    }

    private static void expireDue(ServerLevel level) {
        PriorityQueue<ConstructionData.Region> queue = expiryQueue(level);
        ConstructionData data = ConstructionData.get(level);
        long gameTime = level.getGameTime();
        while (!queue.isEmpty() && queue.peek().expiresAtGameTime() <= gameTime) {
            ConstructionData.Region region = queue.remove();
            if (region.cancelled()) continue;
            data.cancel(region);
            WORLD_WORK.addLast(new RegionWork(level, region, true));
        }
    }

    private static final class RegionWork {
        private final ServerLevel level;
        private final ConstructionData.Region region;
        private final boolean remove;
        private final List<BlockPos> chunkPositions;
        private final String chunkKey;
        private long cursor;

        private RegionWork(ServerLevel level, ConstructionData.Region region, boolean remove) {
            this(level, region, remove, null, null);
        }

        private RegionWork(ServerLevel level, ConstructionData.Region region, boolean remove, List<BlockPos> chunkPositions, String chunkKey) {
            this.level = level;
            this.region = region;
            this.remove = remove;
            this.chunkPositions = chunkPositions;
            this.chunkKey = chunkKey;
        }

        static RegionWork forChunk(ServerLevel level, ConstructionData.Region region, ChunkPos chunk, String key) {
            List<BlockPos> positions = new ArrayList<>();
            for (BlockPos pos : region.positions()) {
                if (pos.getX() >= chunk.getMinBlockX() && pos.getX() <= chunk.getMaxBlockX()
                        && pos.getZ() >= chunk.getMinBlockZ() && pos.getZ() <= chunk.getMaxBlockZ()) positions.add(pos);
            }
            return new RegionWork(level, region, region.cancelled(), positions, key);
        }

        int step(int budget) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(region.owner());
            if (owner == null || owner.serverLevel() != level) return 0;
            int used = 0;
            long size = chunkPositions == null ? region.cells() : chunkPositions.size();
            while (used < budget && cursor < size) {
                BlockPos pos = chunkPositions == null ? region.position(cursor) : chunkPositions.get((int) cursor);
                cursor++;
                used++;
                if (level.hasChunkAt(pos)) apply(level, region, pos, remove || region.cancelled());
            }
            return used;
        }

        boolean done() { return cursor >= (chunkPositions == null ? region.cells() : chunkPositions.size()); }
        void completed() { if (chunkKey != null) CHUNK_WORK.remove(chunkKey); }
    }

    private static final class FillWork {
        private final ServerLevel level;
        private final UUID playerId;
        private final InteractionHand hand;
        private final ConstructionData.Region region;
        private final long start;
        private final ItemStack material;
        private long scanned;

        private FillWork(ServerLevel level, UUID playerId, InteractionHand hand, ConstructionData.Region region, long start, ItemStack material) {
            this.level = level;
            this.playerId = playerId;
            this.hand = hand;
            this.region = region;
            this.start = start;
            this.material = material;
        }

        boolean step(int budget) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.serverLevel() != level || region.cancelled()) return true;
            ItemStack held = matchingHeldOrRefill(player);
            if (held == null) return true;
            long cells = region.cells();
            int work = 0;
            while (work++ < budget && scanned < cells) {
                if (held.isEmpty()) {
                    held = matchingHeldOrRefill(player);
                    if (held == null) return true;
                }
                long index = (start + scanned++) % cells;
                BlockPos pos = region.position(index);
                if (!level.hasChunkAt(pos) || !permitted(player, pos, held)) continue;
                BlockState current = level.getBlockState(pos);
                if (!current.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get()) && !current.canBeReplaced()) continue;
                placeNative(player, hand, pos, held);
            }
            player.getInventory().setChanged();
            return scanned >= cells;
        }

        private ItemStack matchingHeldOrRefill(ServerPlayer player) {
            ItemStack held = player.getItemInHand(hand);
            if (!held.isEmpty()) return ItemStack.isSameItemSameTags(held, material) ? held : null;
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                ItemStack candidate = inventory.items.get(slot);
                if (candidate.isEmpty() || !ItemStack.isSameItemSameTags(candidate, material)) continue;
                inventory.items.set(slot, ItemStack.EMPTY);
                player.setItemInHand(hand, candidate);
                return candidate;
            }
            return null;
        }
    }

    private ConstructionRuntime() { }
}
