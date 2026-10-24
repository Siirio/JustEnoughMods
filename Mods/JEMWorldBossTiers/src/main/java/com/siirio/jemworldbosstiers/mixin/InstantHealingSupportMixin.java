package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native instant healing exposes its real owner here, unlike source-less LivingHealEvent. */
@Mixin(MobEffect.class)
public abstract class InstantHealingSupportMixin {
    @Redirect(method = "applyInstantenousEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    private void jemWorldBossTiers$creditActualHealing(LivingEntity healed, float amount, Entity direct, Entity indirect,
                                                       LivingEntity recipient, int amplifier, double proximity) {
        float previous = healed.getHealth();
        healed.heal(amount);
        if (!healed.level().isClientSide)
            HostedEncounterApi.supportHealing(indirect == null ? direct : indirect, healed, Math.max(0, healed.getHealth() - previous));
    }
}
