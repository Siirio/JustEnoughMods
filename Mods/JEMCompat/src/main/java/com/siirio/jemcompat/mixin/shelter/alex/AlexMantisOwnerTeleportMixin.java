package com.siirio.jemcompat.mixin.shelter.alex;

import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import com.siirio.jemcompat.feature.shelter.CompanionData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp$FollowOwner", remap = false)
public abstract class AlexMantisOwnerTeleportMixin {
    @Shadow @Final private EntityMantisShrimp tameable;
    @Inject(method = "tryToTeleportNearEntity", at = @At("HEAD"), cancellable = true)
    private void jemcompat$holdOwnerTeleport(CallbackInfo ci) {
        if (tameable.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) ci.cancel();
    }
}
