package com.siirio.jemcompat.mixin.backpack;

import com.mrcrayfish.backpacked.client.TextureDefinitions;
import com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import com.mrcrayfish.framework.api.client.screen.widget.texture.FrameworkTexture;
import com.siirio.jemcompat.feature.backpack.BackpackWorkbench;
import net.minecraft.client.gui.GuiGraphics;
import com.mrcrayfish.backpacked.client.gui.screen.widget.popup.CustomContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (hasPopupMenu()) return super.mouseScrolled(mouseX, mouseY, delta);
        if (super.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (!((BackpackWorkbench) menu).jemcompat$hasWorkbench() || imageHeight <= height - 8 || delta == 0) return false;
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
        if (((BackpackWorkbench) menu).jemcompat$hasWorkbench() && imageHeight > height - 8) jemcompat$moveViewport(4);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jemcompat$expandScreen(BackpackContainerMenu menu, Inventory inventory, Component title, CallbackInfo callback) {
        if (((BackpackWorkbench) menu).jemcompat$hasWorkbench()) {
            imageHeight += BackpackWorkbench.INSERT_HEIGHT;
            inventoryLabelY += BackpackWorkbench.INSERT_HEIGHT;
        }
    }

    @Redirect(method = "drawBackgroundWindow", at = @At(value = "INVOKE", target = "Lcom/mrcrayfish/framework/api/client/screen/widget/texture/FrameworkTexture;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void jemcompat$movePlayerPanel(FrameworkTexture texture, GuiGraphics graphics, int x, int y, int width, int height) {
        if (((BackpackWorkbench) menu).jemcompat$hasWorkbench()
                && (texture == TextureDefinitions.INVENTORY_BACKGROUND || texture == TextureDefinitions.INVENTORY_SLOT)) y += BackpackWorkbench.INSERT_HEIGHT;
        texture.draw(graphics, x, y, width, height);
    }

    @Inject(method = "drawBackgroundWindow", at = @At("TAIL"))
    private void jemcompat$drawCrafting(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, CallbackInfo callback) {
        if (!((BackpackWorkbench) menu).jemcompat$hasWorkbench()) return;
        int left = x + (width - 176) / 2;
        int top = y + 44 + menu.getRows() * 18;
        graphics.blit(new ResourceLocation("jemcompat", "textures/gui/backpack_workbench.png"), left, top, 0, 0, 176, 76, 176, 76);
        graphics.drawString(font, Component.translatable("gui.jemcompat.backpack.workbench"), left + 8, top + 4, 0xFF47392A, false);
        if (imageHeight > this.height - 8) graphics.drawString(font, Component.translatable("gui.jemcompat.backpack.scroll"), 4, this.height - 12, 0xFFF5E9D4, true);
    }
}
