package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.client.CoinPriceRenderer;
import com.siirio.jemeconomy.client.NativeTradeClient;
import com.siirio.jemeconomy.merchant.NativeTradeQuote;
import com.siirio.jemeconomy.physical.CoinFormatter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> implements com.siirio.jemeconomy.client.NativeTradeScreenAccess {
    @Shadow private int shopItem;
    @Shadow int scrollOff;
    @org.spongepowered.asm.mixin.Unique private int jem$revision = -1;
    @org.spongepowered.asm.mixin.Unique private int jem$serverSelection = -1;
    @Override public int jem$selected() { return shopItem; }
    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    private NativeTradeQuote jem$row(int y) { return NativeTradeClient.quote(menu, scrollOff + (y - topPos - 19) / 20); }

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
    private void jem$cost(GuiGraphics graphics, ItemStack cost, ItemStack base, int x, int y, CallbackInfo callback) {
        var quote = jem$row(y);
        if (quote == null || !cost.is(Items.EMERALD)) return;
        int index = quote.offerIndex();
        var offer = menu.getOffers().get(index);
        if (quote.copper() > 0) CoinPriceRenderer.draw(graphics, quote.copper(), x, y + 4,
                offer.getCostB().isEmpty() || offer.getCostB().is(Items.EMERALD) ? 52 : 25);
        else graphics.drawString(font, "?", x + 4, y + 4, 0xAAAAAA, true);
        callback.cancel();
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getCostB()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$secondMoney(MerchantOffer offer) {
        if (NativeTradeClient.snapshot(menu) != null && offer.getCostB().is(Items.EMERALD)) return ItemStack.EMPTY;
        return offer.getCostB();
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getResult()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$payout(MerchantOffer offer) {
        if (NativeTradeClient.snapshot(menu) != null && offer.getResult().is(Items.EMERALD)) return ItemStack.EMPTY;
        return offer.getResult();
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void jem$prices(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        var snapshot = NativeTradeClient.snapshot(menu);
        if (snapshot == null) return;
        for (int row = 0; row < 7; row++) {
            var quote = NativeTradeClient.quote(menu, row + scrollOff);
            if (quote == null || quote.offerIndex() >= menu.getOffers().size()) continue;
            var offer = menu.getOffers().get(quote.offerIndex());
            int y = topPos + 19 + row * 20;
            if (offer.getResult().is(Items.EMERALD)) {
                if (quote.copper() > 0) CoinPriceRenderer.draw(graphics, quote.copper(), leftPos + 73, y + 4, 25);
                else graphics.drawString(font, "?", leftPos + 77, y + 4, 0xAAAAAA, true);
            } else if (!offer.getCostA().is(Items.EMERALD) && offer.getCostB().is(Items.EMERALD)) {
                CoinPriceRenderer.draw(graphics, quote.copper(), leftPos + 40, y + 4, 25);
            }
            if (!quote.available()) graphics.drawString(font, "×", leftPos + 94, y + 4, 0xE07070, true);
            int moneyX = offer.getResult().is(Items.EMERALD) ? 73 : offer.getCostA().is(Items.EMERALD) ? 10 : 40;
            int moneyWidth = moneyX == 10 && (offer.getCostB().isEmpty() || offer.getCostB().is(Items.EMERALD)) ? 52 : 25;
            if (quote.direction() != NativeTradeQuote.Direction.ITEM_BARTER
                    && mouseX >= leftPos + moneyX && mouseX < leftPos + moneyX + moneyWidth && mouseY >= y && mouseY < y + 16) {
                var tooltip = Component.empty().append(CoinFormatter.format(quote.copper()));
                if (!quote.available()) tooltip.append(" — ").append(Component.translatable(quote.reasonKey()));
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }
        var selected = NativeTradeClient.quote(menu, shopItem);
        if (selected != null && selected.copper() > 0)
            CoinPriceRenderer.draw(graphics, selected.copper(), leftPos + 126, topPos + 59, 140);
        String status = !snapshot.status().isEmpty() ? snapshot.status()
                : selected != null && !selected.available() ? selected.reasonKey() : "";
        if (!status.isEmpty()) {
            var text = Component.translatable(status);
            graphics.drawString(font, font.substrByWidth(text, 140).getString(), leftPos + 126, topPos + 20, 0xFFFFFF, true);
        }
        if (mouseX >= leftPos + 126 && mouseX < leftPos + 266 && mouseY >= topPos + 57 && mouseY < topPos + 71)
            graphics.renderTooltip(font, Component.translatable("jem.economy.trade.balance", CoinFormatter.format(snapshot.wallet()),
                    CoinFormatter.format(snapshot.remaining()), snapshot.updateTicks() / 20L), mouseX, mouseY);
    }
}
