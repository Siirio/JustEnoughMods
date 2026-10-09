package com.siirio.jemcompat.mixin.guide;

import net.minecraftforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.unearthedjourney.procedures.SpawnWithJourneyGuideProcedure", remap = false)
public abstract class SpawnWithJourneyGuideProcedureMixin {
    @Inject(method = "onPlayerLoggedIn", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemcompat$disableJourneyGuide(PlayerEvent.PlayerLoggedInEvent event, CallbackInfo callback) {
        callback.cancel();
    }
}
