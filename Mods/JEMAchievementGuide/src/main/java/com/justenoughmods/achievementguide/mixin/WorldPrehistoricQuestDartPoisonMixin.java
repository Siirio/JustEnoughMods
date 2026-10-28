package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="net.mcreator.unearthedjourney.procedures.TippedBlowDartProjectileProjectileHitsLivingEntityProcedure",remap=false)
public abstract class WorldPrehistoricQuestDartPoisonMixin {
 @Redirect(method="execute",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))private static boolean jem$nativePoison(LivingEntity target,MobEffectInstance effect){boolean accepted=target.addEffect(effect);WorldPrehistoricQuestDart.poison(target,effect,accepted);return accepted;}
}
