package com.siirio.jemserver.mixin.death;

import com.siirio.jemserver.smp.events.BloodMoonDeaths;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "top.theillusivec4.curios.common.event.CuriosEventHandler", remap = false)
public abstract class SafeCuriosMixin {
    @Inject(method = "playerDrops", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$keepWornSlots(LivingDropsEvent event, CallbackInfo ci) {
        if (event.getEntity() instanceof Player player && BloodMoonDeaths.keeps(player)) ci.cancel();
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "lambda$playerDrops$20",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;m_46207_(Lnet/minecraft/world/level/GameRules$Key;)Z"), remap = false)
    private static boolean jem$ordinaryDrops(net.minecraft.world.level.GameRules rules,
            net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue> key,
            LivingDropsEvent event, net.minecraft.world.entity.LivingEntity entity,
            @org.spongepowered.asm.mixin.injection.Coerce Object handler) {
        return !(entity instanceof Player player && BloodMoonDeaths.ordinaryDeath(player)) && rules.getBoolean(key);
    }
}
