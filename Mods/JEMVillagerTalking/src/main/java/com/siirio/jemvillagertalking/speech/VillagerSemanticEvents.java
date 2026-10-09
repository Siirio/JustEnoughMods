package com.siirio.jemvillagertalking.speech;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class VillagerSemanticEvents {
    private VillagerSemanticEvents() {
    }

    public static boolean local(
            String eventId,
            String occurrenceId,
            ServerLevel level,
            BlockPos origin,
            Entity subject,
            Entity actor,
            String contextState
    ) {
        return VillagerReactionRegistry.emitLocal(eventId, occurrenceId, level, origin, subject, actor, contextState);
    }

    public static boolean world(
            String eventId,
            String occurrenceId,
            MinecraftServer server,
            Entity subject,
            Entity actor,
            String contextState
    ) {
        return VillagerReactionRegistry.emitWorld(eventId, occurrenceId, server, subject, actor, contextState);
    }
}
