package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.client.xaero.MapClient;
import com.siirio.jemserver.client.xaero.MapView;

import com.siirio.jemserver.MapNetwork;
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
public abstract class MapScreenMixin implements MapView {
    @Shadow private double cameraX;
    @Shadow private double cameraZ;
    @Shadow private double scale;
    @Shadow private boolean shouldResetCameraPos;
    @Shadow private ResourceKey<Level> lastViewedDimensionId;
    @Shadow private ResourceKey<Level> rightClickDim;
    @Shadow private int rightClickX;
    @Shadow private int rightClickZ;

    @Override
    public net.minecraft.resources.ResourceLocation jem$dimension() {
        return lastViewedDimensionId == null ? null : lastViewedDimensionId.location();
    }

    @Override
    public void jem$center(double x, double z) {
        cameraX = x; cameraZ = z; shouldResetCameraPos = false;
    }

    @Inject(method = "m_7856_", at = @At("TAIL"))
    private void jem$request(CallbackInfo callback) { if (MapClient.connected()) MapNetwork.request(); }

    @Inject(method = "renderPreDropdown", at = @At("HEAD"))
    private void jem$claims(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (lastViewedDimensionId == null) return;
        var screen = (GuiMap) (Object) this;
        MapClient.render(graphics, lastViewedDimensionId.location(), cameraX, cameraZ, scale / Minecraft.getInstance().getWindow().getGuiScale(), screen.width, screen.height);
    }

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void jem$unclaim(CallbackInfoReturnable<ArrayList<RightClickOption>> callback) {
        if (rightClickDim == null) return;
        var options = callback.getReturnValue();
        var event = MapClient.event(rightClickDim.location(), rightClickX, rightClickZ);
        if (event != null) {
            options.add(new RightClickOption("gui.jem_server.event_teleport", options.size(), (GuiMap) (Object) this) {
                @Override public void onAction(Screen screen) { Minecraft.getInstance().setScreen(null); MapNetwork.teleportEvent(event.id()); }
            });
        }
        var claim = MapClient.owned(rightClickDim.location(), rightClickX, rightClickZ);
        if (claim == null) return;
        options.add(new RightClickOption("gui.jem_server.unclaim", options.size(), (GuiMap) (Object) this) {
            @Override public void onAction(Screen screen) { Minecraft.getInstance().setScreen(null); MapNetwork.unclaim(claim.id()); }
        });
    }
}
