package com.justenoughmods.achievementguide.client.handbook;

import com.justenoughmods.achievementguide.client.handbook.enchantment.EnchantmentHelp;
import com.justenoughmods.achievementguide.client.handbook.jei.HandbookJeiLinks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;

/** Native Patchouli item presentation with five stable slots and one overflow carousel. */
public final class HandbookItemLinks implements ICustomComponent {
    public String enchantment = "";
    public String items = "";
    private transient ResourceLocation enchantmentId;
    private transient List<ItemStack> stacks = List.of();
    private transient ItemStack book = ItemStack.EMPTY;
    private transient final ItemStack[] displayed = new ItemStack[6];
    private transient int x, y, overflow = 5;

    @Override public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {
        if (!enchantment.isEmpty()) enchantmentId = ResourceLocation.tryParse(lookup.apply(IVariable.wrap(enchantment)).asString());
        if (!items.isEmpty()) {
            List<ItemStack> available = new ArrayList<>();
            for (var value : lookup.apply(IVariable.wrap(items)).asList()) {
                ResourceLocation id = ResourceLocation.tryParse(value.asString());
                if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                    ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
                    if (!stack.isEmpty()) available.add(stack);
                }
            }
            stacks = List.copyOf(available);
        }
    }

    @Override public void build(int left, int top, int page) { x = left; y = top; }

    @Override public void onDisplayed(IComponentRenderContext context) {
        if (enchantmentId != null) {
            stacks = EnchantmentHelp.applicationItems(enchantmentId);
            book = EnchantmentHelp.enchantedBook(enchantmentId);
        }
        java.util.Arrays.fill(displayed, ItemStack.EMPTY);
    }

    @Override public void render(GuiGraphics graphics, IComponentRenderContext context, float partial, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int row = y;
        if (enchantmentId != null) {
            context.renderItemStack(graphics, x, y, mouseX, mouseY, book);
            graphics.drawWordWrap(font, Component.translatable("jem_advancements.handbook.links.book"), x + 22, y, 94, context.getTextColor());
            graphics.drawString(font, Component.translatable("jem_advancements.handbook.links.targets"), x, y + 29, context.getTextColor(), false);
            row += 42;
        }
        // Hold the currently drawn item while hovered, so a click always follows the shown stack.
        if (stacks.size() > 6 && !context.isAreaHovered(mouseX, mouseY, x + 90, row, 16, 16))
            overflow = 5 + context.getTicksInBook() / 20 % (stacks.size() - 5);
        for (int slot = 0; slot < 6; slot++) {
            int index = slot == 5 && stacks.size() > 6 ? overflow : slot;
            displayed[slot] = index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
            if (!displayed[slot].isEmpty()) context.renderItemStack(graphics, x + slot * 18, row, mouseX, mouseY, displayed[slot]);
        }
    }

    @Override public boolean mouseClicked(IComponentRenderContext context, double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1 || !ModList.get().isLoaded("jei")) return false;
        if (!book.isEmpty() && inside(mouseX, mouseY, x, y)) return HandbookJeiLinks.open(book, button == 1);
        int row = y + (enchantmentId == null ? 0 : 42);
        for (int slot = 0; slot < displayed.length; slot++) {
            ItemStack stack = displayed[slot];
            if (stack != null && !stack.isEmpty() && inside(mouseX, mouseY, x + 18 * slot, row))
                return HandbookJeiLinks.open(stack, button == 1);
        }
        return false;
    }

    private static boolean inside(double mouseX, double mouseY, int left, int top) {
        return mouseX >= left && mouseX < left + 16 && mouseY >= top && mouseY < top + 16;
    }
}
