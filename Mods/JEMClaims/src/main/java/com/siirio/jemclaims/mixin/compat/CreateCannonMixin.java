package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity", remap = false)
public abstract class CreateCannonMixin {
    @Inject(method = "shouldPlace", at = @At("HEAD"), cancellable = true)
    private void jemClaims$print(BlockPos target, BlockState state, BlockEntity targetEntity, BlockState existing,
            BlockState other, boolean check, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity cannon = (BlockEntity) (Object) this;
        if (cannon.getLevel() instanceof ServerLevel level
                && (!FlanBridge.canAutomate(level, cannon.getBlockPos(), target, ClaimPermission.PLACE)
                || !FlanBridge.canAutomate(level, cannon.getBlockPos(), target, ClaimPermission.BREAK))) cir.setReturnValue(false);
    }
}
