package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.imasillylittleguy.cnc.procedures.RingtailOnEntityTickUpdateProcedure", remap = false)
public abstract class CreatureCareRingtailAssistanceMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean jem$assisted(LivingEntity target, MobEffectInstance effect, LevelAccessor world, double x, double y, double z, Entity entity) {
        MobEffectInstance before = target.getEffect(MobEffects.DIG_SPEED);
        int oldAmplifier = before == null ? -1 : before.getAmplifier(), oldDuration = before == null ? 0 : before.getDuration();
        boolean accepted = target.addEffect(effect); MobEffectInstance after = target.getEffect(MobEffects.DIG_SPEED);
        if (accepted && effect.getEffect() == MobEffects.DIG_SPEED && target instanceof ServerPlayer actor && CreatureCareProof.real(actor) != null
                && entity instanceof TamableAnimal ringtail && CreatureCareProof.owned(ringtail, actor)
                && CreatureCareProof.matches(ringtail, "ringtail_food_owner", actor.getUUID()) && after != null && after.getAmplifier() >= 1
                && (after.getAmplifier() > oldAmplifier || after.getAmplifier() == oldAmplifier && after.getDuration() > oldDuration)) {
            CreatureCareProof.emit(actor, "v3_fauna_ringtail_and_caribou");
        }
        return accepted;
    }
}
