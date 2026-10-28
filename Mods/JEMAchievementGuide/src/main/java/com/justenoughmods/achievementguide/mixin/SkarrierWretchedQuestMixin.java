package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SkarrierCoatingProof;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.procedures.WretchedFangWeaponEffectProcedure", remap = false)
public abstract class SkarrierWretchedQuestMixin {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;D)V", at = @At("HEAD"))
    private static void jem$beforeNativeFang(Event event, LevelAccessor world, double x, double y, double z,
                                             Entity source, double amount, CallbackInfo callback) {
        boolean actualHit = event instanceof LivingHurtEvent hurt && !hurt.isCanceled()
                && hurt.getSource().getEntity() == source && hurt.getEntity() != source
                && Float.isFinite(hurt.getAmount()) && hurt.getAmount() > 0;
        SkarrierCoatingProof.beginWeapon(actualHit ? source : null, actualHit ? source : null);
    }

    @Redirect(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;D)V", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean jem$nativeRecoveryInstalled(LivingEntity target, MobEffectInstance effect) {
        boolean accepted = target.addEffect(effect);
        SkarrierCoatingProof.installed(target, effect, accepted);
        return accepted;
    }

    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;D)V", at = @At("RETURN"))
    private static void jem$afterNativeFang(Event event, LevelAccessor world, double x, double y, double z,
                                            Entity source, double amount, CallbackInfo callback) {
        SkarrierCoatingProof.endWeapon(source, source);
    }
}
