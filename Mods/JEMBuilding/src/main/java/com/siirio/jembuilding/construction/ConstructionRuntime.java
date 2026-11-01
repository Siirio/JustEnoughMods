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
    private static final Deque<RegionWork> WORLD_WORK = new ArrayDeque<>();
    private static final Map<UUID, FillWork> FILL_WORK = new HashMap<>();
    private static final Set<String> CHUNK_WORK = new HashSet<>();
    private static final ThreadLocal<Boolean> NATIVE_FILL = ThreadLocal.withInitial(() -> false);

    public static boolean isNativeFill() {
        return NATIVE_FILL.get();
    }

    public static void confirm(ServerPlayer player, InteractionHand hand, ConstructionPlane plane, BlockPos requestedEnd) {
        ItemStack hammer = player.getItemInHand(hand);
        if (!(hammer.getItem() instanceof BuilderHammerItem)) return;
        Optional<BlockPos> selected = BuilderHammerItem.start(hammer);
        if (selected.isEmpty()) return;
        BlockPos start = selected.get();
        BlockPos end = plane.constrain(start, requestedEnd);
        ServerLevel level = player.serverLevel();
        if (!plane.contains(start, end) || !within(level, start) || !within(level, end)) return;
        ConstructionData.Region region = ConstructionData.get(level).create(player.getUUID(), plane, start, end);
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
            FILL_WORK.put(serverPlayer.getUUID(), new FillWork(serverPlayer.serverLevel(), serverPlayer.getUUID(), hand,
                    region, region.index(clicked), material));
        });
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

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(region.owner());
        if (owner == null || owner.serverLevel() != level) return;
        BlockState current = level.getBlockState(pos);
        if (remove) {
            if (current.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get()) && permitted(owner, pos, owner.getMainHandItem())) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return;
        }
        if (current.is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get()) || !current.canBeReplaced()) return;
        ItemStack permissionStack = new ItemStack(BuildingContent.BUILDER_HAMMER.get());
        if (!permitted(owner, pos, permissionStack)) return;
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, BuildingContent.CONSTRUCTION_PLACEHOLDER.get().defaultBlockState(), Block.UPDATE_ALL)) return;
        if (ForgeEventFactory.onBlockPlace(owner, snapshot, Direction.UP)) snapshot.restore(true, false);
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
            int minX = Math.max(region.minX(), chunk.getMinBlockX());
            int maxX = Math.min(region.maxX(), chunk.getMaxBlockX());
            int minZ = Math.max(region.minZ(), chunk.getMinBlockZ());
            int maxZ = Math.min(region.maxZ(), chunk.getMaxBlockZ());
            if (minX <= maxX && minZ <= maxZ) {
                switch (region.plane()) {
                    case XY -> {
                        if (region.minZ() >= chunk.getMinBlockZ() && region.minZ() <= chunk.getMaxBlockZ()) {
                            for (int y = region.minY(); y <= region.maxY(); y++) for (int x = minX; x <= maxX; x++) positions.add(new BlockPos(x, y, region.minZ()));
                        }
                    }
                    case ZY -> {
                        if (region.minX() >= chunk.getMinBlockX() && region.minX() <= chunk.getMaxBlockX()) {
                            for (int y = region.minY(); y <= region.maxY(); y++) for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(region.minX(), y, z));
                        }
                    }
                    case XZ -> {
                        for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) positions.add(new BlockPos(x, region.minY(), z));
                    }
                }
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
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty() || !(held.getItem() instanceof BlockItem) || !ItemStack.isSameItemSameTags(held, material)) return true;
            long cells = region.cells();
            int work = 0;
            while (work++ < budget && scanned < cells && !held.isEmpty()) {
                long index = (start + scanned++) % cells;
                BlockPos pos = region.position(index);
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(BuildingContent.CONSTRUCTION_PLACEHOLDER.get()) || !permitted(player, pos, held)) continue;
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                NATIVE_FILL.set(true);
                try { player.gameMode.useItemOn(player, level, held, hand, hit); }
                finally { NATIVE_FILL.set(false); }
            }
            player.getInventory().setChanged();
            return held.isEmpty() || scanned >= cells;
        }
    }

    private ConstructionRuntime() { }
}
