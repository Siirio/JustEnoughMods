package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=yesman.epicfight.skill.guard.GuardSkill.class,remap=false)
public abstract class BattleFinalQuestNativeGuardBypassMixin {
    @Redirect(method="lambda$onInitiate$16",at=@At(value="INVOKE",target="Lyesman/epicfight/world/damagesource/EpicFightDamageSource;m_269533_(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean jem$guard(yesman.epicfight.world.damagesource.EpicFightDamageSource source,net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> tag,yesman.epicfight.skill.SkillContainer container,yesman.epicfight.world.entity.eventlistener.TakeDamageEvent.Attack event){boolean puncture=source.is(tag);if(puncture&&tag==yesman.epicfight.world.damagesource.EpicFightDamageTypeTags.GUARD_PUNCTURE)BattleFinalQuestNightfall.guard(container,source);return puncture;}
}
