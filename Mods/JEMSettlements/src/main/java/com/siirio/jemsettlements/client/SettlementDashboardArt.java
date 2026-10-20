package com.siirio.jemsettlements.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Crafted dashboard family; native recruit screens retain their own materials. */
public final class SettlementDashboardArt {
    private static final int SHEET = 64, CORNER = 8, CENTER = 48;
    private static final ResourceLocation STONE_FRAME = material("stone_frame");
    private static final ResourceLocation PAPER_FRAME = material("paper_frame");
    private static final ResourceLocation DARK_FRAME = material("dark_frame");
    private static final ResourceLocation WOOD_FRAME = material("wood_frame");
    private static final ResourceLocation CARD = material("card");
    private static final ResourceLocation CONTROL = material("control");
    private static final ResourceLocation HOVER = material("control_hover");
    private static final ResourceLocation SELECTED = material("control_selected");
    private static final ResourceLocation DISABLED = material("control_disabled");
    private static final ResourceLocation BANNER = ornament("banner");
    private static final ResourceLocation LANTERN = ornament("lantern");
    private static final ResourceLocation VINE = ornament("vine");
    private static final ResourceLocation TREASURY = new ResourceLocation("jem_settlements", "textures/gui/crafted/illustrations/treasury.png");

    private SettlementDashboardArt() { }

    public static void shell(GuiGraphics g, int x, int y, int w, int h, int sidebarWidth) {
        g.fill(x + 3, y + 5, x + w + 3, y + h + 4, 0x90000000);
        surface(g, STONE_FRAME, x, y, w, h);
        int split = Math.max(8, Math.min(w - 8, sidebarWidth));
        panel(g, x + split, y + 7, w - split - 7, h - 14, false);
        if (h > 90 && split > 44) sprite(g, VINE, x + 5, y + h - 34, 40, 32);
    }

    /** Text and controls keep the central parchment; cloth and lamps anchor its ends. */
    public static void header(GuiGraphics g, int x, int y, int w, int h) {
        surface(g, WOOD_FRAME, x, y, w, h);
        if (w > 132) panel(g, x + 51, y + 5, w - 102, h - 10, false);
        int ornamentY = y + Math.max(3, (h - 32) / 2);
        sprite(g, BANNER, x + 30, ornamentY, 16, 32);
        sprite(g, BANNER, x + w - 46, ornamentY, 16, 32);
        sprite(g, LANTERN, x + 9, y + Math.max(3, (h - 28) / 2), 16, 28);
        sprite(g, LANTERN, x + w - 25, y + Math.max(3, (h - 28) / 2), 16, 28);
        sprite(g, VINE, x - 5, y - 16, 40, 32);
        sprite(g, VINE, x + w - 33, y - 16, 40, 32);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h, boolean dark) {
        surface(g, dark ? DARK_FRAME : PAPER_FRAME, x, y, w, h);
    }

    public static void card(GuiGraphics g, int x, int y, int w, int h, int accent) {
        surface(g, CARD, x, y, w, h);
        if (w > 18 && h > 16) {
            // Small material inlays identify category; the paper itself stays neutral.
            int opaqueAccent = 0xff000000 | (accent & 0xffffff);
            g.fill(x + 7, y + 5, x + 10, y + 6, opaqueAccent);
            g.fill(x + w - 10, y + 5, x + w - 7, y + 6, opaqueAccent);
        }
    }

    public static void control(GuiGraphics g, int x, int y, int w, int h,
                               boolean selected, boolean hovered, boolean enabled) {
        surface(g, !enabled ? DISABLED : selected ? SELECTED : hovered ? HOVER : CONTROL, x, y, w, h);
    }

    /** Exact native-pixel metal trough and beveled fill; live values remain server-owned. */
    public static void track(GuiGraphics g, int x, int y, int w, int h, int value, int color) {
        if (w < 5 || h < 5) return;
        g.fill(x + 1, y, x + w - 1, y + h, 0xff171b23);
        g.fill(x, y + 1, x + w, y + h - 1, 0xff171b23);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xff777a82);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xff363b45);
        int filled = (w - 4) * Math.max(0, Math.min(100, value)) / 100;
        if (filled > 0) {
            g.fill(x + 2, y + 2, x + 2 + filled, y + h - 2, 0xff000000 | (color & 0xffffff));
            if (h > 5) g.fill(x + 2, y + 2, x + 2 + filled, y + 3, 0x77ffffff);
        }
        for (int i = 1; i < 5; i++) {
            int marker = x + 2 + (w - 4) * i / 5;
            g.fill(marker, y + 2, marker + 1, y + h - 2, 0x70000000);
        }
    }

    public static void mood(GuiGraphics g, int x, int y, int size, int value) {
        icon(g, value < 35 ? "mood_sad" : value < 80 ? "mood_neutral" : "mood_happy", x, y, size);
    }

    public static void icon(GuiGraphics g, String name, int x, int y, int size) {
        if (size <= 0) return;
        g.blit(new ResourceLocation("jem_settlements", "textures/gui/crafted/icons/" + name + ".png"),
                x, y, size, size, 0, 0, 32, 32, 32, 32);
    }

    /** The separate160x128 illustration retains its5:4 aspect within the finance subject area. */
    public static void treasury(GuiGraphics g, int x, int y) {
        g.blit(TREASURY, x, y, 40, 32, 0, 0, 160, 128, 160, 128);
    }

    private static ResourceLocation material(String name) {
        return new ResourceLocation("jem_settlements", "textures/gui/crafted/" + name + ".png");
    }

    private static ResourceLocation ornament(String name) {
        return new ResourceLocation("jem_settlements", "textures/gui/dashboard/" + name + ".png");
    }

    private static void sprite(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        g.blit(texture, x, y, 0, 0, w, h, w, h);
    }

    /** Eight protected native pixels at each corner; center and edges repeat, never stretch. */
    private static void surface(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        int bx = Math.min(CORNER, w / 2), by = Math.min(CORNER, h / 2);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            int dx = col == 0 ? x : col == 1 ? x + bx : x + w - bx;
            int dy = row == 0 ? y : row == 1 ? y + by : y + h - by;
            int dw = col == 1 ? w - 2 * bx : bx, dh = row == 1 ? h - 2 * by : by;
            int u = col == 0 ? 0 : col == 1 ? CORNER : SHEET - bx;
            int v = row == 0 ? 0 : row == 1 ? CORNER : SHEET - by;
            repeat(g, texture, dx, dy, dw, dh, u, v, col == 1 ? CENTER : bx, row == 1 ? CENTER : by);
        }
    }

    private static void repeat(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
                               int u, int v, int sourceW, int sourceH) {
        if (sourceW <= 0 || sourceH <= 0) return;
        for (int dy = 0; dy < h; dy += sourceH) for (int dx = 0; dx < w; dx += sourceW)
            g.blit(texture, x + dx, y + dy, u, v,
                    Math.min(sourceW, w - dx), Math.min(sourceH, h - dy), SHEET, SHEET);
    }
}
