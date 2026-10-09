package com.siirio.jemserver.client.smp;

import net.minecraft.client.gui.GuiGraphics;
import static com.siirio.jemserver.client.ui.JemPalette.EDGE;
import static com.siirio.jemserver.client.ui.JemPalette.HOVER;
import static com.siirio.jemserver.client.ui.JemPalette.INSET;
import static com.siirio.jemserver.client.ui.JemPalette.PANEL;
import static com.siirio.jemserver.client.ui.JemPalette.GREEN;
import static com.siirio.jemserver.client.ui.JemPalette.RED;

public final class SmpGuiAssets {
    private SmpGuiAssets() {}

    public static int accent(String activity) {
        return switch (activity) {
            case "BLOOD_MOON" -> 0xFFB64E4E;
            case "FISHING" -> 0xFF67A9C4;
            case "COOKING_SHOW" -> 0xFFD99A58;
            case "RESOURCE_RUSH" -> 0xFF84B46A;
            case "BOSS", "BOSS_RAID" -> 0xFFC87A58;
            default -> com.siirio.jemserver.client.ui.JemPalette.COPPER;
        };
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, lighten(fill));
        graphics.fill(x, y + height - 1, x + width, y + height, darken(fill));
        graphics.fill(x, y + 1, x + 1, y + height - 1, lighten(fill));
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, darken(fill));
        if (width >= 8 && height >= 8) {
            graphics.fill(x, y, x + 2, y + 2, INSET);
            graphics.fill(x + width - 2, y, x + width, y + 2, INSET);
            graphics.fill(x, y + height - 2, x + 2, y + height, INSET);
            graphics.fill(x + width - 2, y + height - 2, x + width, y + height, INSET);
        }
    }

    public static void frame(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + 1, accent);
        graphics.fill(x, y + height - 1, x + width, y + height, accent);
        graphics.fill(x, y + 1, x + 1, y + height - 1, accent);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, accent);
    }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height, String role, boolean hovered, boolean disabled) {
        int base = switch (role) {
            case "primary" -> com.siirio.jemserver.client.ui.JemPalette.COPPER;
            case "confirm" -> GREEN;
            case "map" -> 0xFF315C7A;
            case "danger" -> RED;
            default -> PANEL;
        };
        int fill = disabled ? EDGE : hovered ? lighten(base) : base;
        panel(graphics, x, y, width, height, fill);
        int rail = disabled ? PANEL : switch (role) {
            case "primary" -> 0xFFFFD886;
            case "confirm" -> 0xFFA8C690;
            case "map" -> 0xFF8CC8EA;
            case "danger" -> 0xFFE58473;
            default -> accent(role);
        };
        graphics.fill(x + 2, y + 2, x + 4, y + height - 2, rail);
        if (hovered && !disabled) graphics.fill(x + 5, y + height - 2, x + width - 2, y + height - 1, rail);
    }

    public static boolean icon(GuiGraphics graphics, String key, int x, int y, int size) {
        return false;
    }

    public static void cover(GuiGraphics graphics, String activity, int x, int y, int width, int height) {
        coverBackground(graphics, activity, x, y, width, height);
        int size = Math.min(width, height);
        SmpIcons.draw(graphics, activity, x + (width - size) / 2, y + (height - size) / 2, size);
    }

    public static void coverBackground(GuiGraphics graphics, String activity, int x, int y, int width, int height) {
        int accent = accent(activity);
        graphics.fillGradient(x, y, x + width, y + height, 0xCC000000 | accent & 0xFFFFFF, 0xEE100C0A);
    }

    private static int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 255) + 18);
        int green = Math.min(255, ((color >> 8) & 255) + 16);
        int blue = Math.min(255, (color & 255) + 12);
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int darken(int color) {
        int red = Math.max(0, ((color >> 16) & 255) - 18);
        int green = Math.max(0, ((color >> 8) & 255) - 16);
        int blue = Math.max(0, (color & 255) - 12);
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
