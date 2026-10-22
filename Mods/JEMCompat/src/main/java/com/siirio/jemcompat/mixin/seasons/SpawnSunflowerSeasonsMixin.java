package com.siirio.jemcompat.mixin.seasons;

import com.ninni.spawn.server.block.SunflowerBlock;
import com.siirio.jemcompat.feature.seasons.SpawnSunflowerSeasons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SunflowerBlock.class)
public abstract class SpawnSunflowerSeasonsMixin {
    // Spawn 4.0.7 calls this same write from both random and scheduled ticks.
    // The method is a native Spawn name; only the vanilla setBlock target maps.
    @Redirect(method = "tryAdvanceRotation(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    remap = true),
            remap = false, require = 1, allow = 1)
    private boolean jemcompat$seasonalSeedRotation(ServerLevel level, BlockPos pos, BlockState nextState, int flags,
                                                  BlockState previousState, ServerLevel sourceLevel, BlockPos sourcePos) {
        if (!previousState.getValue(SunflowerBlock.SEEDS) && nextState.getValue(SunflowerBlock.SEEDS)) {
            BlockState beforeGrowth = level.getBlockState(pos);
            boolean allowed = SpawnSunflowerSeasons.allowSeedGrowth(level, pos);
            // The native behavior-2 policy may destroy the position. Never write
            // the old flower back over that removal or a replacement block.
            if (level.getBlockState(pos) != beforeGrowth) {
                return false;
            }
            if (!allowed) {
                // Rotation still advances; existing ripe seeds never enter here.
                nextState = nextState.setValue(SunflowerBlock.SEEDS, false);
            }
        }
        return level.setBlock(pos, nextState, flags);
    }

    // The mature flower is deliberately absent from the global crop block tags,
    // so this is the sole fertility check for player, dispenser and direct calls.
    @Inject(method = "performBonemeal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void jemcompat$seasonalSeedBonemeal(ServerLevel level, RandomSource random, BlockPos pos,
                                               BlockState state, CallbackInfo ci) {
        if (!state.getValue(SunflowerBlock.SEEDS)) {
            BlockState beforeGrowth = level.getBlockState(pos);
            boolean allowed = SpawnSunflowerSeasons.allowSeedGrowth(level, pos);
            if (!allowed || level.getBlockState(pos) != beforeGrowth) {
                ci.cancel();
            }
        }
    }
}
