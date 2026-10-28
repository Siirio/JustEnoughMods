package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.BattleFinalQuestNightfall;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class BattleFinalQuestNightfallRemovedMixin {
    @Inject(method="onEffectRemoved",at=@At("RETURN"))private void jem$removed(MobEffectInstance effect,CallbackInfo ci){BattleFinalQuestNightfall.removed((LivingEntity)(Object)this,effect);}
}
