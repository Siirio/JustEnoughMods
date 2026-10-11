package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.client.xaero.MapClient;
import com.siirio.jemserver.client.xaero.MapView;

import com.siirio.jemserver.MapNetwork;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.gui.Waypoint;

@Pseudo
@Mixin(targets = "xaero.map.mods.gui.WaypointReader", remap = false)
public abstract class WaypointMenuMixin {
    @Inject(method = "getRightClickOptions(Lxaero/map/mods/gui/Waypoint;Lxaero/map/gui/IRightClickableElement;)Ljava/util/ArrayList;", at = @At("RETURN"))
    private void jem$actions(Waypoint waypoint, IRightClickableElement target, CallbackInfoReturnable<ArrayList<RightClickOption>> callback) {
        if (!(Minecraft.getInstance().screen instanceof GuiMap map)) return;
        var dimension = ((MapView) map).jem$dimension();
        if (dimension == null) return;
        var options = callback.getReturnValue();
        var landmark = com.siirio.jemserver.client.XaeroLandmarks.shared(waypoint.getOriginal());
        if (landmark != null) {
            var player = Minecraft.getInstance().player;
            if (player != null && !com.siirio.jemserver.Landmarks.isBoss(landmark) && landmark.creator().equals(player.getUUID())) {
                options.add(new RightClickOption("gui.jem_server.remove_personal", options.size(), target) {
                    @Override public void onAction(Screen screen) {
                        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(confirmed -> {
                            if (confirmed) com.siirio.jemserver.LandmarkNetwork.remove(landmark.id());
                            Minecraft.getInstance().setScreen(screen);
                        }, Component.translatable("gui.jem_server.remove_personal"),
                                Component.translatable("gui.jem_server.remove_personal_confirm", landmark.name())));
                    }
                });
            }
        }
        if (!MapClient.connected()) return;
        Component teleport = Component.translatable("gui.xaero_right_click_waypoint_teleport");
        options.removeIf(option -> option.getDisplayName().getString().equals(teleport.getString()));
        if (landmark != null && com.siirio.jemserver.Landmarks.isBoss(landmark))
            options.add(new RightClickOption("gui.xaero_right_click_waypoint_teleport", options.size(), target) {
                @Override public void onAction(Screen screen) { Minecraft.getInstance().setScreen(null); MapNetwork.teleportBoss(landmark.id()); }
            });
    }
}
