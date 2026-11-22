package com.siirio.jemsettlements.happiness;

import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

/** Native POI house clusters replace the former collision flood-fill simulation. */
public final class ResidentialRooms {
    private static final int HOUSE_RADIUS = 8;

    public record Snapshot(int area, Set<BlockPos> floors, boolean complete, boolean unavailable) {
        public Snapshot { floors = Set.copyOf(floors); }
    }
    public enum RoomRelation { SAME, DIFFERENT, PENDING, UNAVAILABLE }

    private ResidentialRooms() { }

    public static Snapshot request(ServerLevel level, BlockPos bed) {
        if (!level.hasChunkAt(bed)) return new Snapshot(0, Set.of(), false, true);
        Set<BlockPos> beds = level.getPoiManager()
                .getInRange(holder -> holder.is(PoiTypes.HOME), bed, HOUSE_RADIUS, PoiManager.Occupancy.ANY)
                .map(record -> record.getPos().immutable())
                .collect(Collectors.toUnmodifiableSet());
        return new Snapshot(beds.size(), beds, true, false);
    }

    public static RoomRelation sameFloorRoom(ServerLevel level, BlockPos first, BlockPos second) {
        if (first.getY() != second.getY()) return RoomRelation.DIFFERENT;
        Snapshot cluster = request(level, first);
        if (cluster.unavailable()) return RoomRelation.UNAVAILABLE;
        return cluster.floors().contains(second) ? RoomRelation.SAME : RoomRelation.DIFFERENT;
    }

    public static void clear() { }
    public static void invalidate(ServerLevel level, BlockPos changed) { }
}
