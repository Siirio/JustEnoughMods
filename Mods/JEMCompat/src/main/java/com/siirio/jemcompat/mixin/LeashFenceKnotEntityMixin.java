package com.siirio.jemcompat.mixin;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotEntityMixin {
    @Inject(method = "survives", at = @At("RETURN"), cancellable = true)
    private void jemcompat$survivesOnWalls(CallbackInfoReturnable<Boolean> callback) {
        LeashFenceKnotEntity knot = (LeashFenceKnotEntity) (Object) this;
        if (!callback.getReturnValueZ() && knot.level().getBlockState(knot.getPos()).is(BlockTags.WALLS)) {
            callback.setReturnValue(true);
        }
    }
}
