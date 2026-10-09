package com.siirio.jemcompat.mixin;

import com.siirio.jemcompat.feature.create.DrillWear;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public abstract class BlockBreakingMovementBehaviourMixin {
    @Inject(method = "visitNewPosition", at = @At("HEAD"), cancellable = true)
    private void jemcompat$breakOnBedrock(MovementContext context, BlockPos pos, CallbackInfo callback) {
        BlockState state = context.world.getBlockState(pos);
        if (DrillWear.destroyForBedrock(context, state)) {
            callback.cancel();
        }
    }

    @Inject(method = "onBlockBroken", at = @At("TAIL"))
    private void jemcompat$recordWear(MovementContext context, BlockPos pos, BlockState state, CallbackInfo callback) {
        DrillWear.recordBreak(context, state);
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void jemcompat$adjustSpeed(MovementContext context, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(DrillWear.adjustSpeed(context, callback.getReturnValueF()));
    }
}
