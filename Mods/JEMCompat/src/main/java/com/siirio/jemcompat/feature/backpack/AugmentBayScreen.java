package com.siirio.jemcompat.feature.backpack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mrcrayfish.backpacked.client.TextureDefinitions;

public final class AugmentBayScreen extends AbstractContainerScreen<AugmentBayMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("jemcompat", "textures/gui/augment_bay.png");
    private Button augment;
    private Button bay;

    public AugmentBayScreen(AugmentBayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 220;
        inventoryLabelY = 123;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new BayButton(leftPos + 8, topPos + 66, 18, Component.literal("<"), b -> send(0)));
        augment = addRenderableWidget(new BayButton(leftPos + 28, topPos + 66, 120, Component.empty(), b -> send(1)));
        addRenderableWidget(new BayButton(leftPos + 150, topPos + 66, 18, Component.literal(">"), b -> send(1)));
        bay = addRenderableWidget(new BayButton(leftPos + 8, topPos + 91, 64, Component.empty(), b -> send(3)));
        addRenderableWidget(new BayButton(leftPos + 76, topPos + 91, 92, Component.translatable("gui.jemcompat.augment_bay.install"), b -> send(4)));
        if (imageHeight > height - 8) moveViewport(4);
    }

    private void moveViewport(int nextTop) {
        int shift = nextTop - topPos;
        topPos = nextTop;
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) widget.setY(widget.getY() + shift);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (super.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (imageHeight <= height - 8 || delta == 0) return false;
        moveViewport(net.minecraft.util.Mth.clamp(topPos + (int) Math.signum(delta) * 18, height - imageHeight - 4, 4));
        return true;
    }

    private void send(int button) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button); }

    @Override
    protected void containerTick() {
        super.containerTick();
        var type = menu.selectedType();
        augment.setMessage(type == null ? Component.empty() : type.name());
        augment.setTooltip(type == null ? null : Tooltip.create(type.description()));
        bay.setMessage(Component.translatable("gui.jemcompat.augment_bay.bay", menu.selectedBay() + 1));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 176, 220);
        graphics.renderFakeItem(new ItemStack(Items.LAPIS_LAZULI), leftPos + 76, topPos + 18);
        graphics.drawString(font, "×4", leftPos + 94, topPos + 23, 0xFF47392A, false);
        graphics.renderFakeItem(new ItemStack(Items.CRAFTING_TABLE), leftPos + 121, topPos + 18);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (imageHeight > height - 8) graphics.drawString(font, Component.translatable("gui.jemcompat.backpack.scroll"), 4, height - 12, 0xFFF5E9D4, true);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private final class BayButton extends Button {
        private BayButton(int x, int y, int width, Component text, OnPress action) {
            super(x, y, width, 20, text, action, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            var background = !active ? TextureDefinitions.BUTTON_DISABLED : isHoveredOrFocused()
                    ? TextureDefinitions.BUTTON_ENABLED_HOVERED : TextureDefinitions.BUTTON_ENABLED;
            background.draw(graphics, getX(), getY(), getWidth(), getHeight());
            String text = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 6);
            graphics.drawCenteredString(font, text, getX() + getWidth() / 2, getY() + 6, active ? 0xFFF5E9D4 : 0xFFB8AA91);
        }
    }
}
