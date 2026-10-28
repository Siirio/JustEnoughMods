package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=com.hm.efn.gameasset.animations.EFNLanceAnimations.class,remap=false)
public abstract class BattleFinalQuestDuskStageMixin {
    @Inject(method="lambda$build$11",at=@At("RETURN"))
    private static void jem$stage(yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<?> patch,yesman.epicfight.api.asset.AssetAccessor<? extends yesman.epicfight.api.animation.types.DynamicAnimation> animation,yesman.epicfight.api.animation.property.AnimationParameters params,CallbackInfo ci){BattleFinalQuestNightfall.duskStage(patch);}
}
