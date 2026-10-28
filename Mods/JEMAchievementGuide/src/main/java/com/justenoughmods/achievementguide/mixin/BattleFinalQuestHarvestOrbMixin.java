package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.skill.Skill;import yesman.epicfight.skill.SkillContainer;
@Mixin(value=yesman.epicfight.world.entity.DeathHarvestOrb.class,remap=false)
public abstract class BattleFinalQuestHarvestOrbMixin implements BattleFinalQuestPassive.OrbAccess {
    @Shadow private net.minecraft.world.entity.player.Player dest;
    @Override public net.minecraft.world.entity.player.Player jem$harvestDestination(){return dest;}
    @Redirect(method="lambda$tick$0",at=@At(value="INVOKE",target="Lyesman/epicfight/skill/Skill;setConsumptionSynchronize(Lyesman/epicfight/skill/SkillContainer;F)V"))
    private void jem$gain(Skill skill,SkillContainer container,float value){float before=container.getResource();skill.setConsumptionSynchronize(container,value);BattleFinalQuestPassive.collected((net.minecraft.world.entity.Entity)(Object)this,container,before);}
}
