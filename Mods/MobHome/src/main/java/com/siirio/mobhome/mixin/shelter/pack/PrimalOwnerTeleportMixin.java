package com.siirio.mobhome.mixin.shelter.pack;

import com.siirio.mobhome.feature.shelter.CompanionData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {"org.primal.entity.animal.BearEntity", "org.primal.entity.animal.LionEntity",
        "org.primal.entity.animal.SnakeEntity", "org.primal.entity.animal.EagleEntity"}, remap = false)
public abstract class PrimalOwnerTeleportMixin {
    @Inject(method = "shouldTryTeleportToOwner", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemPauseOnlyOwnerTeleport(CallbackInfoReturnable<Boolean> callback) {
        if (((Entity) (Object) this).getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) callback.setReturnValue(false);
    }
}
