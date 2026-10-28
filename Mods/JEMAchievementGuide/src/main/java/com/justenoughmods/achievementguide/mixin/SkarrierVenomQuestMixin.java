package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SkarrierCoatingProof;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"net.mcreator.skarriermobs.procedures.VenomSackWeaponEffectProcedure",
        "net.mcreator.skarriermobs.procedures.SerratedShredWeaponEffectProcedure",
        "net.mcreator.skarriermobs.procedures.BowlderDustWeaponEffectProcedure"}, remap = false)
public abstract class SkarrierVenomQuestMixin {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private static void jem$beforeVenom(Event event, LevelAccessor world, double x, double y, double z,
                                        Entity target, Entity owner, CallbackInfo callback) {
        SkarrierCoatingProof.beginWeapon(target, owner);
    }

    @Redirect(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean jem$venomActuallyInstalled(LivingEntity target, MobEffectInstance effect) {
        boolean accepted = target.addEffect(effect);
        SkarrierCoatingProof.installed(target, effect, accepted);
        return accepted;
    }

    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private static void jem$afterVenom(Event event, LevelAccessor world, double x, double y, double z,
                                       Entity target, Entity owner, CallbackInfo callback) {
        SkarrierCoatingProof.endWeapon(target, owner);
    }
}
