package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.client.SettlementPriceRenderer;
import com.siirio.jemsettlements.client.NobleQuoteClient;
import com.talhanation.recruits.world.RecruitsHireTrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "com.talhanation.recruits.client.gui.NobleTradeScreen$TradeList$TradeEntry")
public abstract class NobleTradeListEntryMixin {
    @Shadow(remap = false) @Final private RecruitsHireTrade trade;
    @Unique private int jem$priceX, jem$priceY;
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$price(GuiGraphics g, ItemStack stack, int x, int y) {
        jem$priceX = x; jem$priceY = y;
        var quote = NobleQuoteClient.current(trade.resourceLocation);
        if (quote == null) g.renderFakeItem(stack, x, y);
        else SettlementPriceRenderer.draw(g, Minecraft.getInstance().font, quote.cost(), x, y, 22, 0xFFFFFFFF, quote.coins());
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$count(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        if (NobleQuoteClient.current(trade.resourceLocation) == null) g.renderItemDecorations(font, stack, x, y);
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void jem$priceTooltip(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        var quote = NobleQuoteClient.current(trade.resourceLocation);
        if (quote != null && mouseX >= jem$priceX && mouseX < jem$priceX + 22 && mouseY >= jem$priceY && mouseY < jem$priceY + 16) {
            NobleQuoteClient.hoverPrice(quote);
        }
    }
}
