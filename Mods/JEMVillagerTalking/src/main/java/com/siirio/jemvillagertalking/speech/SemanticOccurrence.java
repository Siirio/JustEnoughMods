package com.siirio.jemvillagertalking.speech;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public record SemanticOccurrence(
        VillagerSpeechCatalog.EventDefinition event,
        String occurrenceId,
        MinecraftServer server,
        ServerLevel level,
        BlockPos origin,
        Entity subject,
        Entity actor,
        String contextState
) {
    public static SemanticOccurrence local(
            VillagerSpeechCatalog.EventDefinition event,
            String occurrenceId,
            ServerLevel level,
            BlockPos origin,
            Entity subject,
            Entity actor,
            String contextState
    ) {
        return new SemanticOccurrence(event, occurrenceId, level.getServer(), level, origin, subject, actor, contextState);
    }

    public static SemanticOccurrence world(
            VillagerSpeechCatalog.EventDefinition event,
            String occurrenceId,
            MinecraftServer server,
            Entity subject,
            Entity actor,
            String contextState
    ) {
        return new SemanticOccurrence(event, occurrenceId, server, null, null, subject, actor, contextState);
    }
}
