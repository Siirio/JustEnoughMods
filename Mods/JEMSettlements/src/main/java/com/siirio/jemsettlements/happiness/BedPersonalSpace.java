package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Loaded physical beds own spacing; no invented beds, chunk loads or alternative sleep AI. */
public final class BedPersonalSpace {
    public enum Space { CLEAR, CROWDED, UNAVAILABLE, NOT_BED }
    private record Observation(long tick, Space space) { }
    private static final java.util.Map<GlobalPos, Observation> CACHE = new java.util.LinkedHashMap<>();
    private BedPersonalSpace() { }
    public static void clear() { CACHE.clear(); }
    public static void invalidate(ServerLevel level, BlockPos changed) {
        CACHE.keySet().removeIf(key -> key.dimension().equals(level.dimension()) && key.pos().distSqr(changed) <= Math.pow(HousingConditionConfig.BED_RADIUS.get() + 4, 2));
    }
    private static BlockPos head(BlockPos pos, BlockState state) {
        return state.getValue(BedBlock.PART) == BedPart.HEAD ? pos.immutable() : pos.relative(state.getValue(BedBlock.FACING));
    }
    public static Space inspect(ServerLevel level, BlockPos bed) {
        if (!level.hasChunkAt(bed)) return Space.UNAVAILABLE;
        var state = level.getBlockState(bed);
        if (!(state.getBlock() instanceof BedBlock)) return Space.NOT_BED;
        BlockPos head = head(bed, state), foot = head.relative(state.getValue(BedBlock.FACING).getOpposite());
        var key = GlobalPos.of(level.dimension(), head); long now = level.getGameTime();
        var prior = CACHE.get(key);
        if (prior != null && now >= prior.tick() && now - prior.tick() < 20) return prior.space();
        boolean unavailable = false;
        Space result = Space.CLEAR;
        int radius = HousingConditionConfig.BED_RADIUS.get();
        for (BlockPos pos : BlockPos.betweenClosed(
                new BlockPos(Math.min(head.getX(), foot.getX()) - radius, head.getY(), Math.min(head.getZ(), foot.getZ()) - radius),
                new BlockPos(Math.max(head.getX(), foot.getX()) + radius, head.getY(), Math.max(head.getZ(), foot.getZ()) + radius))) {
            if (Math.min(pos.distSqr(head), pos.distSqr(foot)) > radius * radius) continue;
            if (!level.hasChunkAt(pos)) { unavailable = true; continue; }
            var other = level.getBlockState(pos);
            if (!(other.getBlock() instanceof BedBlock) || head(pos, other).equals(head)) continue;
            BlockPos near = pos.distSqr(head) <= pos.distSqr(foot) ? head : foot;
            var relation = ResidentialRooms.sameFloorRoom(level, near, pos);
            if (relation == ResidentialRooms.RoomRelation.SAME) { result = Space.CROWDED; break; }
            if (relation == ResidentialRooms.RoomRelation.PENDING || relation == ResidentialRooms.RoomRelation.UNAVAILABLE) unavailable = true;
        }
        if (result != Space.CROWDED && unavailable) result = Space.UNAVAILABLE;
        if (CACHE.size() >= 4096) CACHE.remove(CACHE.keySet().iterator().next());
        CACHE.put(key, new Observation(now, result)); return result;
    }
    private static ResidentMood history(LivingEntity entity, SettlementSavedData data) {
        if (entity instanceof Villager) return data.resident(entity.getUUID()).filter(ResidentMood::living).orElse(null);
        return data.workforceEntity(entity.getUUID()).filter(row -> row.type() == com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT
                && row.needsEnrolled() && row.settlementActive() && net.minecraftforge.fml.ModList.get().isLoaded("recruits")
                && com.siirio.jemsettlements.compat.recruits.RecruitsAdapter.ownedBy(entity, row.ownerId()))
                .map(row -> row.foodHistory()).orElse(null);
    }
    public static void refresh(LivingEntity entity, ResidentMood resident) {
        if (!(entity.level() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())) return;
        GlobalPos home = entity instanceof Villager villager ? villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null) : null;
        if (home == null) home = resident.observedBed().orElseGet(() -> resident.crowdedHome().orElse(null));
        if (home == null || !home.dimension().equals(level.dimension())) return;
        Space space = inspect(level, home.pos());
        if (space == Space.CROWDED) {
            resident.crowdedHome(home);
            if (entity.isSleeping()) {
                BlockPos sleeping = entity.getSleepingPos().orElse(home.pos());
                entity.stopSleeping();
                refuse(entity, sleeping);
            }
        }
        else if (space == Space.CLEAR) resident.crowdedHome(null);
    }
    public static boolean refuse(LivingEntity entity, BlockPos bed) {
        if (!(entity.level() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())) return false;
        var data = SettlementSavedData.get(level.getServer()); var resident = history(entity, data);
        if (resident == null) return false;
        Space space = inspect(level, bed);
        if (space == Space.NOT_BED) return false;
        // Pending topology cannot grant successful sleep before privacy is known.
        if (space == Space.UNAVAILABLE) return true;
        resident.observedBed(GlobalPos.of(level.dimension(), head(bed, level.getBlockState(bed))));
        if (space != Space.CROWDED) { resident.crowdedHome(null); data.changed(); return false; }
        resident.crowdedHome(GlobalPos.of(level.dimension(), head(bed, level.getBlockState(bed))));
        if (entity instanceof Villager villager) {
            var home = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
            if (home != null && home.dimension().equals(level.dimension()) && level.hasChunkAt(home.pos())
                    && level.getBlockState(home.pos()).getBlock() instanceof BedBlock
                    && head(home.pos(), level.getBlockState(home.pos())).equals(head(bed, level.getBlockState(bed)))) {
                villager.releasePoi(MemoryModuleType.HOME);
                villager.getBrain().eraseMemory(MemoryModuleType.HOME);
            }
        }
        long now = level.getGameTime(), after = entity.getPersistentData().getLong("JemBedSpaceNoticeAfter");
        if (now >= after || after - now > 200) {
            entity.getPersistentData().putLong("JemBedSpaceNoticeAfter", now + 200);
            com.siirio.jemsettlements.network.SettlementNetwork.sendBedSpaceNotice(entity);
        }
        data.changed(); return true;
    }
}
