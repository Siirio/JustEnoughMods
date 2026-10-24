package com.siirio.mobhome.mixin.shelter.alex;

import com.github.alexthe666.alexsmobs.entity.EntityWarpedToad;
import com.siirio.mobhome.feature.shelter.CompanionData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityWarpedToad$FollowOwner", remap = false)
public abstract class AlexToadOwnerTeleportMixin {
    @Shadow @Final private EntityWarpedToad tameable;
    @Inject(method = "tryToTeleportNearEntity", at = @At("HEAD"), cancellable = true)
    private void jemcompat$holdOwnerTeleport(CallbackInfo ci) {
        if (tameable.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) ci.cancel();
    }
}
