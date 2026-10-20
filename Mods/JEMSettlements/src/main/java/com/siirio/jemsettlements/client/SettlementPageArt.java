package com.siirio.jemsettlements.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Native Aseprite regions, composed into the live layout; no preview data is rendered. */
final class SettlementPageArt {
    private static final ResourceLocation MATERIALS = resource("materials");

    private SettlementPageArt() { }

    static void icon(GuiGraphics g, String name, int x, int y) {
        g.blit(resource("icon-" + name), x, y, 0, 0, 16, 16, 16, 16);
    }

    static void masonry(GuiGraphics g, int x, int y, int w, int h) {
        repeat(g, x, y, w, h, 6, 215, 98, 23);
    }

    static void paper(GuiGraphics g, int x, int y, int w, int h) {
        repeat(g, x, y, w, h, 314, 114, 64, 48);
    }

    static void wood(GuiGraphics g, int x, int y, int w, int h) {
        patch(g, x, y, w, h, 5, 4, 390, 29, 5);
    }

    static void button(GuiGraphics g, int x, int y, int w, int h) {
        patch(g, x, y, w, h, 319, 249, 80, 20, 3);
    }

    /** Fixed native corners and repeating texels keep controls and hit bounds identical. */
    private static void patch(GuiGraphics g, int x, int y, int w, int h,
                              int u, int v, int sw, int sh, int corner) {
        if (w < corner * 2 || h < corner * 2) return;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            int dx = col == 0 ? 0 : col == 1 ? corner : w - corner;
            int dy = row == 0 ? 0 : row == 1 ? corner : h - corner;
            int dw = col == 1 ? w - corner * 2 : corner;
            int dh = row == 1 ? h - corner * 2 : corner;
            int su = u + (col == 0 ? 0 : col == 1 ? corner : sw - corner);
            int sv = v + (row == 0 ? 0 : row == 1 ? corner : sh - corner);
            repeat(g, x + dx, y + dy, dw, dh, su, sv,
                    col == 1 ? sw - corner * 2 : corner,
                    row == 1 ? sh - corner * 2 : corner);
        }
    }

    private static void repeat(GuiGraphics g, int x, int y, int w, int h,
                               int u, int v, int sw, int sh) {
        for (int dy = 0; dy < h; dy += sh) for (int dx = 0; dx < w; dx += sw)
            g.blit(MATERIALS, x + dx, y + dy, u, v,
                    Math.min(sw, w - dx), Math.min(sh, h - dy), 512, 320);
    }

    private static ResourceLocation resource(String name) {
        return new ResourceLocation("jem_settlements", "textures/gui/pages/" + name + ".png");
    }
}
