package com.siirio.jemserver.client.smp;

import static com.siirio.jemserver.client.ui.JemPalette.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Consumer;

final class ItemRewardStrip {
    static final int CELL = 27;
    static int height(ListTag items, int width) {
        int columns = Math.max(1, width / CELL);
        return ((items.size() + columns - 1) / columns) * CELL;
    }
    static void render(GuiGraphics g, ListTag items, int x, int y, int width, int mouseX, int mouseY, Consumer<ItemStack> hover) {
        int columns = Math.max(1, width / CELL);
        for (int i = 0; i < items.size(); i++) {
            int sx = x + i % columns * CELL, sy = y + i / columns * CELL;
            if (sy + CELL < 0 || sy >= Minecraft.getInstance().getWindow().getGuiScaledHeight()) continue;
            var tag = items.getCompound(i);
            ItemStack stack = stack(tag);
            SmpGuiAssets.panel(g, sx, sy, CELL - 2, CELL - 2, INSET);
            g.renderItem(stack, sx + 4, sy + 3);
            g.renderItemDecorations(Minecraft.getInstance().font, stack, sx + 4, sy + 3);
            if (mouseX >= sx && mouseX < sx + CELL && mouseY >= sy && mouseY < sy + CELL) hover.accept(stack);
        }
    }
    static int horizontalWidth(ListTag items) {
        return items.size() * CELL;
    }
    static int twoRowWidth(ListTag items) {
        return ((items.size() + 1) / 2) * CELL;
    }
    static void renderTwoRows(GuiGraphics g, ListTag items, int x, int y, int width, int offset, int mouseX, int mouseY, Consumer<ItemStack> hover) {
        g.enableScissor(x, y, x + width, y + CELL * 2);
        try {
            for (int i = 0; i < items.size(); i++) {
                int sx = x + i / 2 * CELL - offset;
                int sy = y + i % 2 * CELL;
                if (sx + CELL <= x || sx >= x + width) continue;
                ItemStack stack = stack(items.getCompound(i));
                SmpGuiAssets.panel(g, sx, sy, CELL - 2, CELL - 2, INSET);
                g.renderItem(stack, sx + 4, sy + 3);
                g.renderItemDecorations(Minecraft.getInstance().font, stack, sx + 4, sy + 3);
                if (mouseX >= sx && mouseX < sx + CELL && mouseY >= sy && mouseY < sy + CELL) hover.accept(stack);
            }
        } finally {
            g.disableScissor();
        }
    }
    static void renderHorizontal(GuiGraphics g, ListTag items, int x, int y, int width, int offset, int mouseX, int mouseY, Consumer<ItemStack> hover) {
        g.enableScissor(x, y, x + width, y + CELL);
        try {
            for (int i = 0; i < items.size(); i++) {
                int sx = x + i * CELL - offset;
                if (sx + CELL <= x || sx >= x + width) continue;
                ItemStack stack = stack(items.getCompound(i));
                SmpGuiAssets.panel(g, sx, y, CELL - 2, CELL - 2, INSET);
                g.renderItem(stack, sx + 4, y + 3);
                g.renderItemDecorations(Minecraft.getInstance().font, stack, sx + 4, y + 3);
                if (mouseX >= sx && mouseX < sx + CELL && mouseY >= y && mouseY < y + CELL) hover.accept(stack);
            }
        } finally {
            g.disableScissor();
        }
    }
    private static ItemStack stack(CompoundTag tag) {
        if (tag.contains("id")) {
            var stack = ItemStack.of(tag);
            if (!stack.isEmpty()) {
                if (tag.contains("previewEnchantPower")) stack.setHoverName(Component.translatable("jem.smp.reward.enchanted_book", tag.getInt("previewEnchantPower")));
                return stack;
            }
            var missing = new ItemStack(Items.BARRIER);
            missing.setHoverName(Component.literal(tag.getString("id")));
            return missing;
        }
        var id = ResourceLocation.tryParse(tag.getString("item"));
        var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) return new ItemStack(Items.BARRIER);
        int count = Math.max(1, tag.contains("maxCount") ? tag.getInt("maxCount") : tag.getInt("count"));
        return new ItemStack(item, count);
    }
    private ItemRewardStrip() {}
}
