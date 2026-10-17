package com.siirio.jemcompat.mixin.client;

import com.siirio.jemmenus.client.VanillaGuiPlayers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact installed Quick Skin 2.6.2.4 preview seam, including previews outside a loaded world. */
@Pseudo
@Mixin(targets = "com.quickskin.mod.client.rendering.PlayerModelRenderer", remap = false)
public abstract class QuickSkinVanillaPreviewMixin {
    @Inject(method = "renderPlayerModel(Lnet/minecraft/client/gui/GuiGraphics;IIFFLcom/quickskin/mod/client/rendering/PreviewPlayerData;IIZ)V", at = @At("HEAD"), cancellable = true)
    private static void jem$skinOnly(GuiGraphics graphics, int x, int y, float scale, float rotation,
                                     @Coerce Object preview, int mouseX, int mouseY, boolean equipment, CallbackInfo callback) {
        try {
            ResourceLocation skin = (ResourceLocation) preview.getClass().getMethod("getSkinLocation").invoke(preview);
            String model = (String) preview.getClass().getMethod("getModelType").invoke(preview);
            VanillaGuiPlayers.renderQuickSkin(graphics, x, y, scale, rotation, skin, "slim".equals(model));
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Quick Skin preview API changed", failure); }
        callback.cancel();
    }
}
