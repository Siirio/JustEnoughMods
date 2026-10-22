package com.siirio.jemvisualsandbox.mixin;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reserve physical B/И for gameplay; narrator settings remain explicitly accessible. */
@Mixin(KeyboardHandler.class)
public abstract class RaceNarratorHotkeyMixin {
    // In1.20.1 this single availability check gates only vanilla's Ctrl+B toggle.
    // Skipping that branch lets the original key continue to screens/key mappings.
    @Redirect(method="keyPress",at=@At(value="INVOKE",target="Lnet/minecraft/client/GameNarrator;isActive()Z"))
    private boolean races$disableNarratorChord(GameNarrator narrator){return false;}
}
