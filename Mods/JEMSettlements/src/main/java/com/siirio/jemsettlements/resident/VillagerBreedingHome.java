package com.siirio.jemsettlements.resident;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;

/** A breeding adult needs a loaded 3x3x3 home unit with a bed, job site, and chest. */
public final class VillagerBreedingHome {
    private VillagerBreedingHome() { }

    public static boolean eligiblePair(Villager first, Villager second) {
        if (!(first.level() instanceof ServerLevel level) || second.level() != level
                || !SettlementAuthority.active(level.getServer())) return false;
        return eligible(level, first) && eligible(level, second);
    }

    private static boolean eligible(ServerLevel level, Villager villager) {
        GlobalPos home = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
        if (home == null || !home.dimension().equals(level.dimension())) return false;
        BlockPos center = home.pos();
        boolean bed = false;
        boolean workstation = false;
        boolean chest = false;
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (!level.hasChunkAt(cursor)) return false;
            var state = level.getBlockState(cursor);
            bed |= state.getBlock() instanceof BedBlock;
            chest |= state.getBlock() instanceof ChestBlock;
            workstation |= level.getPoiManager().getType(cursor)
                    .filter(type -> type.is(PoiTypeTags.ACQUIRABLE_JOB_SITE)).isPresent();
        }
        return bed && workstation && chest;
    }
}
