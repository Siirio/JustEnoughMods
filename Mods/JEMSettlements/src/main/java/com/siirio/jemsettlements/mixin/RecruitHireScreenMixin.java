package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.client.HireQuoteClient;
import com.siirio.jemsettlements.client.RecruitHirePresentation;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.RecruitHireScreen;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitHireMenu;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native menu, slot positions, group list and hire request remain authoritative. */
@Mixin(RecruitHireScreen.class)
public abstract class RecruitHireScreenMixin extends AbstractContainerScreen<RecruitHireMenu> {
    @Shadow(remap = false) @Final private AbstractRecruitEntity recruit;
    @Shadow(remap = false) private ExtendedButton hireButton;
    @Shadow(remap = false) private ScrollDropDownMenu<RecruitsGroup> groupSelectionDropDownMenu;
    @Shadow(remap = false) public RecruitsGroup group;
    @Unique private float jem$rotation;
    @Unique private boolean jem$rotating;
    protected RecruitHireScreenMixin(RecruitHireMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Inject(method = "init", at = @At("HEAD"))
    private void jem$dimensions(CallbackInfo ci) { imageWidth = 300; imageHeight = 223; }

    @Inject(method = "init", at = @At("TAIL"))
    private void jem$controls(CallbackInfo ci) {
        ExtendedButton nativeHire = hireButton;
        removeWidget(nativeHire);
        hireButton = addRenderableWidget(new ExtendedButton(leftPos + 8, topPos + 101, 80, 20,
                RecruitHirePresentation.tr("hire"), ignored -> nativeHire.onPress()) {
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                RecruitHirePresentation.wood(g, getX(), getY(), width, height, active && isHoveredOrFocused());
                g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 6,
                        active ? 0xfff9e5a6 : 0xffb1a18c);
            }
        });
        // Native command GROUP, not a recruit class or training selector.
        removeWidget(groupSelectionDropDownMenu);
        groupSelectionDropDownMenu = new ScrollDropDownMenu<>(group, leftPos + 94, topPos + 101, 80, 20,
                ClientManager.groups, RecruitHirePresentation::groupName, selected -> {
                    group = selected;
                    ClientManager.groupSelection = ClientManager.groups.indexOf(selected);
                });
        groupSelectionDropDownMenu.setBgFill(0xff49301f);
        groupSelectionDropDownMenu.setBgFillHovered(0xff765035);
        groupSelectionDropDownMenu.setBgFillSelected(0xff5b3c27);
        groupSelectionDropDownMenu.setDisplayColor(0xfff9e5a6);
        groupSelectionDropDownMenu.setOptionTextColor(0xfff9e5a6);
        // Native screen renders the dropdown after slots so open options stay on top.
        addWidget(groupSelectionDropDownMenu);
        jem$updateHire();
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void jem$tick(CallbackInfo ci) { jem$updateHire(); }

    @Unique private void jem$updateHire() {
        if (hireButton == null) return;
        var quote = HireQuoteClient.quote(recruit.getId());
        boolean awaitingQuote = RecruitHirePresentation.requiresQuote() && quote == null;
        hireButton.active = ClientManager.canPlayerHire && group != null && !group.isDisabled() && !recruit.getIsOwned()
                && !awaitingQuote && (quote == null || quote.affordable() && quote.reason().isEmpty());
        Component reason = awaitingQuote ? RecruitHirePresentation.tr("pending")
                : quote != null && !quote.reason().isEmpty() ? Component.translatable(quote.reason())
                : RecruitHirePresentation.tr(recruit.getIsOwned() ? "already_employed"
                : group == null || group.isDisabled() ? "group_required" : "unavailable");
        hireButton.setTooltip(hireButton.active ? null : Tooltip.create(reason));
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void jem$background(GuiGraphics g, float partial, int mx, int my, CallbackInfo ci) {
        RecruitHirePresentation.background(g, font, recruit, menu, leftPos, topPos, jem$rotation);
        ci.cancel();
    }
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void jem$labels(GuiGraphics g, int mx, int my, CallbackInfo ci) {
        RecruitHirePresentation.labels(g, font, recruit, HireQuoteClient.quote(recruit.getId()));
        ci.cancel();
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void jem$tooltips(GuiGraphics g, int mx, int my, float partial, CallbackInfo ci) {
        RecruitHirePresentation.tooltips(g, font, recruit, leftPos, topPos, mx, my);
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void jem$startRotation(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && mx >= leftPos + 9 && mx < leftPos + 75 && my >= topPos + 36 && my < topPos + 86) {
            jem$rotating = true; cir.setReturnValue(true);
        }
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (jem$rotating && button == 0) { jem$rotation = (jem$rotation + (float) dx * 2F) % 360F; return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (jem$rotating && button == 0) { jem$rotating = false; return true; }
        return super.mouseReleased(mx, my, button);
    }
}
