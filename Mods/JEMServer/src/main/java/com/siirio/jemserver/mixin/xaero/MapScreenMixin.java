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

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void jem$expand(CallbackInfoReturnable<ArrayList<RightClickOption>> callback) {
        if (rightClickDim == null || !MapClient.connected()) return;
        var town = MapClient.settlement(rightClickDim.location(), rightClickX, rightClickZ, true);
        if (town == null || town.radius() >= 256) return;
        var options = callback.getReturnValue();
        boolean confirming = MapClient.previewing(town.id());
        options.add(new RightClickOption(confirming ? "gui.jem_server.settlement_expand_confirm" : "gui.jem_server.settlement_expand_preview", options.size(), (GuiMap) (Object) this) {
            @Override public void onAction(Screen screen) {
                if (confirming) { MapNetwork.expandSettlement(town.id()); MapClient.clearSettlementPreview(); }
                else MapClient.preview(town.id());
            }
        });
    }
}
