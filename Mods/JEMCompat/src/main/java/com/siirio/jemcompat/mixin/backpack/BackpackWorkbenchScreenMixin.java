package com.siirio.jemcompat.mixin.backpack;

import com.mrcrayfish.backpacked.client.TextureDefinitions;
import com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import com.mrcrayfish.framework.api.client.screen.widget.texture.FrameworkTexture;
import com.siirio.jemcompat.feature.backpack.BackpackWorkbench;
import net.minecraft.client.gui.GuiGraphics;
import com.mrcrayfish.backpacked.client.gui.screen.widget.popup.CustomContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BackpackScreen.class, remap = false)
public abstract class BackpackWorkbenchScreenMixin extends CustomContainerScreen<BackpackContainerMenu> {
    @Shadow @Final private java.util.List<net.minecraft.client.gui.layouts.Layout> layouts;
    protected BackpackWorkbenchScreenMixin(BackpackContainerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (!super.hasClickedOutside(mouseX, mouseY, left, top, button)) return false;
        if (hasPopupMenu()) return true;
        var layout = (BackpackWorkbench) menu;
        int rowLeft = left + layout.jemcompat$workbenchLeft(imageWidth);
        int rowTop = top + layout.jemcompat$insertTop(menu.getRows());
        return layout.jemcompat$insertHeight() == 0 || mouseX < rowLeft || mouseX >= rowLeft + layout.jemcompat$insertWidth()
                || mouseY < rowTop || mouseY >= rowTop + layout.jemcompat$insertHeight();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (hasPopupMenu()) return super.mouseScrolled(mouseX, mouseY, delta);
        if (super.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (((BackpackWorkbench) menu).jemcompat$insertHeight() == 0 || imageHeight <= height - 8 || delta == 0) return false;
        jemcompat$moveViewport(net.minecraft.util.Mth.clamp(topPos + (int) Math.signum(delta) * 18, height - imageHeight - 4, 4));
        return true;
    }

    @Unique
    private void jemcompat$moveViewport(int nextTop) {
        int shift = nextTop - topPos;
        topPos = nextTop;
        var contained = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.minecraft.client.gui.components.AbstractWidget, Boolean>());
        for (var layout : layouts) {
            layout.visitWidgets(contained::add);
            layout.setY(layout.getY() + shift);
        }
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && !contained.contains(widget)) widget.setY(widget.getY() + shift);
    }

    @Inject(method = "m_7856_()V", at = @At("TAIL"), remap = false)
    private void jemcompat$startAtReachableTop(CallbackInfo callback) {
        if (((BackpackWorkbench) menu).jemcompat$insertHeight() > 0 && imageHeight > height - 8) jemcompat$moveViewport(4);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jemcompat$expandScreen(BackpackContainerMenu menu, Inventory inventory, Component title, CallbackInfo callback) {
        if (((BackpackWorkbench) menu).jemcompat$insertHeight() > 0) {
            imageHeight += ((BackpackWorkbench) menu).jemcompat$inventoryShift(menu.getRows());
            inventoryLabelY += ((BackpackWorkbench) menu).jemcompat$inventoryShift(menu.getRows());
        }
    }

    @Redirect(method = "drawBackgroundWindow", at = @At(value = "INVOKE", target = "Lcom/mrcrayfish/framework/api/client/screen/widget/texture/FrameworkTexture;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void jemcompat$movePlayerPanel(FrameworkTexture texture, GuiGraphics graphics, int x, int y, int width, int height) {
        if (((BackpackWorkbench) menu).jemcompat$insertHeight() > 0) {
            if (texture == TextureDefinitions.INVENTORY_BACKGROUND) {
                y += ((BackpackWorkbench) menu).jemcompat$inventoryShift(menu.getRows());
                texture = TextureDefinitions.AUGMENT_MENU_BACKGROUND;
            } else if (texture == TextureDefinitions.INVENTORY_SLOT) {
                y += ((BackpackWorkbench) menu).jemcompat$inventoryShift(menu.getRows());
                texture = TextureDefinitions.BACKPACK_SLOT;
            }
        }
        texture.draw(graphics, x, y, width, height);
    }

    @Inject(method = "drawBackgroundWindow", at = @At("TAIL"))
    private void jemcompat$drawCrafting(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, CallbackInfo callback) {
        if (((BackpackWorkbench) menu).jemcompat$insertHeight() == 0) return;
        var layout = (BackpackWorkbench) menu;
        int left = x + layout.jemcompat$workbenchLeft(width);
        int top = y + layout.jemcompat$insertTop(menu.getRows());
        if (layout.jemcompat$hasWorkbench()) {
        TextureDefinitions.BACKPACK_BACKGROUND.draw(graphics, left, top, BackpackWorkbench.CRAFT_WIDTH, BackpackWorkbench.INSERT_HEIGHT);
        TextureDefinitions.BACKPACK_SLOT.draw(graphics, left + BackpackWorkbench.GRID_X - 1, top + BackpackWorkbench.GRID_Y - 1, 54, 54);
        TextureDefinitions.BACKPACK_SLOT.draw(graphics, left + BackpackWorkbench.RESULT_X - 1, top + BackpackWorkbench.RESULT_Y - 1, 18, 18);
        TextureDefinitions.ARROW_RIGHT.draw(graphics, left + 72, top + 34, 8, 8);
        }
        if (layout.jemcompat$hasFurnace()) {
            left = x + layout.jemcompat$furnaceLeft(width);
            int furnaceTop = top;
            TextureDefinitions.BACKPACK_BACKGROUND.draw(graphics, left, furnaceTop, BackpackWorkbench.CRAFT_WIDTH, BackpackWorkbench.FURNACE_HEIGHT);
            TextureDefinitions.BACKPACK_SLOT.draw(graphics, left + BackpackWorkbench.FURNACE_INPUT_X - 1, furnaceTop + 11, 18, 18);
            TextureDefinitions.BACKPACK_SLOT.draw(graphics, left + BackpackWorkbench.FURNACE_FUEL_X - 1, furnaceTop + 47, 18, 18);
            TextureDefinitions.BACKPACK_SLOT.draw(graphics, left + BackpackWorkbench.FURNACE_OUTPUT_X - 1, furnaceTop + 29, 18, 18);
            var furnaceTexture = new net.minecraft.resources.ResourceLocation("minecraft", "textures/gui/container/furnace.png");
            int burning = layout.jemcompat$furnaceData(0), fuel = layout.jemcompat$furnaceData(1);
            if (burning > 0 && fuel > 0) {
                int flame = Math.min(13, (int)((long) burning * 13 / fuel)) + 1;
                graphics.blit(furnaceTexture, left + 23, furnaceTop + 45 - flame, 176, 12 - flame, 14, flame);
            }
            graphics.blit(furnaceTexture, left + 49, furnaceTop + 31, 79, 34, 24, 17);
            int duration = layout.jemcompat$furnaceData(3);
            int progress = duration <= 0 ? 0 : Math.min(24, (int)((long)layout.jemcompat$furnaceData(2) * 24 / duration));
            if (progress > 0) graphics.blit(furnaceTexture, left + 49, furnaceTop + 31, 176, 14, progress, 16);
        }
        if (imageHeight > this.height - 8) graphics.drawString(font, Component.translatable("gui.jemcompat.backpack.scroll"), 4, this.height - 12, 0xFFF5E9D4, true);
    }
}
