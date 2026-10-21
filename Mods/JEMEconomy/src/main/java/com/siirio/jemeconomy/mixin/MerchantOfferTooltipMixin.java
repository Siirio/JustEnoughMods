package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.client.NativeTradeScreenAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Prevent the original emerald tooltip from being painted underneath the authoritative Coin tooltip. */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.MerchantScreen$TradeOfferButton")
public abstract class MerchantOfferTooltipMixin {
    @Redirect(method = "renderToolTip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$moneyTooltip(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (stack.is(Items.EMERALD) && Minecraft.getInstance().screen instanceof NativeTradeScreenAccess screen && screen.jem$monetaryHovered(x,y)) return;
        graphics.renderTooltip(font,stack,x,y);
    }
}
