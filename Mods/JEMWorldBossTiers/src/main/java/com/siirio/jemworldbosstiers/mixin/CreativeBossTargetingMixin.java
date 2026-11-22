package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TargetingConditions.class)
public abstract class CreativeBossTargetingMixin {
    @Redirect(
            method = "test",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z"
            )
    )
    private boolean allowRegisteredBossToTargetCreativePlayer(LivingEntity attacker, LivingEntity target) {
        if (target instanceof Player player
                && player.isCreative()
                && !player.isSpectator()
                && WorldTierApi.profile(attacker).isPresent()) {
            return true;
        }
        return attacker.canAttack(target);
    }
}
