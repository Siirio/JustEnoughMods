package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestDamage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=LivingEntity.class,priority=500)
public abstract class BattleQuestDamageMixin {
    @Inject(method="hurt",at=@At("RETURN")) private void jem$end(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){BattleQuestDamage.end((LivingEntity)(Object)this,ci.getReturnValue());}
}
