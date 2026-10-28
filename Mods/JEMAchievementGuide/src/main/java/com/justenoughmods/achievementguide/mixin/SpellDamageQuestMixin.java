package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DamageSources.class, remap = false)
public abstract class SpellDamageQuestMixin {
    @Redirect(method = "applyDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean jem$committedSpellDamage(LivingEntity target, DamageSource source, float amount) {
        float before = target.getHealth() + target.getAbsorptionAmount();
        boolean accepted = target.hurt(source, amount);
        float after = target.getHealth() + target.getAbsorptionAmount();
        if (accepted && Float.isFinite(amount) && amount > 0 && Float.isFinite(before) && Float.isFinite(after)
                && after < before && target instanceof Enemy && source instanceof SpellDamageSource
                && source.getEntity() instanceof ServerPlayer actor) {
            QuestActionTrigger.INSTANCE.emit(actor, "spell_positive_hostile_damage");
        }
        return accepted;
    }
}
