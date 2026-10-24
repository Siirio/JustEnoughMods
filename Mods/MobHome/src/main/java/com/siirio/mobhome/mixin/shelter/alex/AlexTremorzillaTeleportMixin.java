package com.siirio.mobhome.mixin.shelter.alex;

import com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity;
import com.siirio.mobhome.feature.shelter.CompanionData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.ai.TremorzillaFollowOwnerGoal", remap = false)
public abstract class AlexTremorzillaTeleportMixin {
    @Shadow(remap = false) @Final private TremorzillaEntity tremorzilla;
    @Inject(method = "teleportToOwner", at = @At("HEAD"), cancellable = true)
    private void jemcompat$holdOwnerTeleport(CallbackInfo ci) {
        if (tremorzilla.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) ci.cancel();
    }
}
