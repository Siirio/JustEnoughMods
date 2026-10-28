package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.HealingCircleProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.HealingAoe", remap = false)
public abstract class HealingCircleResultQuestMixin {
    @Redirect(method = "applyEffect", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;m_5634_(F)V"))
    private void jem$nativeEffectiveHeal(LivingEntity recipient, float amount) {
        float before = recipient.getHealth();
        recipient.heal(amount);
        var actor = HealingCircleProof.actor((Entity) (Object) this);
        if (actor != null && Float.isFinite(amount) && amount > 0 && Float.isFinite(before)
                && Float.isFinite(recipient.getHealth()) && recipient.getHealth() > before && recipient.level() == actor.level()) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_magic_healing_blood");
        }
    }
}
