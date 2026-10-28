package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.skill.SkillContainer;import yesman.epicfight.world.entity.eventlistener.PlayerKilledEvent;
@Mixin(value=yesman.epicfight.skill.passive.DeathHarvestSkill.class,remap=false)
public abstract class BattleFinalQuestHarvestMixin {
    @Inject(method="lambda$onInitiate$0",at=@At("HEAD"))private static void jem$begin(SkillContainer container,PlayerKilledEvent event,CallbackInfo ci){BattleFinalQuestPassive.harvestBegin(container,event);}
    @Inject(method="lambda$onInitiate$0",at=@At("RETURN"))private static void jem$end(SkillContainer container,PlayerKilledEvent event,CallbackInfo ci){BattleFinalQuestPassive.harvestEnd();}
}
