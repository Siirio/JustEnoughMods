package com.siirio.jemcompat.mixin.audioyt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "SCP.audioyt_1_20_1.events.JukeboxMonitor", remap = false)
public abstract class AudioYtJukeboxMonitorMixin {
    @Inject(method = "scanNearbyJukeboxes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemcompat$disableNearbyBlockScan(CallbackInfo callback) {
        callback.cancel();
    }
}
