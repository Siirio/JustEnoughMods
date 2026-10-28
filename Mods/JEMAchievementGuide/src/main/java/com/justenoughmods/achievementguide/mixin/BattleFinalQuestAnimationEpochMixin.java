package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=yesman.epicfight.api.animation.ServerAnimator.class,remap=false)
public abstract class BattleFinalQuestAnimationEpochMixin {
    @Inject(method="playAnimation(Lyesman/epicfight/api/asset/AssetAccessor;F)V",at=@At("HEAD"))
    private void jem$epoch(yesman.epicfight.api.asset.AssetAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> animation,float transition,CallbackInfo ci){BattleFinalQuestNightfall.animation(((yesman.epicfight.api.animation.ServerAnimator)(Object)this).getEntityPatch(),animation);}
}
