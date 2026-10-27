package com.siirio.jemcinematicinteractions.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "dev.dyslexic.cinematic.client.TableRecipeBook", remap = false)
public abstract class TableRecipeBookMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jemCinematicInteractions$hideRecipePanel(
            GuiGraphics graphics, Font font, float alpha, CallbackInfo callback) {
        callback.cancel();
    }

    @Inject(method = {"over", "click", "scroll", "charTyped", "keyPressed"}, at = @At("HEAD"), cancellable = true)
    private void jemCinematicInteractions$disableRecipePanelInput(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = "tooltip", at = @At("HEAD"), cancellable = true)
    private void jemCinematicInteractions$hideRecipePanelTooltip(
            CallbackInfoReturnable<List<Component>> callback) {
        callback.setReturnValue(null);
    }
}
