package com.siirio.jemsettlements.family;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;

public final class FamilyHomeService {
    public record Status(boolean bed, boolean chest, boolean workstation, boolean light, boolean child, int happiness, String complaint) { public boolean complete() { return bed && chest && light && (child || workstation); } }
    private FamilyHomeService() { }

    public static Status inspect(ServerLevel level, LivingEntity entity, FamilyRecord record, VillagerFamilySavedData data) {
        boolean child = entity.isBaby();
        BlockPos bed = FamilyHomeIndex.bed(level, entity, record.bed());
        if (bed == null) { record.bed(null); record.chest(null); record.workstation(null); record.happiness(55); return new Status(false, false, child, false, child, 55, "home"); }
        record.bed(GlobalPos.of(level.dimension(), bed));
        BlockPos chest = FamilyHomeIndex.storage(level, entity.getUUID(), bed, record.chest());
        BlockPos work = child ? null : FamilyHomeIndex.workstation(level, entity, record.workstation());
        record.chest(chest == null ? null : GlobalPos.of(level.dimension(), chest)); record.workstation(work == null ? null : GlobalPos.of(level.dimension(), work));
        boolean light = false; for (BlockPos pos : zone(bed)) if (level.hasChunkAt(pos) && level.getBrightness(LightLayer.BLOCK, pos) > 0) { light = true; break; }
        int missing = (chest == null ? 1 : 0) + (!child && work == null ? 1 : 0) + (!light ? 1 : 0); int happiness = 100 - missing * 15; record.happiness(happiness);
        String complaint = chest == null ? "chest" : !light ? "light" : !child && work == null ? "workstation" : "";
        return new Status(true, chest != null, child || work != null, light, child, happiness, complaint);
    }
    private static Iterable<BlockPos> zone(BlockPos bed) { return BlockPos.betweenClosed(bed.offset(-1,0,-1), bed.offset(1,2,1)); }

    public static CompoundTag snapshot(ServerLevel level, FamilyRecord record, Status status) {
        CompoundTag result = new CompoundTag(); result.putString("Name", record.name()); result.putInt("Happiness", status.happiness());
        result.putBoolean("HasBed", status.bed()); result.putBoolean("HasChest", status.chest()); result.putBoolean("HasWorkstation", status.workstation()); result.putBoolean("HasLight", status.light()); result.putBoolean("Child", status.child());
        if (record.bed() == null || !record.bed().dimension().equals(level.dimension())) { result.putBoolean("NoHome", true); return result; }
        BlockPos anchor = record.bed().pos(); result.putLong("Anchor", anchor.asLong()); result.putString("Dimension", level.dimension().location().toString()); result.putInt("Radius", 1); result.putInt("VerticalRadius", 2); result.putBoolean("BedPresent", true);
        ListTag palette = new ListTag(), blocks = new ListTag(); java.util.Map<BlockState,Integer> ids = new java.util.LinkedHashMap<>();
        for (BlockPos pos : zone(anchor)) {
            if (!level.hasChunkAt(pos)) { result.putBoolean("Unloaded", true); continue; }
            BlockState state = level.getBlockState(pos); if (state.isAir()) continue;
            int id = ids.computeIfAbsent(state, value -> { palette.add(NbtUtils.writeBlockState(value)); return palette.size()-1; });
            CompoundTag row = new CompoundTag(); row.putLong("Position", pos.asLong()); row.putInt("State", id);
            row.putBoolean("Bed", pos.equals(anchor));
            if (record.chest() != null && record.chest().pos().equals(pos)) { row.putString("Category", "storage"); row.putBoolean("Counted", true); row.putBoolean("Active", true); row.putBoolean("Unique", true); }
            if (record.workstation() != null && record.workstation().pos().equals(pos)) { row.putString("Category", "work"); row.putBoolean("Counted", true); row.putBoolean("Active", true); row.putBoolean("Unique", true); }
            blocks.add(row);
        }
        result.put("Palette", palette); result.put("Blocks", blocks); result.putLong("Captured", level.getGameTime()); return result;
    }
}
