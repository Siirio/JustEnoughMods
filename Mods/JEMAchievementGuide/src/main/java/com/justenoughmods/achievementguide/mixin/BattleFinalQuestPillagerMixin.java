package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;import yesman.epicfight.world.entity.eventlistener.PlayerKilledEvent;
@Mixin(value=yesman.epicfight.skill.passive.StaminaPillagerSkill.class,remap=false)
public abstract class BattleFinalQuestPillagerMixin {
    @Redirect(method="lambda$onInitiate$0",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;setStamina(F)V"))
    private void jem$gain(ServerPlayerPatch patch,float value,PlayerKilledEvent event){float before=patch.getStamina();patch.setStamina(value);BattleFinalQuestPassive.pillager(patch,(yesman.epicfight.skill.Skill)(Object)this,event,before);}
}
