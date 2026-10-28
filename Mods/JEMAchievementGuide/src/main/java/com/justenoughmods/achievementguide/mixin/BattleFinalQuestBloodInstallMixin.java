package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.BattleFinalQuestNightfall;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.hm.efn.comboevents.TimeEvents.class,remap=false)
public abstract class BattleFinalQuestBloodInstallMixin {
    @Redirect(method="lambda$applyEffectAtTime$3",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean jem$nativeBlood(LivingEntity actor,MobEffectInstance effect){boolean accepted=actor.addEffect(effect);BattleFinalQuestNightfall.bloodInstalled(actor,effect,accepted);return accepted;}
}
