package com.siirio.jemluxium.mixin;

import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

@Mixin(value = OptionsScreen.class, priority = 900)
public abstract class OptionsScreenDirectEmbeddiumMixin {
    @ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/OptionsScreen;openScreenButton(Lnet/minecraft/network/chat/Component;Ljava/util/function/Supplier;)Lnet/minecraft/client/gui/components/Button;",
                    ordinal = 2
            ),
            index = 1
    )
    private Supplier<Screen> jemLuxium$openEmbeddiumDirectly(Supplier<Screen> ignoredSupplier) {
        return () -> new SodiumOptionsGUI((Screen) (Object) this);
    }
}
