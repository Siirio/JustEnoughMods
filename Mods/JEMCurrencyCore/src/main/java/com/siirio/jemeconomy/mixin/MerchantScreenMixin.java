package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.client.CoinPriceRenderer;
import com.siirio.jemeconomy.client.NativeTradeClient;
import com.siirio.jemeconomy.client.NativeTradeScreenAccess;
import com.siirio.jemeconomy.merchant.NativeTradeQuote;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemcurrency.CurrencyItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Monetary cells only: native offers, geometry, buttons, goods, slots and restocking retain ownership. */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> implements NativeTradeScreenAccess {
    @Shadow private int shopItem;
    @Shadow int scrollOff;
    @Unique private int jem$revision = -1;
    @Unique private int jem$serverSelection = -1;
    @Override public int jem$selected() { return shopItem; }
    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

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
    private void jem$priceCell(GuiGraphics g, ItemStack cost, ItemStack base, int x, int y, CallbackInfo ci) {
        var quote = jem$rowQuote(y);
        if (quote == null || !cost.is(Items.EMERALD)) return;
        var offer = menu.getOffers().get(quote.offerIndex());
        // Native A starts at10, B at40 and the arrow at60. Never draw over an actual second item.
        int cellWidth = !offer.getCostB().isEmpty() && !offer.getCostB().is(Items.EMERALD) ? 24 : 48;
        jem$drawMoney(g, quote, x, y, cellWidth);
        ci.cancel();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$otherMoneyCell(GuiGraphics g, ItemStack stack, int x, int y) {
        var quote = jem$rowQuote(y);
        if (quote == null || !stack.is(Items.EMERALD)) { g.renderFakeItem(stack, x, y); return; }
        var offer = menu.getOffers().get(quote.offerIndex());
        if (x == leftPos + 40) {
            // Two emerald legs are one authoritative total, displayed once in A.
            if (!offer.getCostA().is(Items.EMERALD)) jem$drawMoney(g, quote, x, y, 18);
        } else if (x == leftPos + 73) jem$drawMoney(g, quote, x, y, 18);
        else g.renderFakeItem(stack, x, y);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$moneyCount(GuiGraphics g, net.minecraft.client.gui.Font textFont, ItemStack stack, int x, int y) {
        if (jem$rowQuote(y) == null || !stack.is(Items.EMERALD) || x != leftPos + 40 && x != leftPos + 73)
            g.renderItemDecorations(textFont, stack, x, y);
    }

    @Unique
    private void jem$drawMoney(GuiGraphics g, NativeTradeQuote quote, int x, int y, int cellWidth) {
        if (jem$priced(quote)) CoinPriceRenderer.draw(g, font, quote.copper(), x, y, cellWidth, quote.available() ? 0xFFFFFFFF : 0xFFFF9999);
        else g.renderFakeItem(new ItemStack(Items.BARRIER), x, y);
    }
    @Unique
    private static boolean jem$priced(NativeTradeQuote quote) {
        return quote.direction() == NativeTradeQuote.Direction.PLAYER_BUYS || quote.direction() == NativeTradeQuote.Direction.PLAYER_SELLS;
    }
    @Unique
    private NativeTradeQuote jem$rowQuote(int y) {
        // Forge47.4.10 renders the first item at top+19 (17+2), then every20px.
        int local = y - topPos - 19;
        if (local < 0 || local >= 140 || local % 20 != 0) return null;
        var quote = NativeTradeClient.quote(menu, scrollOff + local / 20);
        return quote == null || quote.offerIndex() >= menu.getOffers().size() ? null : quote;
    }
    @Unique
    private NativeTradeQuote jem$hoverQuote(int mouseX, int mouseY) {
        int row = mouseY - topPos - 18;
        if (row < 0 || row >= 140) return null;
        var quote = NativeTradeClient.quote(menu, scrollOff + row / 20);
        if (quote == null || quote.offerIndex() >= menu.getOffers().size()) return null;
        var offer = menu.getOffers().get(quote.offerIndex()); int x = mouseX - leftPos;
        if (x >= 5 && x < 35 && offer.getCostA().is(Items.EMERALD)
                || x >= 35 && x < 59 && offer.getCostB().is(Items.EMERALD)
                || x >= 70 && x < 93 && offer.getResult().is(Items.EMERALD)) return quote;
        return null;
    }
    @Override public boolean jem$monetaryHovered(int mouseX, int mouseY) { return jem$hoverQuote(mouseX, mouseY) != null; }

    /** The inherited native tooltip remains intact for actual merchandise and barter. */
    @Override protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        var quote = NativeTradeClient.quote(menu, shopItem);
        if (quote != null && jem$priced(quote) && menu.getCarried().isEmpty() && hoveredSlot != null) {
            var offer = shopItem < menu.getOffers().size() ? menu.getOffers().get(shopItem) : null;
            boolean money = quote.direction() == NativeTradeQuote.Direction.PLAYER_SELLS && hoveredSlot.index == 2
                    || quote.direction() == NativeTradeQuote.Direction.PLAYER_BUYS && offer != null
                    && (hoveredSlot.index == 0 && offer.getCostA().is(Items.EMERALD)
                    || hoveredSlot.index == 1 && offer.getCostB().is(Items.EMERALD)) && !hoveredSlot.hasItem();
            if (money) { jem$tooltip(g, quote, mouseX, mouseY); return; }
        }
        super.renderTooltip(g, mouseX, mouseY);
    }
    @Unique
    private void jem$tooltip(GuiGraphics g, NativeTradeQuote quote, int mouseX, int mouseY) {
        if (jem$priced(quote)) CoinPriceRenderer.tooltip(g, font, quote.copper(), mouseX, mouseY, width, height,
                quote.available() ? Component.empty() : Component.translatable(quote.reasonKey()));
        else g.renderTooltip(font, Component.translatable(quote.reasonKey()), mouseX, mouseY);
    }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantMenu;getOffers()Lnet/minecraft/world/item/trading/MerchantOffers;", ordinal = 0))
    private void jem$selectedPrice(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var quote = NativeTradeClient.quote(menu, shopItem);
        if (quote != null && jem$priced(quote) && shopItem < menu.getOffers().size()) {
            Component label = Component.translatable(quote.direction() == NativeTradeQuote.Direction.PLAYER_BUYS ? "jem.economy.market.buy" : "jem.economy.market.sell");
            int priceX = leftPos + 108 + font.width(label) + 6;
            g.drawString(font, label, leftPos + 108, topPos + 63, 0xFF404040, false);
            CoinPriceRenderer.draw(g, font, quote.copper(), priceX, topPos + 59, Math.max(1, leftPos + 268 - priceX), 0xFF404040);
            if (quote.direction() == NativeTradeQuote.Direction.PLAYER_BUYS) {
                var offer = menu.getOffers().get(shopItem);
                for (int index = 0; index < 2; index++) {
                    var cost = index == 0 ? offer.getCostA() : offer.getCostB();
                    if (!cost.is(Items.EMERALD) || index == 1 && offer.getCostA().is(Items.EMERALD)) continue;
                    var slot = menu.getSlot(index);
                    if (!slot.hasItem()) g.renderFakeItem(new ItemStack(CurrencyItems.item(jem$face(quote.copper()))), leftPos + slot.x, topPos + slot.y);
                }
            }
        }
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void jem$priceTooltips(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var quote = NativeTradeClient.quote(menu,shopItem);
        if (quote!=null && jem$priced(quote) && mouseX >= leftPos + 108 && mouseX < leftPos + 268 && mouseY >= topPos + 59 && mouseY < topPos + 75)
            jem$tooltip(g, quote, mouseX, mouseY);
        // Native TradeOfferButton tooltips are suppressed only for these monetary hit regions.
        var hovered = jem$hoverQuote(mouseX, mouseY);
        if (hovered != null) jem$tooltip(g, hovered, mouseX, mouseY);
    }
    @Unique
    private static CoinDenomination jem$face(long amount) {
        for (var coin : CoinDenomination.descending()) if (amount >= coin.value()) return coin;
        return CoinDenomination.COPPER;
    }
}
