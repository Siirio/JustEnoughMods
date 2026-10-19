package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.framework.api.client.screen.widget.texture.FrameworkTexture;
import net.minecraft.client.gui.GuiGraphics;

/** Editable 12-pixel crafting-table silhouette, in the native cream/shadow icon palette. */
final class WorkbenchIcon extends FrameworkTexture {
    private static final String[] PIXELS = {
            "............",
            ".ssssssssss.",
            ".sllsllslls.",
            ".ssssssssss.",
            ".sllsllslls.",
            ".ssssssssss.",
            ".sllsllslls.",
            ".ssssssssss.",
            "..ll....ll..",
            "..ll....ll..",
            "..ll....ll..",
            "..ss....ss.."
    };

    @Override public int width() { return 12; }
    @Override public int height() { return 12; }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int row = 0; row < 12; row++) for (int col = 0; col < 12; col++) {
            char pixel = PIXELS[row].charAt(col);
            if (pixel != '.') graphics.fill(x + col * width / 12, y + row * height / 12,
                    x + (col + 1) * width / 12, y + (row + 1) * height / 12,
                    pixel == 'l' ? 0xFFEEDBC0 : 0xFF98866B);
        }
    }
}
