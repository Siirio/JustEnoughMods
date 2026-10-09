package com.siirio.jemcompat.mixin.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "net.mcreator.luminousbeasts.procedures.TreeEntEntityDiesProcedure",
    "net.mcreator.luminousbeasts.procedures.VileGatorEntityDiesProcedure",
    "net.mcreator.luminousbeasts.procedures.HorselessHeadsmandiesProcedure",
    "net.mcreator.luminousbeasts.procedures.WitchDoctorEntityDiesProcedure",
    "net.mcreator.luminousbeasts.procedures.LuminousMothDiesProcedure",
    "net.mcreator.luminousbeasts.procedures.MummyEntityDiesProcedure",
    "net.mcreator.luminousbeasts.procedures.YetiEntityDiesProcedure"
}, remap = false)
public abstract class LuminousEventVictoryMixin {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void jem$excludeEventVictory(Event event, LevelAccessor level, DamageSource source, Entity entity, CallbackInfo callback) {
        if (entity != null && (entity.getPersistentData().getBoolean("jem:event_spawned")
                || "RAID_EVENT".equals(entity.getPersistentData().getCompound("jem_world_boss_tiers").getString("Provenance"))))
            callback.cancel();
    }
}
