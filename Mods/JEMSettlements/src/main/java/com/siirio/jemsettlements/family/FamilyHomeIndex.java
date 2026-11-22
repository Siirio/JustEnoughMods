package com.siirio.jemsettlements.family;

import com.siirio.jemsettlements.JemSettlements;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Loaded storage index and persistent-session ownership for native home POIs. */
@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID)
public final class FamilyHomeIndex {
    private static final int ASSOCIATION_RADIUS = 32;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> STORAGE = new HashMap<>();
    private static final Map<GlobalPos, UUID> BED_OWNERS = new HashMap<>();
    private static final Map<GlobalPos, UUID> JOB_OWNERS = new HashMap<>();
    private static final Map<GlobalPos, UUID> STORAGE_OWNERS = new HashMap<>();

    private FamilyHomeIndex() { }

    public static BlockPos bed(ServerLevel level, LivingEntity resident, GlobalPos assigned) {
        GlobalPos memory = resident instanceof Villager villager
                ? villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null)
                : resident.getSleepingPos().map(pos -> GlobalPos.of(level.dimension(), pos)).orElse(null);
        BlockPos selected = validBed(level, assigned) ? normalizeBed(level, assigned.pos())
                : validBed(level, memory) ? normalizeBed(level, memory.pos())
                : level.getPoiManager().findClosest(holder -> holder.is(PoiTypes.HOME), resident.blockPosition(),
                        ASSOCIATION_RADIUS, PoiManager.Occupancy.ANY).map(pos -> normalizeBed(level, pos)).orElse(null);
        return claim(level, selected, resident.getUUID(), BED_OWNERS) ? selected : null;
    }

    public static BlockPos workstation(ServerLevel level, LivingEntity resident, GlobalPos assigned) {
        GlobalPos memory = resident instanceof Villager villager
                ? villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null) : null;
        BlockPos selected = validJob(level, assigned) ? assigned.pos()
                : validJob(level, memory) ? memory.pos()
                : level.getPoiManager().findClosest(FamilyHomeIndex::workPoi, resident.blockPosition(),
                        ASSOCIATION_RADIUS, PoiManager.Occupancy.ANY).orElse(null);
        return claim(level, selected, resident.getUUID(), JOB_OWNERS) ? selected : null;
    }

    public static BlockPos storage(ServerLevel level, UUID resident, BlockPos bed, GlobalPos assigned) {
        BlockPos selected = validStorage(level, assigned) ? assigned.pos() : STORAGE
                .getOrDefault(level.dimension(), Set.of()).stream()
                .filter(pos -> pos.distSqr(bed) <= ASSOCIATION_RADIUS * ASSOCIATION_RADIUS)
                .filter(pos -> available(GlobalPos.of(level.dimension(), pos), resident, STORAGE_OWNERS))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(bed))).orElse(null);
        return claim(level, selected, resident, STORAGE_OWNERS) ? selected : null;
    }

    private static boolean workPoi(net.minecraft.core.Holder<net.minecraft.world.entity.ai.village.poi.PoiType> holder) {
        return !holder.is(PoiTypes.HOME) && !holder.is(PoiTypes.MEETING);
    }

    private static boolean validBed(ServerLevel level, GlobalPos pos) {
        return pos != null && pos.dimension().equals(level.dimension()) && level.hasChunkAt(pos.pos())
                && level.getBlockState(pos.pos()).getBlock() instanceof BedBlock;
    }

    private static BlockPos normalizeBed(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getValue(BedBlock.PART) == BedPart.HEAD ? pos.immutable()
                : pos.relative(state.getValue(BedBlock.FACING)).immutable();
    }

    private static boolean validJob(ServerLevel level, GlobalPos pos) {
        return pos != null && pos.dimension().equals(level.dimension()) && level.hasChunkAt(pos.pos())
                && net.minecraft.world.entity.ai.village.poi.PoiTypes.forState(level.getBlockState(pos.pos()))
                .filter(FamilyHomeIndex::workPoi).isPresent();
    }

    private static boolean validStorage(ServerLevel level, GlobalPos pos) {
        return pos != null && pos.dimension().equals(level.dimension())
                && STORAGE.getOrDefault(level.dimension(), Set.of()).contains(pos.pos());
    }

    private static boolean claim(ServerLevel level, BlockPos pos, UUID resident, Map<GlobalPos, UUID> owners) {
        owners.entrySet().removeIf(entry -> entry.getValue().equals(resident)
                && (pos == null || !entry.getKey().equals(GlobalPos.of(level.dimension(), pos))));
        if (pos == null) return false;
        GlobalPos global = GlobalPos.of(level.dimension(), pos);
        if (!available(global, resident, owners)) return false;
        owners.put(global, resident);
        return true;
    }

    private static boolean available(GlobalPos pos, UUID resident, Map<GlobalPos, UUID> owners) {
        UUID owner = owners.get(pos);
        return owner == null || owner.equals(resident);
    }

    private static boolean isStorage(BlockEntity entity) {
        return entity instanceof Container || entity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    private static void refresh(ServerLevel level, BlockPos pos) {
        Set<BlockPos> index = STORAGE.computeIfAbsent(level.dimension(), ignored -> new HashSet<>());
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null && isStorage(entity)) index.add(pos.immutable());
        else {
            index.remove(pos);
            STORAGE_OWNERS.remove(GlobalPos.of(level.dimension(), pos));
        }
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        chunk.getBlockEntities().keySet().forEach(pos -> refresh(level, pos));
    }

    @SubscribeEvent
    public static void unloaded(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        int x = event.getChunk().getPos().x;
        int z = event.getChunk().getPos().z;
        STORAGE.computeIfAbsent(level.dimension(), ignored -> new HashSet<>())
                .removeIf(pos -> pos.getX() >> 4 == x && pos.getZ() >> 4 == z);
        STORAGE_OWNERS.keySet().removeIf(pos -> pos.dimension().equals(level.dimension())
                && pos.pos().getX() >> 4 == x && pos.pos().getZ() >> 4 == z);
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) refresh(level, event.getPos());
    }

    @SubscribeEvent
    public static void broken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            STORAGE.computeIfAbsent(level.dimension(), ignored -> new HashSet<>()).remove(event.getPos());
            STORAGE_OWNERS.remove(GlobalPos.of(level.dimension(), event.getPos()));
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STORAGE.clear();
        BED_OWNERS.clear();
        JOB_OWNERS.clear();
        STORAGE_OWNERS.clear();
    }
}
