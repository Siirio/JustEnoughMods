package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=net.minecraft.world.entity.LivingEntity.class)
public abstract class BattleFinalQuestRelicEffectMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("RETURN"))
    private void jem$effect(net.minecraft.world.effect.MobEffectInstance effect,net.minecraft.world.entity.Entity source,CallbackInfoReturnable<Boolean> ci){BattleFinalQuestRelics.effect((net.minecraft.world.entity.LivingEntity)(Object)this,effect,ci.getReturnValue());}
}
