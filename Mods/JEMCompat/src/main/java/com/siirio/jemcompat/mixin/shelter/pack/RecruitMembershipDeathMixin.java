package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.ShelterEvents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.entities.AbstractRecruitEntity", remap = false)
public abstract class RecruitMembershipDeathMixin {
    @Inject(method = {"die", "m_6667_"}, at = {
            @At(value = "INVOKE", target = "Lcom/talhanation/recruits/entities/AbstractInventoryEntity;m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V", shift = At.Shift.AFTER, remap = false),
            @At(value = "INVOKE", target = "Lcom/talhanation/recruits/entities/AbstractInventoryEntity;die(Lnet/minecraft/world/damagesource/DamageSource;)V", shift = At.Shift.AFTER, remap = false)}, cancellable = true, remap = false)
    private void jemRetainStableOwnedMembership(CallbackInfo callback) {
        // A pending recoverable recruit still owns its unit slot, faction and group under the same UUID.
        if (ShelterEvents.capturedDeath((Entity) (Object) this)) callback.cancel();
    }
}
