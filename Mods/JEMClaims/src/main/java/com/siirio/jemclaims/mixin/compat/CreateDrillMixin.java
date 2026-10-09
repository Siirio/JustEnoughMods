package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity", remap = false)
public abstract class CreateDrillMixin {
    @Shadow protected BlockPos breakingPos;

    @Inject(method = "onBlockBroken", at = @At("HEAD"), cancellable = true)
    private void jemClaims$break(BlockState state, CallbackInfo ci) {
        BlockEntity machine = (BlockEntity) (Object) this;
        if (machine.getLevel() instanceof ServerLevel level
                && !FlanBridge.canAutomate(level, machine.getBlockPos(), breakingPos, ClaimPermission.BREAK)) ci.cancel();
    }
}
