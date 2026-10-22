package com.justenoughmods.achievementguide.client.handbook;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.client.book.gui.GuiBook;

/** Uses the native bookmark silhouette at its native size, mirrored to the left edge. */
final class HandbookRibbon extends Button {
    private final GuiBook book;
    private final HandbookSection section;
    private final boolean selected;
    private final ItemStack icon;

    HandbookRibbon(GuiBook book, HandbookSection section, int index, boolean selected) {
        super(book.bookLeft - 13, book.bookTop + 18 + index * 21, 13, 10, section.title(),
                b -> HandbookNavigation.select(book, section), DEFAULT_NARRATION);
        this.book = book;
        this.section = section;
        this.selected = selected;
        icon = new ItemStack(section.icon);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean focus = isHoveredOrFocused();
        graphics.setColor(((section.color >> 16) & 255) / 255F, ((section.color >> 8) & 255) / 255F,
                (section.color & 255) / 255F, 1);
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + width, getY(), 0);
        graphics.pose().scale(-1, 1, 1);
        GuiBook.drawFromTexture(graphics, book.book, 0, 0, 272 + (focus || selected ? 13 : 0), 160, 13, 10);
        graphics.pose().popPose();
        graphics.setColor(1, 1, 1, 1);
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + 2, getY() + 1, 0);
        graphics.pose().scale(0.5F, 0.5F, 1);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
        if (focus) book.setTooltip(section.title(), Component.translatable(selected
                ? "jem_advancements.handbook.selected" : "jem_advancements.handbook.select"));
    }
}
