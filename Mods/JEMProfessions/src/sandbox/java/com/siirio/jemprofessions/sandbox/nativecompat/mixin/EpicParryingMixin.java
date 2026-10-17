package com.siirio.jemprofessions.sandbox.nativecompat.mixin;

import com.siirio.jemprofessions.sandbox.nativecompat.SandboxWarriorEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

/** Exact native perfect-block seam; no public post-parry event or cost context exists in 20.14.17. */
@Mixin(value = ParryingSkill.class, remap = false)
public abstract class EpicParryingMixin {
    @Redirect(method = "guard", at = @At(value = "INVOKE", target = "Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;consumeForSkill(Lyesman/epicfight/skill/Skill;Lyesman/epicfight/skill/Skill$Resource;F)Z"))
    private boolean jem$cost(ServerPlayerPatch patch, Skill skill, Skill.Resource resource, float amount,
                             SkillContainer container, CapabilityItem item, TakeDamageEvent.Attack event,
                             float knockback, float impact, boolean advanced) {
        return patch.consumeForSkill(skill, resource, SandboxWarriorEffects.parryCost(patch, event.isParried(), amount));
    }

    @Inject(method = "guard", at = @At("RETURN"))
    private void jem$perfectBlock(SkillContainer container, CapabilityItem item, TakeDamageEvent.Attack event,
                                  float knockback, float impact, boolean advanced, CallbackInfo callback) {
        if (event.isParried()) SandboxWarriorEffects.parried(event.getPlayerPatch());
    }
}
