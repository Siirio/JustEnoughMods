package com.siirio.jemcompat.mixin.shelter.alex;

import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.siirio.jemcompat.feature.shelter.ShelterEvents;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Elephant's trailing inventory loop is outside Forge's captured death-drop collection. */
@Pseudo
@Mixin(value = EntityElephant.class, remap = false)
public abstract class AlexElephantDeathMixin {
    @Group(name = "jemcompat$elephantTrailingDeath", min = 1, max = 1)
    @Inject(method = "m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V", shift = At.Shift.AFTER), cancellable = true, require = 0)
    private void jemcompat$suppressProductionTrailingDeathInventory(DamageSource source, CallbackInfo ci) {
        if (ShelterEvents.capturedDeath((EntityElephant) (Object) this)) ci.cancel();
    }

    @Group(name = "jemcompat$elephantTrailingDeath", min = 1, max = 1)
    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;die(Lnet/minecraft/world/damagesource/DamageSource;)V", shift = At.Shift.AFTER), cancellable = true, require = 0)
    private void jemcompat$suppressMappedTrailingDeathInventory(DamageSource source, CallbackInfo ci) {
        if (ShelterEvents.capturedDeath((EntityElephant) (Object) this)) ci.cancel();
    }
}
