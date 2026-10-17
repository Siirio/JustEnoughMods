package com.siirio.jemprofessions.mixin.sandbox.nativecompat;

import com.siirio.jemprofessions.sandbox.nativecompat.SandboxReviveEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.cap.Bleeding;

/** Native 2.0.31 exposes progress read-only; tail runs after native helper-distance/item rules. */
@Mixin(value = Bleeding.class, remap = false)
public abstract class ReviveProgressMixin {
    @Shadow private float progress;
    @Inject(method = "tick", at = @At("TAIL"))
    private void jem$progress(Player player, CallbackInfo callback) {
        progress += SandboxReviveEffects.extraProgress(player, (IBleeding) this);
    }
}
