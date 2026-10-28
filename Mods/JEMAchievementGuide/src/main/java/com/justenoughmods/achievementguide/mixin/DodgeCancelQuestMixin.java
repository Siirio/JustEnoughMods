package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.CombatCastCancelProof;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;

@Pseudo
@Mixin(targets = "yesman.epicfight.skill.dodge.DodgeSkill", remap = false)
public abstract class DodgeCancelQuestMixin {
    @Inject(method = "executeOnServer", at = @At("RETURN"))
    private void jem$nativeDodgeCommitted(SkillContainer container, FriendlyByteBuf buffer, CallbackInfo callback) {
        CombatCastCancelProof.dodged(container);
    }
}
