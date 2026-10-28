package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=com.hm.efn.skill.weapon_innate.MeenLanceInnate.class,remap=false)
public abstract class BattleFinalQuestDuskChargeMixin {
    @Inject(method="validateServerState",at=@At("RETURN"))private void jem$state(yesman.epicfight.skill.SkillContainer container,CallbackInfo ci){BattleFinalQuestNightfall.duskState(container);}
    @Redirect(method="lambda$onInitiate$0",at=@At(value="INVOKE",target="Lyesman/epicfight/skill/Skill;setStackSynchronize(Lyesman/epicfight/skill/SkillContainer;I)V"))
    private void jem$paid(yesman.epicfight.skill.Skill skill,yesman.epicfight.skill.SkillContainer actual,int value,yesman.epicfight.skill.SkillContainer container,yesman.epicfight.world.entity.eventlistener.AttackPhaseEndEvent event){int before=actual.getStack();skill.setStackSynchronize(actual,value);BattleFinalQuestNightfall.duskPaid(actual,event.getAnimation().get(),before);}
}
