package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.util.RockyChestplateUtil", remap = false)
public abstract class WildlifeQuestRockyRamMixin {
    @Redirect(method = "tickRockyRolling", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean jem$ram(Entity target, DamageSource source, float damage, LivingEntity roller) {
        float before = target instanceof LivingEntity living ? living.getHealth() : 0;
        boolean accepted = target.hurt(source, damage);
        var actor = roller instanceof Player player ? WildlifeQuestProof.real(player) : null;
        if (accepted && target instanceof LivingEntity living && living.getHealth() < before && WildlifeQuestProof.hostile(living, actor)) WildlifeQuestProof.emit(actor, "v3_fauna_wild_gear");
        return accepted;
    }
}
