package com.siirio.jemsettlements.client;

import com.siirio.jemeconomy.client.CoinPriceRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Render the server-selected settlement currency inside native currency cells. */
public final class SettlementPriceRenderer {
    private SettlementPriceRenderer() { }
    public static Component format(long amount, boolean coins) {
        return coins ? com.siirio.jemeconomy.physical.CoinFormatter.format(amount)
                : Component.translatable("jem.settlements.currency.emerald_amount", amount);
    }
    public static void draw(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int color, boolean coins) {
        if (coins) { CoinPriceRenderer.draw(graphics, font, amount, x, y, width, color); return; }
        graphics.renderFakeItem(new ItemStack(Items.EMERALD), x, y);
        String count = Long.toString(amount);
        if (width > 30) graphics.drawString(font, count, x + 18, y + 4, color, false);
        else {
            graphics.pose().pushPose(); graphics.pose().translate(x + 15 - font.width(count) * .5F, y + 10, 200);
            graphics.pose().scale(.5F, .5F, 1F); graphics.drawString(font, count, 0, 0, color, true); graphics.pose().popPose();
        }
    }
    public static void tooltip(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int height, boolean coins) {
        if (coins) CoinPriceRenderer.tooltip(graphics, font, amount, x, y, width, height);
        else graphics.renderTooltip(font, format(amount, false), x, y);
    }
}
