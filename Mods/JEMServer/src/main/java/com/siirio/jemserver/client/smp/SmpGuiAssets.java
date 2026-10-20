package com.siirio.jemserver.client.smp;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import static com.siirio.jemserver.client.ui.JemPalette.*;

/** Authored materials retain their native pixel-sized corners and repeated edges. */
public final class SmpGuiAssets {
    public static final int INK = 0xFF33291F;
    public static final int PAPER_MUTED = 0xFF695C50;
    private static final String ROOT = "textures/gui/crafted/";
    private SmpGuiAssets() {}

    private static ResourceLocation texture(String path) {
        return new ResourceLocation("jem_server", ROOT + path + ".png");
    }

    public static int accent(String activity) {
        return switch (activity) {
            case "BLOOD_MOON" -> 0xFFB64E4E;
            case "FISHING" -> 0xFF67A9C4;
            case "COOKING_SHOW" -> 0xFFD99A58;
            case "RESOURCE_RUSH" -> 0xFF84B46A;
            case "BOSS", "BOSS_RAID" -> 0xFFC87A58;
            default -> COPPER;
        };
    }

    public static void material(GuiGraphics g, String name, int x, int y, int w, int h) {
        nineSlice(g, texture(name), x, y, w, h, true);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        material(g, fill == HOVER ? "control_hover" : fill == ROW ? "control" : "dark_frame", x, y, w, h);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int accent) {
        nineSlice(g, texture("wood_frame"), x, y, w, h, false);
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h, String role, boolean hovered, boolean disabled) {
        material(g, disabled ? "control_disabled" : hovered ? "control_hover" : role.equals("primary") || role.equals("confirm") ? "control_selected" : "control", x, y, w, h);
    }

    public static void ornament(GuiGraphics g, String name, int x, int y) {
        int w = name.equals("vine") ? 40 : 16;
        int h = name.equals("lantern") ? 28 : 32;
        g.blit(texture("ornaments/" + name), x, y, 0, 0, w, h, w, h);
    }

    public static boolean icon(GuiGraphics g, String key, int x, int y, int size) {
        String icon = switch (key) {
            case "profile", "profiles", "seller_profile" -> "profile";
            case "players", "parties", "events", "market", "settlements", "roles", "professions", "back", "calendar", "search", "map" -> key;
            case "shops" -> "market";
            case "prizes", "claim_rewards", "claim", "claim_all", "all_rewards" -> "rewards";
            case "BOSS", "BOSS_RAID" -> "boss";
            case "BLOOD_MOON" -> "blood_moon";
            case "RESOURCE_RUSH" -> "resource_rush";
            default -> null;
        };
        if (icon == null) return false;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 32f, size / 32f, 1);
        g.blit(texture("icons/" + icon), 0, 0, 0, 0, 32, 32, 32, 32);
        g.pose().popPose();
        return true;
    }

    public static void cover(GuiGraphics g, String activity, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        String cover = switch (activity) {
            case "BLOOD_MOON" -> "blood_moon";
            case "BOSS", "BOSS_RAID" -> "boss_raid";
            case "RESOURCE_RUSH" -> "resource_rush";
            case "FISHING" -> "fishing";
            case "COOKING_SHOW" -> "cooking_show";
            default -> "seasonal";
        };
        // Crop the source to the destination aspect, rather than deforming the illustration.
        int sourceW = Math.min(384, Math.max(1, Math.round(192f * w / h)));
        int sourceH = Math.min(192, Math.max(1, Math.round(384f * h / w)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale((float) w / sourceW, (float) h / sourceH, 1);
        g.blit(texture("covers/" + cover), 0, 0, (384 - sourceW) / 2, (192 - sourceH) / 2, sourceW, sourceH, 384, 192);
        g.pose().popPose();
    }

    public static void coverBackground(GuiGraphics g, String activity, int x, int y, int w, int h) {
        cover(g, activity, x, y, w, h);
        g.fill(x, y, x + w, y + h, 0x770E1420);
    }

    private static void nineSlice(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, boolean center) {
        if (w <= 0 || h <= 0) return;
        int edge = Math.min(8, Math.min(w / 2, h / 2));
        if (edge == 0) return;
        int[] lengthsX = {edge, w - edge * 2, edge};
        int[] lengthsY = {edge, h - edge * 2, edge};
        int dy = 0;
        for (int row = 0; row < 3; row++) {
            int dx = 0;
            for (int column = 0; column < 3; column++) {
                if (center || row != 1 || column != 1) {
                    int u = column == 0 ? 0 : column == 1 ? 8 : 64 - edge;
                    int v = row == 0 ? 0 : row == 1 ? 8 : 64 - edge;
                    int sw = column == 1 ? 48 : edge, sh = row == 1 ? 48 : edge;
                    repeat(g, texture, x + dx, y + dy, lengthsX[column], lengthsY[row], u, v, sw, sh);
                }
                dx += lengthsX[column];
            }
            dy += lengthsY[row];
        }
    }

    private static void repeat(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, int u, int v, int sw, int sh) {
        for (int dy = 0; dy < h; dy += sh) for (int dx = 0; dx < w; dx += sw)
            g.blit(texture, x + dx, y + dy, u, v, Math.min(sw, w - dx), Math.min(sh, h - dy), 64, 64);
    }
}
