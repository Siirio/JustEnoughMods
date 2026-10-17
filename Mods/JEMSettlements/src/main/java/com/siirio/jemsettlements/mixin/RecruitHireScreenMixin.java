package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.client.SettlementPriceRenderer;
import com.siirio.jemsettlements.client.HireQuoteClient;
import com.talhanation.recruits.client.gui.RecruitHireScreen;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitHireMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native layout and hire button retained; only the server-quoted currency cell changes. */
@Mixin(value = RecruitHireScreen.class)
public abstract class RecruitHireScreenMixin extends AbstractContainerScreen<RecruitHireMenu> {
    @Shadow(remap = false) @Final private AbstractRecruitEntity recruit;
    protected RecruitHireScreenMixin(RecruitHireMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Redirect(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$hirePrice(GuiGraphics g, ItemStack stack, int x, int y) {
        var quote = HireQuoteClient.quote(recruit.getId());
        if (quote == null) g.renderFakeItem(stack, x, y);
        else SettlementPriceRenderer.draw(g, font, quote.cost(), x, y, 16, 0xFFFFFFFF, quote.coins());
    }
    @Redirect(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jem$hireCount(GuiGraphics g, Font textFont, ItemStack stack, int x, int y) {
        if (HireQuoteClient.quote(recruit.getId()) == null) g.renderItemDecorations(textFont, stack, x, y);
    }
    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void jem$quote(GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
        var quote = HireQuoteClient.quote(recruit.getId());
        if (quote == null) return;
        g.drawString(font, Component.translatable("jem.settlements.hire.cost"), 7, 75, 0xFF404040, false);
        SettlementPriceRenderer.draw(g, font, quote.cost(), 72, 71, 94, 0xFF404040, quote.coins());
        g.drawString(font, Component.translatable("jem.settlements.hire.wage"), 7, 88, 0xFF404040, false);
        SettlementPriceRenderer.draw(g, font, quote.wage(), 72, 84, 94, 0xFF404040, quote.coins());
    }
}
