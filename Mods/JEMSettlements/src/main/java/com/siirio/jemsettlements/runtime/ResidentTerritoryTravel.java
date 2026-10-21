package com.siirio.jemsettlements.runtime;

import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/** Navigation adapter; resident membership remains exclusively in SettlementSavedData. */
public final class ResidentTerritoryTravel {
    private ResidentTerritoryTravel() {}
    public static boolean inside(TownRecord town, AABB box) {
        return box.minX >= town.territoryMinX() && box.maxX <= town.territoryMaxX() + 1D
                && box.minZ >= town.territoryMinZ() && box.maxZ <= town.territoryMaxZ() + 1D;
    }
    public static void enrolled(Villager villager, TownRecord town) {
        villager.getPersistentData().putBoolean("JemTownArrived", false);
        advance(villager, town);
    }
    public static void advance(Villager villager, TownRecord town) {
        if (!(villager.level() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())
                || !villager.isAlive() || villager.isSleeping() || villager.isLeashed() || villager.isPassenger()
                || villager.hurtTime > 0 || villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE)) return;
        var state = villager.getPersistentData();
        if (inside(town, villager.getBoundingBox())) {
            state.putBoolean("JemTownArrived", true); state.remove("JemTownPathBlocked"); return;
        }
        long now = level.getGameTime();
        if (now < state.getLong("JemTownNextPath")) return;
        state.putLong("JemTownNextPath", now + 200L);
        double dx = town.center().getX() + .5D - villager.getX(), dz = town.center().getZ() + .5D - villager.getZ();
        double scale = Math.min(1D, 16D / Math.max(1D, Math.sqrt(dx * dx + dz * dz)));
        BlockPos destination = BlockPos.containing(villager.getX() + dx * scale, villager.getY(), villager.getZ() + dz * scale);
        Path path = level.hasChunkAt(destination) ? villager.getNavigation().createPath(destination, 1) : null;
        if (path == null || !path.canReach()) {
            if (!state.getBoolean("JemTownPathBlocked")) for (var player : level.getServer().getPlayerList().getPlayers())
                if (SettlementAuthority.principal(player).filter(town.owner()::equals).isPresent())
                    player.sendSystemMessage(Component.translatable("jem.settlements.resident_path_blocked", villager.getDisplayName()), true);
            state.putBoolean("JemTownPathBlocked", true); state.putLong("JemTownNextPath", now + 600L); return;
        }
        state.putBoolean("JemTownPathBlocked", false);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(destination, .6F, 1));
        villager.getNavigation().moveTo(path, .6D);
    }
    public static boolean permits(Villager villager, Path path) {
        if (!(villager.level() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())
                || path == null || !villager.getPersistentData().getBoolean("JemTownArrived")
                || villager.hurtTime > 0 || villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE)) return true;
        var data = SettlementSavedData.get(level.getServer());
        var resident = data.resident(villager.getUUID());
        var town = resident.flatMap(row -> data.town(row.townId())).orElse(null);
        if (town == null) return true;
        if (!inside(town, villager.getBoundingBox())) return true;
        for (int i = 0; i < path.getNodeCount(); i++) {
            var node = path.getNode(i);
            var box = villager.getBoundingBox().move(node.x + .5D - villager.getX(), node.y - villager.getY(), node.z + .5D - villager.getZ());
            if (!inside(town, box)) return false;
        }
        return true;
    }
}
