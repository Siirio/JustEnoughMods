package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"net.mcreator.skarriermobs.procedures.SerraxeEffectProcedure"}, remap = false)
public abstract class SkarrierSerraxeQuestMixin {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;D)V", at = @At("HEAD"))
    private static void jem$begin(Event event, LevelAccessor world, double x, double y, double z, Entity target, Entity owner, double amount, CallbackInfo callback) {
        SkarrierWorkshopProof.begin("serraxe", event == null ? null : owner, target, event);
    }
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;D)V", at = @At("RETURN"))
    private static void jem$end(Event event, LevelAccessor world, double x, double y, double z, Entity target, Entity owner, double amount, CallbackInfo callback) { SkarrierWorkshopProof.end(); }
    @Redirect(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean jem$effect(LivingEntity target, MobEffectInstance effect) {
        boolean accepted = target.addEffect(effect);
        SkarrierWorkshopProof.effect(target, effect, accepted);
        return accepted;
    }
}
