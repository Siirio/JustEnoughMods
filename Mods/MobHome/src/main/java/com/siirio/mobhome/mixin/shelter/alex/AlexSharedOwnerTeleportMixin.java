package com.siirio.mobhome.mixin.shelter.alex;

import com.siirio.mobhome.feature.shelter.CompanionData;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These exact native goals share the verified TamableAnimal field and private teleport-only method. */
@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.entity.ai.FlyingAIFollowOwner", "com.github.alexthe666.alexsmobs.entity.ai.TameableAIFollowOwnerWater"}, remap = false)
public abstract class AlexSharedOwnerTeleportMixin {
    @Shadow @Final private TamableAnimal tameable;
    @Inject(method = "tryToTeleportNearEntity", at = @At("HEAD"), cancellable = true)
    private void jemcompat$holdOwnerTeleport(CallbackInfo ci) {
        if (tameable.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) ci.cancel();
    }
}
