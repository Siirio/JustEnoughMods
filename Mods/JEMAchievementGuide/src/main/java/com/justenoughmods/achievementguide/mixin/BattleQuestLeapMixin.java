package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.skill.SkillContainer;
@Mixin(value = yesman.epicfight.skill.mover.DemolitionLeapSkill.class, remap = false)
public abstract class BattleQuestLeapMixin {
    @Inject(method="startHolding",at=@At("HEAD")) private void jem$start(SkillContainer c,CallbackInfo ci) { BattleQuestEpic.leapStarted(c); }
    @Unique private final ThreadLocal<Float> jem$before= new ThreadLocal<>();
    @Inject(method="holdTick",at=@At("HEAD")) private void jem$before(SkillContainer c,CallbackInfo ci) { jem$before.set(c.getExecutor().getStamina()); }
    @Inject(method="holdTick",at=@At("RETURN")) private void jem$paid(SkillContainer c,CallbackInfo ci) { Float before=jem$before.get(); jem$before.remove(); if(before!=null) BattleQuestEpic.leapPaid(c,before); }
    @Inject(method="onStopHolding",at=@At(value="INVOKE",target="Lyesman/epicfight/network/server/SPSkillExecutionFeedback;getBuffer()Lnet/minecraft/network/FriendlyByteBuf;"))
    private void jem$released(SkillContainer c,yesman.epicfight.network.server.SPSkillExecutionFeedback feedback,CallbackInfo ci) { BattleQuestEpic.leapReleased(c); }
}
