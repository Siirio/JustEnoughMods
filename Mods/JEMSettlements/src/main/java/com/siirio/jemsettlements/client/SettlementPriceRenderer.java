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
    /** Full-size currency cells wrap before the next icon; no denomination is truncated or scaled away. */
    public static int fullHeight(Font font, long amount, int width, boolean coins) {
        if (!coins) return 18;
        int dx=0, rows=1; long remainder=Math.max(0L,amount);
        for (var denomination:com.siirio.jemeconomy.physical.CoinDenomination.descending()) {
            long count=remainder/denomination.value(); remainder%=denomination.value();
            int cell=22+font.width(Long.toString(count));
            if (dx>0 && dx+cell>width) { rows++; dx=0; }
            dx+=cell;
        }
        return rows*18;
    }
    public static void drawFull(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int color, boolean coins) {
        if (!coins) { draw(graphics,font,amount,x,y,width,color,false); return; }
        int dx=0,dy=0; long remainder=Math.max(0L,amount);
        for (var denomination:com.siirio.jemeconomy.physical.CoinDenomination.descending()) {
            long count=remainder/denomination.value(); remainder%=denomination.value();
            int cell=22+font.width(Long.toString(count));
            if (dx>0 && dx+cell>width) { dy+=18; dx=0; }
            graphics.renderItem(new ItemStack(com.siirio.jemcurrency.CurrencyItems.item(denomination)),x+dx,y+dy);
            graphics.drawString(font,Long.toString(count),x+dx+18,y+dy+4,color,false); dx+=cell;
        }
    }
    public static void tooltip(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int height, boolean coins) {
        if (coins) CoinPriceRenderer.tooltip(graphics, font, amount, x, y, width, height);
        else graphics.renderTooltip(font, format(amount, false), x, y);
    }
}
