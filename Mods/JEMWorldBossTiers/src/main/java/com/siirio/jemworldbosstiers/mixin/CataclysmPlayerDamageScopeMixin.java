package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Forge 47.4.10 posts onPlayerAttack before Player delegates to LivingEntity.hurt.
@Mixin(value = Player.class, priority = 2000)
public abstract class CataclysmPlayerDamageScopeMixin {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    private void jem$claimInvocation(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        NativeBossDamage.beginPlayerInvocation((LivingEntity) (Object) this, source);
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void jem$releaseInvocation(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        NativeBossDamage.endInvocation();
    }
}
