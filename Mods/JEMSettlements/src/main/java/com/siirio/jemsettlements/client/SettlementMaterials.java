package com.siirio.jemsettlements.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Native pixel materials shared by settlement controls and the native recruit menu. */
public final class SettlementMaterials {
    private static final ResourceLocation WOOD = new ResourceLocation("minecraft", "textures/block/spruce_planks.png");
    private static final ResourceLocation VINE = new ResourceLocation("minecraft", "textures/block/vine.png");
    private SettlementMaterials() { }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
        g.fill(x, y, x + w, y + h, 0xff151512);
        for (int dy = 2; dy < h - 2; dy += 16) for (int dx = 2; dx < w - 2; dx += 16)
            g.blit(WOOD, x + dx, y + dy, 0, 0, Math.min(16, w - 2 - dx), Math.min(16, h - 2 - dy), 16, 16);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, selected ? 0x282e391c : 0x4820170f);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, selected ? 0xffb4c879 : 0xffad8b58);
        g.fill(x + 1, y + 2, x + 2, y + h - 1, 0xffab8d5b);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xff34291d);
        for (int dx : new int[]{0, w - 7}) for (int dy : new int[]{0, h - 7}) {
            int sx = x + dx, sy = y + dy;
            g.fill(sx + 1, sy + 1, sx + 6, sy + 6, 0xff393b38);
            g.fill(sx + 1, sy + 1, sx + 6, sy + 2, 0xffb6b7a7);
            g.fill(sx + 1, sy + 2, sx + 2, sy + 6, 0xff898d83);
            g.fill(sx + 3, sy + 3, sx + 5, sy + 5, 0xff151b18);
            g.fill(sx + 3, sy + 3, sx + 4, sy + 4, 0xffd0ceba);
        }
    }

    public static void foliage(GuiGraphics g, int x, int y) {
        // Vines are a tinted vanilla block sprite; tint is restored before any text/item draw.
        g.setColor(.36F, .57F, .20F, 1F);
        g.blit(VINE, x, y, 0, 0, 16, 16, 16, 16);
        g.setColor(1F, 1F, 1F, 1F);
    }
}
