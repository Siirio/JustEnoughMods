package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.shipping.ShippingBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ShippingBoxScreen extends AbstractContainerScreen<ShippingBoxMenu> {
    public ShippingBoxScreen(ShippingBoxMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 260; imageHeight = 248; inventoryLabelY = 154; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("jem.economy.shipping.preview"), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 178, topPos + 38, 74, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("jem.economy.shipping.collect"), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1))
                .bounds(leftPos + 178, topPos + 60, 74, 18).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFF0D5A5);
        graphics.fill(leftPos + 5, topPos + 14, leftPos + 171, topPos + 126, 0xFF7B4E2E);
        graphics.fill(leftPos + 5, topPos + 128, leftPos + 171, topPos + 152, 0xFF4D311F);
        graphics.fill(leftPos + 5, topPos + 160, leftPos + 171, topPos + 244, 0xFFC59A63);
        graphics.fill(leftPos + 174, topPos + 14, leftPos + 256, topPos + 244, 0xFF4D311F);
        for (int i = 0; i < menu.slots.size(); i++) { var slot = menu.slots.get(i); graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF2F2118); graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF9B7048); }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY); }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 5, 0x3C2415, false);
        graphics.drawString(font, Component.translatable("jem.economy.shipping.owner", menu.box().ownerName()), 8, 120, 0x3C2415, false);
        graphics.drawWordWrap(font, Component.translatable("jem.economy.shipping.status." + statusKey(menu.statusCode())), 180, 84, 70, 0xF7E2BE);
        graphics.drawWordWrap(font, Component.translatable("jem.economy.shipping.pending", menu.previewPayout()), 180, 112, 70, 0xFFFFD18A);
        graphics.drawWordWrap(font, Component.translatable("jem.economy.shipping.demand", Component.translatable("jem.economy.demand." + menu.primaryDemand()), Component.translatable("jem.economy.demand." + menu.secondaryDemand())), 180, 138, 70, 0xF7E2BE);
        graphics.drawWordWrap(font, Component.translatable("jem.economy.shipping.limits", menu.accountRemaining(), menu.globalRemaining()), 180, 178, 70, 0xF7E2BE);
        graphics.drawWordWrap(font, Component.translatable("jem.economy.shipping.next", menu.ticksUntilUpdate()), 180, 216, 70, 0xF7E2BE);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x3C2415, false);
    }
    private static String statusKey(int value) { return switch (value) { case 1 -> "preview"; case 2 -> "paid"; case 3 -> "empty"; case 4 -> "blocked_item"; case 5 -> "account_daily_cap"; case 6 -> "global_daily_cap"; case 7 -> "item_account_cap"; case 8 -> "item_global_cap"; case 9 -> "budget_too_small"; case 10 -> "payout_full"; case 11 -> "market_budget_exhausted"; case 12 -> "quarantined"; default -> "waiting"; }; }
}
