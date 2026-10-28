package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.CompanionQuestSpell;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.damagesource.DamageSource;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LivingEntity.class,priority=500) public abstract class CompanionQuestSpellDamageMixin {
 @Inject(method="hurt",at=@At("RETURN")) private void jem$after(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){CompanionQuestSpell.damageEnd((LivingEntity)(Object)this,ci.getReturnValue());}
}
