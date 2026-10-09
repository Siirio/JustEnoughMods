package com.siirio.jemserver.mixin.death;

import com.siirio.jemserver.smp.events.BloodMoonDeaths;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.jack.gravestones.JacksGravestones$ForgeEvents", remap = false)
public abstract class SafeLegacyGraveMixin {
    @Inject(method = "onLivingDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jem$safeDeath(LivingDeathEvent event, CallbackInfo ci) {
        if (event.getEntity() instanceof Player player && (BloodMoonDeaths.keeps(player) || BloodMoonDeaths.protectsNextDeath(player))) ci.cancel();
    }
}
