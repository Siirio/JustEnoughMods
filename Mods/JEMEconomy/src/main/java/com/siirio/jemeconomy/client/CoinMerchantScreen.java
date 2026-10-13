package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.merchant.CoinMerchantMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CoinMerchantScreen extends AbstractContainerScreen<CoinMerchantMenu> {
    public CoinMerchantScreen(CoinMerchantMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 186; inventoryLabelY = 92; }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFE5C991);
        graphics.fill(leftPos + 5, topPos + 16, leftPos + 171, topPos + 94, 0xFF5F472E);
        graphics.fill(leftPos + 5, topPos + 100, leftPos + 171, topPos + 181, 0xFFB88D59);
        for (var slot : menu.slots) { graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF302219); graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF9B7048); }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY); }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { graphics.drawString(font, title, 8, 5, 0x3C2415, false); graphics.drawString(font, Component.translatable("jem.economy.merchant.click"), 8, 84, 0xF4DFC0, false); graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x3C2415, false); }
}
