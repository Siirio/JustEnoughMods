package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemcurrency.CurrencyItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Presentation only: exact copper decomposition, never a financial authority. */
public final class CoinPriceRenderer {
    private CoinPriceRenderer() { }
    public static java.util.List<CoinAmounts.Part> parts(long copper) { return CoinAmounts.parts(copper); }
    /** Fits every denomination; never drops low-value remainder when space runs out. */
    public static void draw(GuiGraphics g, Font font, long copper, int x, int y, int maxWidth, int color) {
        var parts = parts(copper);
        int naturalWidth = parts.stream().mapToInt(part -> 15 + font.width(Long.toString(part.count())) + 3).sum() - 3;
        float scale = Math.min(1f, Math.max(1, maxWidth) / (float) naturalWidth);
        g.pose().pushPose();
        g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        int dx = 0;
        for (CoinAmounts.Part part : parts) {
            g.renderItem(new ItemStack(CurrencyItems.item(part.denomination())), dx, 0);
            String count = Long.toString(part.count());
            g.drawString(font, count, dx + 15, 4, color, false);
            dx += 18 + font.width(count);
        }
        g.pose().popPose();
    }
    public static void tooltip(GuiGraphics g, Font font, long copper, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int natural = parts(copper).stream().mapToInt(part -> 18 + font.width(Long.toString(part.count()))).sum();
        int w = Math.min(Math.max(180, natural), Math.max(1, screenWidth - 12));
        int x = Math.max(4, Math.min(mouseX + 12, screenWidth - w - 4));
        int y = Math.max(4, Math.min(mouseY - 12, screenHeight - 28));
        g.pose().pushPose(); g.pose().translate(0, 0, 500);
        g.fill(x - 3, y - 3, x + w + 3, y + 20, 0xFF100010);
        g.fill(x - 3, y - 3, x + w + 3, y - 2, 0xFF7050A0);
        draw(g, font, copper, x, y, w, 0xFFFFFFFF);
        g.pose().popPose();
    }
}
