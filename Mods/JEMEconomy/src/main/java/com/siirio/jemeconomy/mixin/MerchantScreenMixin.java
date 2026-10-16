package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.client.NativeTradeClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> implements com.siirio.jemeconomy.client.NativeTradeScreenAccess {
    @Shadow private int shopItem;
    @Shadow int scrollOff;
    @org.spongepowered.asm.mixin.Unique private int jem$revision = -1;
    @org.spongepowered.asm.mixin.Unique private int jem$serverSelection = -1;
    @Override public int jem$selected() { return shopItem; }
    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    /** Synchronizes authoritative selection without replacing or decorating the native screen. */
    @Inject(method = "render", at = @At("HEAD"))
    private void jem$selection(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        int count = menu.getOffers().size();
        var snapshot = NativeTradeClient.snapshot(menu);
        if (snapshot != null && snapshot.revision() != jem$revision) {
            if (snapshot.selected() != jem$serverSelection) shopItem = snapshot.selected();
            jem$serverSelection = snapshot.selected();
            jem$revision = snapshot.revision();
        }
        shopItem = Math.max(0, Math.min(shopItem, Math.max(0, count - 1)));
        scrollOff = Math.max(0, Math.min(scrollOff, Math.max(0, count - 7)));
    }

    @Inject(method = "renderAndDecorateCostA", at = @At("HEAD"), cancellable = true)
    private void jem$priceCell(GuiGraphics g, net.minecraft.world.item.ItemStack cost, net.minecraft.world.item.ItemStack base, int x, int y, CallbackInfo ci) {
        var quote = jem$rowQuote(y);
        if (quote != null && quote.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_BUYS
                && cost.is(net.minecraft.world.item.Items.EMERALD)) {
            com.siirio.jemeconomy.client.CoinPriceRenderer.draw(g, font, quote.copper(), x, y, 37, 0xFFFFFFFF);
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$payoutCell(GuiGraphics g, net.minecraft.world.item.ItemStack stack, int x, int y) {
        var quote = jem$rowQuote(y);
        if (quote != null && stack.is(net.minecraft.world.item.Items.EMERALD)
                && (quote.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_SELLS
                || quote.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_BUYS)) {
            if (quote.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_BUYS
                    && menu.getOffers().get(quote.offerIndex()).getCostA().is(net.minecraft.world.item.Items.EMERALD)) return;
            com.siirio.jemeconomy.client.CoinPriceRenderer.draw(g, font, quote.copper(), x, y, 22, 0xFFFFFFFF);
        } else g.renderFakeItem(stack, x, y);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$payoutCount(GuiGraphics g, net.minecraft.client.gui.Font textFont, net.minecraft.world.item.ItemStack stack, int x, int y) {
        var quote = jem$rowQuote(y);
        if (quote == null || (quote.direction() != com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_SELLS
                && quote.direction() != com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_BUYS)
                || !stack.is(net.minecraft.world.item.Items.EMERALD)) g.renderItemDecorations(textFont, stack, x, y);
    }

    @org.spongepowered.asm.mixin.Unique
    private com.siirio.jemeconomy.merchant.NativeTradeQuote jem$rowQuote(int y) {
        int row = (y - topPos - 20) / 20;
        return y < topPos + 20 || row < 0 || row >= 7 ? null : NativeTradeClient.quote(menu, scrollOff + row);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void jem$selectedPrice(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var quote = NativeTradeClient.quote(menu, shopItem);
        if (quote == null || (quote.direction() != com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_BUYS
                && quote.direction() != com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_SELLS)) return;
        g.drawString(font, Component.translatable(quote.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_BUYS
                ? "jem.economy.market.buy" : "jem.economy.market.sell"), leftPos + 108, topPos + 65, 0xFF404040, false);
        com.siirio.jemeconomy.client.CoinPriceRenderer.draw(g, font, quote.copper(), leftPos + 148, topPos + 60, 116, 0xFF404040);
        if (quote.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_SELLS) {
            var slot = menu.getSlot(2);
            g.pose().pushPose(); g.pose().translate(0, 0, 300);
            g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF8B8B8B);
            com.siirio.jemeconomy.client.CoinPriceRenderer.draw(g, font, quote.copper(), leftPos + slot.x, topPos + slot.y, 16, 0xFFFFFFFF);
            g.pose().popPose();
        }
        if (hoveredSlot != null && hoveredSlot.index == 2 && quote.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_SELLS)
            com.siirio.jemeconomy.client.CoinPriceRenderer.tooltip(g, font, quote.copper(), mouseX, mouseY, width, height);
        if (mouseY >= topPos + 20 && mouseY < topPos + 160) {
            int index = scrollOff + (mouseY - topPos - 20) / 20;
            var hovered = NativeTradeClient.quote(menu, index);
            if (hovered != null && ((hovered.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_BUYS && mouseX >= leftPos + 5 && mouseX < leftPos + 59)
                    || (hovered.direction() == com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.PLAYER_SELLS && mouseX >= leftPos + 68 && mouseX < leftPos + 92)))
                com.siirio.jemeconomy.client.CoinPriceRenderer.tooltip(g, font, hovered.copper(), mouseX, mouseY, width, height);
        }
    }
}
