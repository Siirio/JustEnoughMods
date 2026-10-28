package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraft.world.entity.Entity;import net.minecraft.world.level.block.PowderSnowBlock;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PowderSnowBlock.class,priority=1100) public abstract class WorldBattleArmorQuestSnowBaselineMixin {
 @Inject(method="canEntityWalkOnPowderSnow",at=@At("RETURN")) private static void jem$vanilla(Entity entity,CallbackInfoReturnable<Boolean> ci){WorldBattleArmorQuestMotion.snowBaseline(entity,ci.getReturnValue());}
}