package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.BattleFinalQuestNightfall;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.hm.efn.gameasset.animations.EFNLanceAnimations.class,remap=false)
public abstract class BattleFinalQuestDuskResultMixin {
    @Redirect(method={"lambda$build$51","lambda$build$48","lambda$build$37","lambda$build$34","lambda$build$27","lambda$build$16"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean jem$nativeResult(LivingEntity actor,MobEffectInstance effect,LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation,AnimationParameters params){boolean accepted=actor.addEffect(effect);if(patch.getOriginal()==actor)BattleFinalQuestNightfall.duskEffect(actor,effect,accepted,animation);return accepted;}
}
