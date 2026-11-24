package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.GougeTravelProof;
import java.util.UUID;
import net.gouge.physics.GripState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GripState.class, remap = false)
public abstract class GougeTravelStateQuestMixin {
    @Inject(method = {"cleanup(Ljava/util/UUID;)V", "clearActive(Ljava/util/UUID;)V"}, at = @At("HEAD"))
    private static void jem$nativeCleanup(UUID actor, CallbackInfo callback) {
        GougeTravelProof.reset(actor);
    }
}
