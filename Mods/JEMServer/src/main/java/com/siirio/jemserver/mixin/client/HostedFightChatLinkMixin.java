package com.siirio.jemserver.mixin.client;

import com.siirio.jemserver.smp.events.EventNetwork;
import com.siirio.jemserver.smp.events.EventPromptRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class HostedFightChatLinkMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void jemserver$openHostedFight(Style style, CallbackInfoReturnable<Boolean> callback) {
        var minecraft = Minecraft.getInstance();
        if (style == null || minecraft.getConnection() == null || minecraft.hasSingleplayerServer()) return;
        var request = EventPromptRequest.fromClick(style.getClickEvent());
        if (request == null) return;
        EventNetwork.requestPrompt(request);
        callback.setReturnValue(true);
    }
}
