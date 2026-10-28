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
@Mixin(targets = {"net.mcreator.skarriermobs.procedures.MagmadermisShieldEffectProcedure"}, remap = false)
public abstract class SkarrierMagmaShieldQuestMixin {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private static void jem$begin(Event event, LevelAccessor world, double x, double y, double z, Entity owner, Entity target, CallbackInfo callback) {
        SkarrierWorkshopProof.begin("fire_shield", event == null ? null : owner, target, event);
    }
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private static void jem$end(Event event, LevelAccessor world, double x, double y, double z, Entity owner, Entity target, CallbackInfo callback) { SkarrierWorkshopProof.end(); }
}
