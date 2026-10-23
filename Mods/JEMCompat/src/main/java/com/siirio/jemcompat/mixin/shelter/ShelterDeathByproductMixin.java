package com.siirio.jemcompat.mixin.shelter;

import com.siirio.jemcompat.feature.shelter.ShelterEvents;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native wither-rose placement happens outside LivingDropsEvent and must not become a recovery farm. */
@Mixin(LivingEntity.class)
public abstract class ShelterDeathByproductMixin {
    @Inject(method = "die", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;dead:Z", opcode = org.objectweb.asm.Opcodes.PUTFIELD, shift = At.Shift.BEFORE))
    private void jemcompat$journalConfirmedDeath(net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
        // All LivingDeathEvent cancellation has completed; native inventory/drop handling has not begun.
        ShelterEvents.captureDeath((LivingEntity) (Object) this);
    }
    @Inject(method = "createWitherRose", at = @At("HEAD"), cancellable = true)
    private void jemcompat$suppressRecoveryByproduct(LivingEntity killer, CallbackInfo ci) {
        if (ShelterEvents.capturedDeath((LivingEntity) (Object) this)) ci.cancel();
    }
}
