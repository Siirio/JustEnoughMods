package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.balance.ProgressionArmor;
import com.siirio.jemworldbosstiers.balance.ProgressionWeapons;
import com.siirio.jemworldbosstiers.compat.irons.SiphonHealing;
import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class ProgressionGuardMixin {
    @Shadow protected abstract void actuallyHurt(DamageSource source, float amount);

    // Both native branches (full hit and iframe damage difference) dispatch virtually,
    // including Player.actuallyHurt. A finally block also releases canceled/failed hits.
    @Redirect(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            require = 2, expect = 2)
    private void jem$guardedDamage(LivingEntity wearer, DamageSource source, float amount) {
        if (!(wearer.level() instanceof ServerLevel level)) {
            actuallyHurt(source, amount);
            return;
        }
        ProgressionArmor.beginHit(wearer, source, amount);
        SiphonHealing.beginHit(wearer, source);
        NativeBossDamage.beginResolution(wearer, source);
        ProgressionWeapons.beginResolution(wearer, source);
        boolean completed = false;
        try {
            actuallyHurt(source, amount);
            completed = true;
        } finally {
            try {
                ProgressionArmor.finishHit(wearer);
            } finally {
                try {
                    SiphonHealing.finishHit(wearer, completed);
                } finally {
                    try {
                        ProgressionWeapons.endResolution();
                    } finally {
                        NativeBossDamage.endResolution();
                    }
                }
            }
        }
    }
}
