package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.client.SettlementPriceRenderer;
import com.siirio.jemsettlements.client.NobleQuoteClient;
import com.siirio.jemsettlements.client.NobleTradePresentation;
import com.siirio.jemsettlements.client.RecruitHirePresentation;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.NobleTradeScreen;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.component.RecruitsMultiLineEditBox;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsHireTrade;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NobleTradeScreen.class)
public abstract class NobleTradeScreenMixin extends Screen implements NobleQuoteClient.ScreenAccess {
    @Shadow(remap = false) @Final private VillagerNobleEntity villagerNoble;
    @Shadow(remap = false) private RecruitsHireTrade selection;
    @Shadow(remap = false) private Button hireButton;
    @Shadow(remap = false) private RecruitsMultiLineEditBox descriptionBox;
    @Shadow(remap = false) private ScrollDropDownMenu<RecruitsGroup> groupSelectionDropDownMenu;
    @Shadow(remap = false) public RecruitsGroup group;
    protected NobleTradeScreenMixin(Component title) { super(title); }
    @Override public int jem$nobleId() { return villagerNoble.getId(); }

    @Inject(method = "init", at = @At("TAIL"))
    private void jem$materials(CallbackInfo ci) {
        // Keep each native action, including the permission-gated inventory action.
        for (var child : java.util.List.copyOf(children())) {
            if (!(child instanceof Button nativeButton)) continue;
            var button = NobleTradePresentation.button(font, nativeButton);
            button.active = nativeButton.active;
            button.visible = nativeButton.visible;
            button.setTooltip(nativeButton.getTooltip());
            removeWidget(nativeButton);
            addRenderableWidget(button);
            if (nativeButton == hireButton) hireButton = button;
        }
        removeWidget(descriptionBox);
        descriptionBox = addRenderableWidget(NobleTradePresentation.description(font, descriptionBox));
        // Same native group objects and selection callback as RecruitHireScreen.
        var nativeGroup = groupSelectionDropDownMenu;
        removeWidget(nativeGroup);
        groupSelectionDropDownMenu = new ScrollDropDownMenu<>(group, nativeGroup.getX(), nativeGroup.getY(),
                nativeGroup.getWidth(), nativeGroup.getHeight(), ClientManager.groups, RecruitHirePresentation::groupName, selected -> {
                    group = selected;
                    ClientManager.groupSelection = ClientManager.groups.indexOf(selected);
                });
        groupSelectionDropDownMenu.canSelect = nativeGroup.canSelect;
        groupSelectionDropDownMenu.setBgFill(0xff49301f);
        groupSelectionDropDownMenu.setBgFillHovered(0xff765035);
        groupSelectionDropDownMenu.setBgFillSelected(0xff5b3c27);
        groupSelectionDropDownMenu.setDisplayColor(0xfff9e5a6);
        groupSelectionDropDownMenu.setOptionTextColor(0xfff9e5a6);
        // NobleTradeScreen renders the native dropdown last, above its description.
        addWidget(groupSelectionDropDownMenu);
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$background(GuiGraphics g, int mx, int my, float partial, CallbackInfo ci) {
        var screen = (RecruitsScreenBase) (Object) this;
        NobleTradePresentation.background(g, screen.getGuiLeft(), screen.getGuiTop());
        ci.cancel();
    }

    @Redirect(method = "renderForeground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", remap = true), remap = false)
    private int jem$readableLabels(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
        var screen = (RecruitsScreenBase) (Object) this;
        if (x == screen.getGuiLeft() + 98 && y == screen.getGuiTop() + 7)
            text = RecruitHirePresentation.tr("noble.level", villagerNoble.getTraderLevel());
        int available = x == screen.getGuiLeft() + 10 ? 78 : 150;
        return g.drawString(font, font.plainSubstrByWidth(text.getString(), available), x, y, 0xfff8e9c5, true);
    }
    @Inject(method = "getPlayerCurrencyAmount", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$coinAffordability(CallbackInfoReturnable<Integer> ci) {
        if (selection == null) return;
        var quote = NobleQuoteClient.quote(villagerNoble.getId(), selection.resourceLocation);
        if (quote != null) ci.setReturnValue(NobleQuoteClient.affordable(quote) ? Integer.MAX_VALUE : -1);
    }
    @Inject(method = "renderForeground", at = @At("TAIL"), remap = false)
    private void jem$wage(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var nativeScreen = (RecruitsScreenBase) (Object) this;
        if (selection != null) NobleTradePresentation.payroll(g, font,
                NobleQuoteClient.quote(villagerNoble.getId(), selection.resourceLocation), nativeScreen.getGuiLeft(), nativeScreen.getGuiTop());
        var priceHover = NobleQuoteClient.takePriceHover();
        if (priceHover != null) SettlementPriceRenderer.tooltip(g, font, priceHover.cost(), mouseX, mouseY, width, height, priceHover.coins());
        if (selection == null) return;
        var quote = NobleQuoteClient.quote(villagerNoble.getId(), selection.resourceLocation);
        if (quote == null) return;
        int x = nativeScreen.getGuiLeft() + 97, y = nativeScreen.getGuiTop() + 53;
        int footerY = nativeScreen.getGuiTop() + 155;
        if (mouseX >= x && mouseX < x + 75 && mouseY >= footerY && mouseY < footerY + 40)
            SettlementPriceRenderer.tooltip(g, font, quote.cost(), mouseX, mouseY, width, height, quote.coins());
        else if (mouseX >= x && mouseX < x + 75 && mouseY >= y && mouseY < y + 20
                || mouseX >= x + 76 && mouseX < x + 151 && mouseY >= footerY && mouseY < footerY + 40)
            g.renderTooltip(font, Component.translatable("jem.settlements.hire.wage").append(": ")
                    .append(SettlementPriceRenderer.format(quote.wage(), quote.coins())), mouseX, mouseY);
    }
}
