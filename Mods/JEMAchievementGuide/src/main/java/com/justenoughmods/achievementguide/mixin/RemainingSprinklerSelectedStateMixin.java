package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeSoilProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class RemainingSprinklerSelectedStateMixin {
    @Inject(method = "getBlockState", at = @At("RETURN"))
    private void jem$nativeSelected(BlockPos pos, CallbackInfoReturnable<BlockState> callback) {
        NativeSoilProof.selected((Level)(Object)this, pos, callback.getReturnValue());
    }
}
