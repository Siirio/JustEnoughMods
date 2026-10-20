package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.framework.api.client.screen.widget.texture.FrameworkTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Reuses installed native assets; no generated art or repaint of the approved Workbench icon. */
final class NativeItemAugmentIcon extends FrameworkTexture {
    private final ItemStack item;
    NativeItemAugmentIcon(Item item) { this.item = item.getDefaultInstance(); }
    @Override public int width() { return 16; }
    @Override public int height() { return 16; }
    @Override public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(width / 16F, height / 16F, 1F);
        graphics.renderItem(item, 0, 0);
        graphics.pose().popPose();
    }
}
