package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=com.p1nero.invincible.skill.ComboBasicAttack.class,remap=false)
public abstract class BattleFinalQuestNativeComboMixin {
    @Redirect(method="executeNodeOnServer(Lyesman/epicfight/skill/SkillContainer;Lcom/p1nero/invincible/api/skill/ComboNode;IJ)V",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;playAnimationSynchronized(Lyesman/epicfight/api/asset/AssetAccessor;F)V"))
    private void jem$node(yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch<?> patch,yesman.epicfight.api.asset.AssetAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> animation,float transition,yesman.epicfight.skill.SkillContainer container,com.p1nero.invincible.api.skill.ComboNode node,int pressed,long interval){patch.playAnimationSynchronized(animation,transition);BattleFinalQuestNightfall.node(container,animation);}
}
