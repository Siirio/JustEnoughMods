package com.siirio.jemsettlements.mixin.xaero;

import com.siirio.jemsettlements.compat.xaero.SettlementMapClient;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class SettlementMapScreenMixin {
    @Shadow private double cameraX;
    @Shadow private double cameraZ;
    @Shadow private double scale;
    @Shadow private ResourceKey<Level> lastViewedDimensionId;
    @Shadow private ResourceKey<Level> rightClickDim;
    @Shadow private xaero.map.gui.dropdown.rightclick.GuiRightClickMenu rightClickMenu;
    @Shadow private int mouseBlockPosX;
    @Shadow private int mouseBlockPosZ;

    @Inject(method = "m_7856_", at = @At("TAIL"))
    private void jem$request(CallbackInfo callback) { SettlementMapClient.opened(); }

    @Inject(method = "renderPreDropdown", at = @At("HEAD"))
    private void jem$settlements(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (lastViewedDimensionId == null) return;
        var screen = (GuiMap) (Object) this;
        SettlementMapClient.render(graphics, lastViewedDimensionId.location(), cameraX, cameraZ,
                scale / Minecraft.getInstance().getWindow().getGuiScale(), screen.width, screen.height,
                mouseBlockPosX, mouseBlockPosZ, mouseX, mouseY);
    }

    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true)
    private void jem$beginSettlementDrag(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button == 0 && rightClickMenu == null && lastViewedDimensionId != null && SettlementMapClient.begin(lastViewedDimensionId.location(), mouseBlockPosX, mouseBlockPosZ)) callback.setReturnValue(true);
    }

    @Inject(method = "m_6348_", at = @At("HEAD"), cancellable = true)
    private void jem$finishSettlementDrag(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button != 0) return;
        if (lastViewedDimensionId != null) SettlementMapClient.update(lastViewedDimensionId.location(), mouseBlockPosX, mouseBlockPosZ);
        if (!SettlementMapClient.finish()) return;
        if (lastViewedDimensionId == null || SettlementMapClient.pending(lastViewedDimensionId.location()) == null) { callback.setReturnValue(true); return; }
        ((GuiMap) (Object) this).m_6375_(mouseX, mouseY, 1);
        callback.setReturnValue(true);
    }

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void jem$expand(CallbackInfoReturnable<ArrayList<RightClickOption>> callback) {
        if (rightClickDim == null) return;
        var draft = SettlementMapClient.pending(rightClickDim.location());
        if (draft == null) return;
        var options = callback.getReturnValue();
        options.add(new RightClickOption("gui.jem_settlements.settlement_claim", options.size(), (GuiMap) (Object) this) {
            @Override public void onAction(Screen screen) {
                SettlementMapClient.confirm(draft);
            }
        }.setNameFormatArgs(SettlementMapClient.price(draft)));
    }
}
