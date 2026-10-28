package com.justenoughmods.achievementguide.integration.creaturecare;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public final class CreatureCareMusic {
    private CreatureCareMusic() {}
    public static void accepted(Entity snake, ServerLevel level, GameEvent event, Vec3 origin, boolean accepted) {
        var proof = CreatureCareProof.data(snake); BlockPos pos = BlockPos.containing(origin);
        if (event == GameEvent.JUKEBOX_STOP_PLAY && proof.contains("music_pos") && proof.getLong("music_pos") == pos.asLong()) {
            proof.remove("music_pos"); proof.remove("music_until");
        } else if (accepted && (event == GameEvent.JUKEBOX_PLAY || event == GameEvent.NOTE_BLOCK_PLAY)) {
            proof.putLong("music_pos", pos.asLong()); proof.putBoolean("music_record", event == GameEvent.JUKEBOX_PLAY);
            proof.putLong("music_until", level.getGameTime() + 10);
        }
    }
    public static boolean active(Entity snake) {
        if (!(snake.level() instanceof ServerLevel level)) return false;
        var proof = CreatureCareProof.data(snake); if (!proof.contains("music_pos")) return false;
        BlockPos pos = BlockPos.of(proof.getLong("music_pos"));
        if (snake.distanceToSqr(Vec3.atCenterOf(pos)) >= 64) return false;
        if (proof.getBoolean("music_record")) return level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox && jukebox.isRecordPlaying();
        return proof.getLong("music_until") >= level.getGameTime() && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.NOTE_BLOCK);
    }
}
