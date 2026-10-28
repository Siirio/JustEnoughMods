package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.WorldPrehistoricQuestDart;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.damagesource.DamageSource;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LivingEntity.class,priority=500) public abstract class WorldPrehistoricQuestDartDamageMixin {
 @Inject(method="hurt",at=@At("RETURN")) private void jem$after(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){WorldPrehistoricQuestDart.damageScopeEnd((LivingEntity)(Object)this,ci.getReturnValue());}
}