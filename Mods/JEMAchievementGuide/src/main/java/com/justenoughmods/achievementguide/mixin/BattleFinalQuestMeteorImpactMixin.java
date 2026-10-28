package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.BattleFinalQuestMeteor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="yesman.epicfight.gameasset.Animations$ReusableSources",remap=false)
public abstract class BattleFinalQuestMeteorImpactMixin {
    @Redirect(method="lambda$static$3",at=@At(value="INVOKE",target="Lyesman/epicfight/api/utils/LevelUtil;circleSlamFracture(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;DZZ)Z"))
    private static boolean jem$impact(LivingEntity actor,Level level,Vec3 center,double radius,boolean hurt,boolean particles,LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation,AnimationParameters params){
        boolean result=yesman.epicfight.api.utils.LevelUtil.circleSlamFracture(actor,level,center,radius,hurt,particles);BattleFinalQuestMeteor.fractured(patch,animation,result);return result;
    }
}
