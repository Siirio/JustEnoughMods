package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.spells.holy.HealSpell;
import io.redspace.ironsspellbooks.spells.holy.GreaterHealSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {HealSpell.class, GreaterHealSpell.class}, remap = false)
public abstract class SpellHealingQuestMixin {
    @Redirect(method = "onCast", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_5634_(F)V"))
    private void jem$committedSelfHeal(LivingEntity target, float amount) {
        float before = target.getHealth();
        target.heal(amount);
        if (target instanceof ServerPlayer actor && Float.isFinite(before) && target.getHealth() > before) {
            QuestActionTrigger.INSTANCE.emit(actor, "spell_effective_player_heal");
        }
    }
}
