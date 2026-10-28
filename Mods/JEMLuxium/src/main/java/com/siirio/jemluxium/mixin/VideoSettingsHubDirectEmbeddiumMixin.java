package com.siirio.jemluxium.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(targets = "com.vinlanx.luxium.client.guiscreen.VideoSettingsHubScreen", remap = false)
public abstract class VideoSettingsHubDirectEmbeddiumMixin extends Screen {
    @Shadow(remap = false) @Final private Supplier<Screen> videoScreenFactory;

    protected VideoSettingsHubDirectEmbeddiumMixin(net.minecraft.network.chat.Component title) {
        super(title);
    }

    @Inject(method = "m_7856_", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemLuxium$openEmbeddiumDirectly(CallbackInfo callbackInfo) {
        minecraft.setScreen(videoScreenFactory.get());
        callbackInfo.cancel();
    }
}
