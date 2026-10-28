package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntitySharkToothArrow", remap = false)
public abstract class WildlifeQuestSharkArrowMixin {
    @Redirect(method = "m_7761_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$aquaticHit(LivingEntity target, DamageSource source, float amount) {
        float before = target.getHealth(); boolean accepted = target.hurt(source, amount);
        var actor = ((AbstractArrow)(Object)this).getOwner() instanceof Player player ? WildlifeQuestProof.real(player) : null;
        if (accepted && target.getHealth() < before && WildlifeQuestProof.hostile(target, actor)) WildlifeQuestProof.emit(actor, "v3_fauna_sea_defense");
        return accepted;
    }
}
