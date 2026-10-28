package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.GrapePotReceipt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.satisfy.vinery.core.block.GrapevinePotBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class GrapePotStateInvalidationMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void jem$invalidate(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> callback) {
        BlockState old = callback.getReturnValue();
        // Ordinary block changes bail before any capability lookup, including world generation.
        if (old == null || old == state || (!(old.getBlock() instanceof GrapevinePotBlock)
                && !(state.getBlock() instanceof GrapevinePotBlock))) return;
        GrapePotReceipt.invalidate((LevelChunk)(Object)this, pos);
    }
}
