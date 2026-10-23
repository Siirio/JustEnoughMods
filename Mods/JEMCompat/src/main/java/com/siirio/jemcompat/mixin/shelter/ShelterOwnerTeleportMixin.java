package com.siirio.jemcompat.mixin.shelter;

import com.siirio.jemcompat.feature.shelter.CompanionData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only native owner teleport is paused; native pathfinding/following, sitting and combat remain native. */
@Mixin(FollowOwnerGoal.class)
public abstract class ShelterOwnerTeleportMixin {
    @Shadow @Final private TamableAnimal tamable;
    @Inject(method = "teleportToOwner", at = @At("HEAD"), cancellable = true)
    private void jemcompat$shelterTeleportHold(CallbackInfo ci) {
        if (tamable.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) ci.cancel();
    }
}
