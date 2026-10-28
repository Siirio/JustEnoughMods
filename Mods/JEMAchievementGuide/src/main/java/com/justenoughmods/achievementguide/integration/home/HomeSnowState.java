package com.justenoughmods.achievementguide.integration.home;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** The original fence/wall state belongs to Snow Real Magic's native block entity. */
public interface HomeSnowState {
    BlockState getContainedState();
    record Snapshot(Level level, BlockPos pos, BlockState original, int layers, ServerPlayer player) {}
}
