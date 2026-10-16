package com.siirio.jemcompat.mixin.client;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep B available to gameplay without changing the accessibility settings screen. */
@Mixin(KeyboardHandler.class)
public abstract class NarratorHotkeyMixin {
    @Redirect(method = "keyPress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/GameNarrator;isActive()Z"))
    private boolean jemcompat$disableNarratorShortcut(GameNarrator narrator) {
        return false;
    }
}
