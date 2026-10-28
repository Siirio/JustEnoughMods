package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=com.hm.efn.skill.weapon_innate.ShortSwordInnate.class,remap=false)
public abstract class BattleFinalQuestQueenReplayMixin {
    @Unique private final ThreadLocal<Integer> jem$stack=new ThreadLocal<>();
    @Inject(method="updateContainer",at=@At("HEAD"))private void jem$activationBefore(yesman.epicfight.skill.SkillContainer container,CallbackInfo ci){jem$stack.set(BattleFinalQuestNightfall.replayBefore(container));}
    @Inject(method="updateContainer",at=@At("RETURN"))private void jem$activationAfter(yesman.epicfight.skill.SkillContainer container,CallbackInfo ci){Integer before=jem$stack.get();jem$stack.remove();if(before!=null)BattleFinalQuestNightfall.replayActivated(container,before);}
    @Inject(method="recordDamage",at=@At("HEAD"))private void jem$recordBefore(yesman.epicfight.skill.SkillContainer container,float damage,int time,CallbackInfo ci){BattleFinalQuestNightfall.recordBegin(container);}
    @Inject(method="recordDamage",at=@At("RETURN"))private void jem$recordAfter(yesman.epicfight.skill.SkillContainer container,float damage,int time,CallbackInfo ci){BattleFinalQuestNightfall.recordEnd();}
    @Inject(method="applyReplayBonus",at=@At("HEAD"))private void jem$replayBefore(yesman.epicfight.skill.SkillContainer container,yesman.epicfight.world.entity.eventlistener.DealDamageEvent.Attack event,int time,CallbackInfo ci){BattleFinalQuestNightfall.replayBegin(container,event);}
    @Inject(method="applyReplayBonus",at=@At("RETURN"))private void jem$replayAfter(yesman.epicfight.skill.SkillContainer container,yesman.epicfight.world.entity.eventlistener.DealDamageEvent.Attack event,int time,CallbackInfo ci){BattleFinalQuestNightfall.replayEnd();}
}
