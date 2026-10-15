package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.MapNetwork;
import com.siirio.jemserver.client.xaero.MapClient;
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
public abstract class MapScreenMixin {
    @Shadow private double cameraX;
    @Shadow private double cameraZ;
    @Shadow private double scale;
    @Shadow private ResourceKey<Level> lastViewedDimensionId;
    @Shadow private ResourceKey<Level> rightClickDim;
    @Shadow private int rightClickX;
    @Shadow private int rightClickZ;
    @Shadow private int mouseBlockPosX;
    @Shadow private int mouseBlockPosZ;

    @Inject(method = "m_7856_", at = @At("TAIL"))
    private void jem$request(CallbackInfo callback) { if (MapClient.connected()) MapNetwork.request(); }

    @Inject(method = "renderPreDropdown", at = @At("HEAD"))
    private void jem$settlements(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (lastViewedDimensionId == null) return;
        var screen = (GuiMap) (Object) this;
        MapClient.render(graphics, lastViewedDimensionId.location(), cameraX, cameraZ,
                scale / Minecraft.getInstance().getWindow().getGuiScale(), screen.width, screen.height,
                mouseBlockPosX, mouseBlockPosZ, mouseX, mouseY);
    }

    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true)
    private void jem$beginSettlementDrag(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button == 0 && lastViewedDimensionId != null && MapClient.beginSettlementDrag(lastViewedDimensionId.location(), mouseBlockPosX, mouseBlockPosZ)) callback.setReturnValue(true);
    }

    @Inject(method = "m_6348_", at = @At("HEAD"), cancellable = true)
    private void jem$finishSettlementDrag(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button != 0 || !MapClient.finishSettlementDrag()) return;
        ((GuiMap) (Object) this).m_6375_(mouseX, mouseY, 1);
        callback.setReturnValue(true);
    }

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void jem$expand(CallbackInfoReturnable<ArrayList<RightClickOption>> callback) {
        if (rightClickDim == null || !MapClient.connected()) return;
        var draft = MapClient.pendingSettlement(rightClickDim.location(), rightClickX, rightClickZ);
        if (draft == null) return;
        var options = callback.getReturnValue();
        options.add(new RightClickOption("gui.jem_server.settlement_claim", options.size(), (GuiMap) (Object) this) {
            @Override public void onAction(Screen screen) {
                MapNetwork.expandSettlement(draft.id(), draft.targetX(), draft.targetZ()); MapClient.clearSettlementPreview();
            }
        }.setNameFormatArgs(com.siirio.jemeconomy.physical.CoinFormatter.format(draft.cost())));
    }
}
