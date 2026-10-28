package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.BattleFinalQuestMeteor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=yesman.epicfight.skill.identity.MeteorSlamSkill.class,remap=false)
public abstract class BattleFinalQuestMeteorStartMixin {
    @Redirect(method="lambda$onInitiate$4",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;playAnimationSynchronized(Lyesman/epicfight/api/asset/AssetAccessor;F)V"))
    private void jem$nativeStart(PlayerPatch<?> patch,AssetAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> animation,float transition){patch.playAnimationSynchronized(animation,transition);BattleFinalQuestMeteor.started(patch,(yesman.epicfight.skill.Skill)(Object)this,animation);}
}
