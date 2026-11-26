package com.siirio.jemperformance.mixin;

import com.mapsyncer.platform.PlatformManager;
import com.mapsyncer.platform.UpdateMode;
import com.mapsyncer.server.IncrementalUpdateHandlerLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps MapSyncer's TICK client policy while replacing its forced-save server timer. */
@Mixin(value = IncrementalUpdateHandlerLogic.class, remap = false)
public abstract class MapSyncerTickMixin {
    @Inject(method = "onServerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemperformance$replaceForcedSaveTimer(CallbackInfo callback) {
        if (PlatformManager.getPlatform().getIncrementalUpdateMode() == UpdateMode.TICK) {
            callback.cancel();
        }
    }
}
