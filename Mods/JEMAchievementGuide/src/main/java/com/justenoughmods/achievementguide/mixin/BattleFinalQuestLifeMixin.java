package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.damagesource.DamageSource;
@Mixin(value=net.minecraft.world.entity.LivingEntity.class,priority=500)
public abstract class BattleFinalQuestLifeMixin {
    @Shadow protected boolean dead;
    @Inject(method="hurt",at=@At("RETURN"))private void jem$hurtAfter(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){BattleFinalQuestLife.hurtEnd((LivingEntity)(Object)this,ci.getReturnValue());}
    @Inject(method="die",at=@At("RETURN"))private void jem$deathAfter(DamageSource source,CallbackInfo ci){BattleFinalQuestLife.deathEnd((LivingEntity)(Object)this,dead);}
}
