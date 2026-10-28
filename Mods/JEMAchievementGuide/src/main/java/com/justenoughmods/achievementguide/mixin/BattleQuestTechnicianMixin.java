package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.skill.SkillContainer; import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch; import yesman.epicfight.world.entity.eventlistener.DodgeSuccessEvent;
@Mixin(value = yesman.epicfight.skill.passive.TechnicianSkill.class, remap = false)
public abstract class BattleQuestTechnicianMixin {
    @Redirect(method="lambda$onInitiate$0",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;setStamina(F)V"))
    private static void jem$resource(PlayerPatch<?> patch,float value,SkillContainer container,DodgeSuccessEvent event) { float before=patch.getStamina(); patch.setStamina(value); BattleQuestEpic.technician(container,before); }
}
