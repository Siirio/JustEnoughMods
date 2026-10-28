package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativePlantProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class RemainingPlantReplacementMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void jem$after(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> callback) {
        BlockState before = callback.getReturnValue();
        if (before != null && ((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level) NativePlantProof.changed(level, pos, before, state);
    }
}
