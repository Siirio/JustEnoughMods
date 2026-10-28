package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import com.github.alexthe666.alexsmobs.entity.EntityCaiman;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.ai.CaimanAIMelee", remap = false)
public abstract class CreatureCareCaimanDefenseMixin {
    @Shadow @Final private EntityCaiman caiman;
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$defense(LivingEntity target, DamageSource source, float amount) {
        float health = target.getHealth(); boolean accepted = target.hurt(source, amount);
        if (caiman.tameAttackFlag) CreatureCareProof.defended(caiman, target, health, accepted, "caiman_hatched");
        return accepted;
    }
}
