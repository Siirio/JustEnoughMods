package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.BakingStageReceipt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.satisfy.bakery.core.block.cake.BlankCakeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class BakingStageInvalidationMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void jem$replace(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> callback) {
        BlockState old = callback.getReturnValue();
        if (old == null || old == state || (!(old.getBlock() instanceof BlankCakeBlock)
                && !(state.getBlock() instanceof BlankCakeBlock))) return;
        BakingStageReceipt.invalidate((LevelChunk)(Object)this, pos);
    }
}
