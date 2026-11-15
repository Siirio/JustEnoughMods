package com.siirio.jemcompat.compat.audioyt;

import SCP.audioyt_1_20_1.events.JukeboxMonitor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AudioYtJukeboxEvents {
    private AudioYtJukeboxEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        chunk.getBlockEntities().forEach((position, blockEntity) -> {
            if (blockEntity instanceof JukeboxBlockEntity) {
                JukeboxMonitor.forceCheck(level, position);
            }
        });
    }
}
