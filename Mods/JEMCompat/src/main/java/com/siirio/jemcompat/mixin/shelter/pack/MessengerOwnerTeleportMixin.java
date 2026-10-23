package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.CompanionData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.entities.MessengerEntity", remap = false)
public abstract class MessengerOwnerTeleportMixin {
    @Inject(method = "teleportNearOwner", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemPauseOnlyReturningToOwner(CallbackInfo callback) {
        if (((Entity) (Object) this).getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) callback.cancel();
    }
}
