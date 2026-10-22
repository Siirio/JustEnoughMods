package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.IncineratorBudget;
import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.effect.Flame_Strike_Entity", remap = false)
public abstract class CataclysmFlameStrikeMixin {
    @Shadow(remap = false) public abstract float getDamage();
    @Shadow(remap = false) public abstract float getHpDamage();
    @Shadow(remap = false) public abstract LivingEntity getOwner();

    @Redirect(method = "damage(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lcom/github/L_Ender/cataclysm/entity/effect/Flame_Strike_Entity;getDamage()F", ordinal = 1),
            require = 1, expect = 1, remap = false)
    private float jem$bossFlatInput(@Coerce Object zone) {
        LivingEntity owner = getOwner();
        return owner != null && NativeBossDamage.supports(owner)
                ? (float) (getDamage() * NativeBossDamage.fixedFactor(owner)) : getDamage();
    }

    @Redirect(method = "damage(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 2, expect = 2, remap = false)
    private boolean jem$perCastBudget(LivingEntity target, DamageSource source, float nativeAmount) {
        Entity zone = (Entity) (Object) this;
        if (!zone.getPersistentData().hasUUID(IncineratorBudget.CAST)) {
            if (source.getEntity() instanceof LivingEntity owner && NativeBossDamage.supports(owner)) {
                // The native getDamage input was scaled before max-HP addition above.
                return NativeBossDamage.hurt(owner, target, source, nativeAmount);
            }
            return target.hurt(source, nativeAmount);
        }
        return IncineratorBudget.hurt(zone, target, source, getDamage(), 0.01F * getHpDamage());
    }
}
