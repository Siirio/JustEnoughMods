package com.siirio.jemsettlements.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Authored dashboard materials. Native recruit screens retain their own material owner. */
public final class SettlementDashboardArt {
    private static final ResourceLocation STONE_FRAME = texture("stone_frame");
    private static final ResourceLocation PAPER_FRAME = texture("paper_frame");
    private static final ResourceLocation DARK_FRAME = texture("dark_frame");
    private static final ResourceLocation WOOD_FRAME = texture("wood_frame");
    private static final ResourceLocation CARD = texture("card");
    private static final ResourceLocation CONTROL = texture("control");
    private static final ResourceLocation HOVER = texture("control_hover");
    private static final ResourceLocation SELECTED = texture("control_selected");
    private static final ResourceLocation DISABLED = texture("control_disabled");
    private static final ResourceLocation PAPER = texture("paper");
    private static final ResourceLocation STONE = texture("stone");
    private static final ResourceLocation WOOD = texture("wood");
    private static final ResourceLocation BANNER = texture("banner");
    private static final ResourceLocation LANTERN = texture("lantern");
    private static final ResourceLocation VINE = texture("vine");
    private static final ResourceLocation HAPPY = texture("mood_happy");
    private static final ResourceLocation NEUTRAL = texture("mood_neutral");
    private static final ResourceLocation SAD = texture("mood_sad");

    private SettlementDashboardArt() { }

    public static void shell(GuiGraphics g, int x, int y, int w, int h, int sidebarWidth) {
        g.fill(x + 3, y + 5, x + w + 3, y + h + 4, 0x90000000);
        SettlementPageArt.masonry(g, x + 4, y + 4, w - 8, h - 8);
        rim(g, STONE_FRAME, x, y, w, h);
        int split = Math.max(8, Math.min(w - 8, sidebarWidth));
        panel(g, x + split, y + 7, w - split - 7, h - 14, false);
        g.fill(x + split - 3, y + 7, x + split - 1, y + h - 7, 0xff191c23);
        g.fill(x + split - 1, y + 7, x + split, y + h - 7, 0xff777987);
        // Ornament occupies the shell foot, outside the sidebar's control area.
        if (h > 90 && split > 44) sprite(g, VINE, x + 5, y + h - 34, 40, 32);
    }

    /** Leave 56 native pixels at each end for the anchored cloth and lamps. */
    public static void header(GuiGraphics g, int x, int y, int w, int h) {
        SettlementPageArt.wood(g, x + 3, y + 3, w - 6, h - 6);
        rim(g, WOOD_FRAME, x, y, w, h);
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
        if (dark) tile(g, STONE, x + 3, y + 3, w - 6, h - 6, 32);
        else SettlementPageArt.paper(g, x + 3, y + 3, w - 6, h - 6);
        rim(g, dark ? DARK_FRAME : PAPER_FRAME, x, y, w, h);
    }

    public static void card(GuiGraphics g, int x, int y, int w, int h, int accent) {
        surface(g, CARD, x, y, w, h);
        if (w > 12 && h > 12) {
            int opaqueAccent = 0xff000000 | (accent & 0xffffff);
            g.fill(x + 5, y + 4, x + w - 5, y + 5, opaqueAccent);
            g.fill(x + 4, y + 5, x + 5, y + 9, opaqueAccent);
            g.fill(x + w - 5, y + 5, x + w - 4, y + 9, opaqueAccent);
        }
    }

    public static void control(GuiGraphics g, int x, int y, int w, int h,
                               boolean selected, boolean hovered, boolean enabled) {
        SettlementPageArt.button(g, x, y, w, h);
        if (hovered && enabled) rim(g, HOVER, x, y, w, h);
        if (!enabled) g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0x7017191b);
        if (selected && enabled && w > 8 && h > 8) {
            rim(g, SELECTED, x, y, w, h);
        }
    }

    /** Values use the canonical 0..100 needs range. Color is a presentation input. */
    public static void track(GuiGraphics g, int x, int y, int w, int h, int value, int color) {
        if (w < 5 || h < 5) return;
        g.fill(x, y, x + w, y + h, 0xff27262b);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xff70747c);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xff3b3e46);
        int filled = (w - 4) * Math.max(0, Math.min(100, value)) / 100;
        if (filled > 0) {
            int rgb = color & 0xffffff;
            g.fill(x + 2, y + 2, x + 2 + filled, y + h - 2, 0xff000000 | rgb);
            g.fill(x + 2, y + 2, x + 2 + filled, y + 3, 0x55ffffff);
        }
        // Segments share one scale regardless of the live value.
        for (int i = 1; i < 5; i++) {
            int marker = x + 2 + (w - 4) * i / 5;
            g.fill(marker, y + 2, marker + 1, y + h - 2, 0x50000000);
        }
    }

    public static void mood(GuiGraphics g, int x, int y, int size, int value) {
        if (size <= 0) return;
        g.blit(value < 35 ? SAD : value < 80 ? NEUTRAL : HAPPY,
                x, y, size, size, 0, 0, 16, 16, 16, 16);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("jem_settlements", "textures/gui/dashboard/" + name + ".png");
    }

    private static void sprite(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        g.blit(texture, x, y, 0, 0, w, h, w, h);
    }

    private static void tile(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, int size) {
        for (int dy = 0; dy < h; dy += size) for (int dx = 0; dx < w; dx += size)
            g.blit(texture, x + dx, y + dy, 0, 0, Math.min(size, w - dx), Math.min(size, h - dy), size, size);
    }

    private static void surface(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        if (w < 10 || h < 10) return;
        repeat(g, texture, x + 5, y + 5, w - 10, h - 10, 5, 5, 14, 14);
        rim(g, texture, x, y, w, h);
    }

    /** Protected 5px corners; edges repeat native texels and never stretch decoration. */
    private static void rim(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        if (w < 10 || h < 10) return;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            if (row == 1 && col == 1) continue;
            int dx = col == 0 ? x : col == 1 ? x + 5 : x + w - 5;
            int dy = row == 0 ? y : row == 1 ? y + 5 : y + h - 5;
            int dw = col == 1 ? w - 10 : 5, dh = row == 1 ? h - 10 : 5;
            repeat(g, texture, dx, dy, dw, dh, col == 0 ? 0 : col == 1 ? 5 : 19,
                    row == 0 ? 0 : row == 1 ? 5 : 19, col == 1 ? 14 : 5, row == 1 ? 14 : 5);
        }
    }

    private static void repeat(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
                               int u, int v, int sourceW, int sourceH) {
        for (int dy = 0; dy < h; dy += sourceH) for (int dx = 0; dx < w; dx += sourceW)
            g.blit(texture, x + dx, y + dy, u, v,
                    Math.min(sourceW, w - dx), Math.min(sourceH, h - dy), 24, 24);
    }
}
