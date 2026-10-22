package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Ignis_Fireball_Entity", remap = false)
public abstract class CataclysmIgnisFireballDamageMixin {
    @ModifyConstant(method = "m_5790_", constant = @Constant(floatValue = 6.0F),
            require = 1, expect = 1, remap = false)
    private float jem$fixed6(float nativeFlat) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        return owner instanceof LivingEntity boss ? (float) (nativeFlat * NativeBossDamage.fixedFactor(boss)) : nativeFlat;
    }

    @ModifyConstant(method = "m_5790_", constant = @Constant(floatValue = 8.0F),
            require = 1, expect = 1, remap = false)
    private float jem$fixed8(float nativeFlat) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        return owner instanceof LivingEntity boss ? (float) (nativeFlat * NativeBossDamage.fixedFactor(boss)) : nativeFlat;
    }

    @Redirect(method = "m_5790_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 3, expect = 3, remap = false)
    private boolean jem$composedHit(Entity target, DamageSource source, float amount) {
        if (target instanceof LivingEntity living && source.getEntity() instanceof LivingEntity boss)
            return NativeBossDamage.hurt(boss, living, source, amount);
        return target.hurt(source, amount);
    }
}

