package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestRitual;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class WorldRitualQuestEffectMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("RETURN"))
    private void jem$accepted(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> ci){WorldRitualQuestRitual.effect((LivingEntity)(Object)this,effect,ci.getReturnValue());}
}
