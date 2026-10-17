package com.siirio.jemprofessions.mixin.sandbox.nativecompat;

import com.siirio.jemprofessions.sandbox.nativecompat.SandboxWarriorEffects;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.dodge.DodgeSkill;

/** Epic Fight 20.14.17: the detachable skill-cast event runs before execution, so it cannot prove a dodge. */
@Mixin(value = DodgeSkill.class, remap = false)
public abstract class EpicDodgeMixin {
    @Inject(method = "executeOnServer", at = @At("RETURN"))
    private void jem$executed(SkillContainer container, FriendlyByteBuf arguments, CallbackInfo callback) {
        SandboxWarriorEffects.dodged(container.getServerExecutor());
    }
}
