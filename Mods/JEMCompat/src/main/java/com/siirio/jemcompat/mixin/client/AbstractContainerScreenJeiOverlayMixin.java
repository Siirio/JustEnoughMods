package com.siirio.jemcompat.mixin.client;

import com.siirio.jemcompat.client.jei.CuratedJeiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenJeiOverlayMixin {
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"))
    private void jemcompat$renderCatalogBeforeNativeTooltip(GuiGraphics graphics, int mouseX, int mouseY,
                                                            CallbackInfo callback) {
        CuratedJeiOverlay.renderInsideContainer((AbstractContainerScreen<?>) (Object) this, graphics,
                mouseX, mouseY, Minecraft.getInstance().getFrameTime());
    }
}
