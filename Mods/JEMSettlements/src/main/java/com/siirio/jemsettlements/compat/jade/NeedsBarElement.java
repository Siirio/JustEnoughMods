package com.siirio.jemsettlements.compat.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

/** Compact reference-directed rail; real synchronized values also remain readable as text. */
final class NeedsBarElement extends Element {
    private static final ResourceLocation MOOD_ICON = new ResourceLocation("jem_settlements", "textures/gui/jade/mood.png");
    private final int value;
    private final boolean mood;
    private final Component label;
    private final Component amount;
    NeedsBarElement(int value, boolean mood) {
        this.value = Math.max(0, Math.min(100, value)); this.mood = mood;
        this.label = Component.translatable(mood ? "jade.jem_settlements.happiness_bar" : "jade.jem_settlements.satiety_bar");
        this.amount = Component.literal(this.value + "%");
        message(label.getString() + ": " + amount.getString());
    }
    @Override public Vec2 getSize() { return new Vec2(Math.max(164, Minecraft.getInstance().font.width(label) + 110), 13); }
    @Override public void render(GuiGraphics graphics, float x, float y, float width, float height) {
        var font = Minecraft.getInstance().font;
        int left = Math.round(x), top = Math.round(y);
        graphics.drawString(font, label, left, top + 2, 0xFFE9DDBF, false);
        int start = left + font.width(label) + 7;
        int end = Math.round(x + width) - 42;
        int railWidth = Math.max(32, end - start);
        graphics.fill(start - 1, top + 1, start + railWidth + 1, top + 12, 0xFF211B18);
        graphics.fill(start, top + 2, start + railWidth, top + 11, 0xFF514139);
        int fill = mood ? railWidth : Math.round(railWidth * value / 100F);
        for (int pixel = 0; pixel < fill; pixel++) {
            float fraction = pixel / (float) Math.max(1, railWidth - 1);
            int color = mood ? (fraction < .5F ? blend(0xFF57BA59, 0xFFE8CE50, fraction * 2) : blend(0xFFE8CE50, 0xFFB6443C, fraction * 2 - 1))
                    : blend(0xFFBD3931, 0xFFF0D83E, fraction);
            graphics.fill(start + pixel, top + 2, start + pixel + 1, top + 11, color);
        }
        if (mood) drawItem(graphics, new ItemStack(Items.NETHER_STAR), start + Math.round((railWidth - 9) * (100 - value) / 100F), top + 2, 9);
        int iconX = start + railWidth + 4;
        if (mood) graphics.blit(MOOD_ICON, iconX, top + 2, 0, 0, 9, 9, 9, 9);
        else drawItem(graphics, new ItemStack(Items.BREAD), iconX, top + 1, 11);
        graphics.drawString(font, amount, iconX + 13, top + 2, 0xFFE9DDBF, false);
    }
    private static int blend(int a, int b, float t) {
        return 0xFF000000 | (Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t) << 16)
                | (Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t) << 8)
                | Math.round((a & 255) * (1 - t) + (b & 255) * t);
    }
    private static void drawItem(GuiGraphics graphics, ItemStack item, int x, int y, int size) {
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0); graphics.pose().scale(size / 16F, size / 16F, 1);
        graphics.renderItem(item, 0, 0); graphics.pose().popPose();
    }
}
