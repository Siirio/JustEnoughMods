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
        tooltip(g,font,copper,mouseX,mouseY,screenWidth,screenHeight,net.minecraft.network.chat.Component.empty());
    }
    /** Full-size wrapped breakdown; small native money cells never make the decision tooltip unreadable. */
    public static void tooltip(GuiGraphics g, Font font, long copper, int mouseX, int mouseY, int screenWidth, int screenHeight,
                               net.minecraft.network.chat.Component reason) {
        var parts = parts(copper);
        int natural = parts.stream().mapToInt(part -> 20 + font.width(Long.toString(part.count()))).sum();
        int w = Math.min(Math.max(180, natural), Math.max(1, screenWidth - 12));
        int rows=1, dx=0;
        for (var part:parts) {
            int cell=20+font.width(Long.toString(part.count()));
            if(dx>0 && dx+cell>w) { rows++; dx=0; } dx+=cell;
        }
        var reasonLines=font.split(reason,w);
        int textHeight=reason.getString().isEmpty()?0:reasonLines.size()*(font.lineHeight+1)+3;
        int h=rows*18+textHeight;
        int x=Math.max(4,Math.min(mouseX+12,screenWidth-w-4));
        int y=Math.max(4,Math.min(mouseY-12,screenHeight-h-4));
        g.pose().pushPose(); g.pose().translate(0,0,500);
        g.fill(x-3,y-3,x+w+3,y+h+3,0xFF100010);
        g.fill(x-3,y-3,x+w+3,y-2,0xFF7050A0);
        dx=0; int dy=0;
        for (var part:parts) {
            int cell=20+font.width(Long.toString(part.count()));
            if(dx>0 && dx+cell>w) { dy+=18; dx=0; }
            g.renderItem(new ItemStack(CurrencyItems.item(part.denomination())),x+dx,y+dy);
            g.drawString(font,Long.toString(part.count()),x+dx+16,y+dy+4,0xFFFFFFFF,false); dx+=cell;
        }
        if(textHeight>0)for(int i=0;i<reasonLines.size();i++)g.drawString(font,reasonLines.get(i),x,y+rows*18+3+i*(font.lineHeight+1),0xFFFFAAAA,false);
        g.pose().popPose();
    }
}
