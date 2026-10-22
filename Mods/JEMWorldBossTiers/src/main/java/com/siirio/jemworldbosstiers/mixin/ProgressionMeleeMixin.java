package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.balance.ProgressionWeapons;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class ProgressionMeleeMixin {
    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1, expect = 1)
    private boolean jem$primaryWeaponHit(Entity target, DamageSource source, float amount) {
        return ProgressionWeapons.nativeMeleeHit(target, source, amount, ((Player) (Object) this).getMainHandItem());
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1, expect = 1)
    private boolean jem$sweepingWeaponHit(LivingEntity target, DamageSource source, float amount) {
        return ProgressionWeapons.nativeMeleeHit(target, source, amount, ((Player) (Object) this).getMainHandItem());
    }
}
