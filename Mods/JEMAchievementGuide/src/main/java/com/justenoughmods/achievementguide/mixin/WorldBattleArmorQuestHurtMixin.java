package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.WorldBattleArmorQuestDamage;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.damagesource.DamageSource;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LivingEntity.class,priority=500) public abstract class WorldBattleArmorQuestHurtMixin {
 @Inject(method="hurt",at=@At("RETURN")) private void jem$end(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){WorldBattleArmorQuestDamage.scopeEnd((LivingEntity)(Object)this,ci.getReturnValue());}
}
