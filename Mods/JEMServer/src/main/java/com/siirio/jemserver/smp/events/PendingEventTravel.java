package com.siirio.jemserver.smp.events;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

record PendingEventTravel(UUID eventId, boolean inside, ServerLevel level, ChunkPos chunk, BlockPos anchor,
                          int playerId, long expiresAtTick) {
    void release() {
        level.getChunkSource().removeRegionTicket(EventTravel.EVENT_TRAVEL_TICKET, chunk, EventTravel.CHUNK_TICKET_RADIUS, playerId);
    }
}
