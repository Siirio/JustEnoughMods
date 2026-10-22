package com.siirio.jemserver.client.smp;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Host-projected amounts; client never chooses a payment provider. */
public final class ServerCurrencyPresentation {
    private ServerCurrencyPresentation() { }
    public static void draw(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int color, boolean coins) {
        if (coins) {
            if (net.minecraftforge.fml.ModList.get().isLoaded("jem_economy")) CoinView.draw(graphics, font, amount, x, y, width, color);
            return;
        }
        graphics.renderFakeItem(new ItemStack(Items.EMERALD), x, y);
        graphics.drawString(font, Long.toString(amount), x + 18, y + 4, color, false);
    }
    public static void openMarket(Screen parent) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_economy")) CoinView.open(parent);
    }
    private static final class CoinView {
        static void draw(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int color) { com.siirio.jemeconomy.client.CoinPriceRenderer.draw(graphics, font, amount, x, y, width, color); }
        static void open(Screen parent) { if (com.siirio.jemeconomy.network.EconomyNetwork.supported()) com.siirio.jemeconomy.client.MarketClientState.open(parent); }
    }
}
