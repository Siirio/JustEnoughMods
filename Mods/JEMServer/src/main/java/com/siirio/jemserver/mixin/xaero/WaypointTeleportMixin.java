package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.client.xaero.MapClient;

import com.siirio.jemserver.MapNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.world.MinimapWorld;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.waypoint.WaypointTeleport", remap = false)
public abstract class WaypointTeleportMixin {
    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"), cancellable = true)
    private void jem$official(Waypoint waypoint, MinimapWorld world, Screen screen, boolean confirm, CallbackInfo callback) {
        if (waypoint == null || !MapClient.connected()) return;
        callback.cancel();
        var landmark = com.siirio.jemserver.client.XaeroLandmarks.shared(waypoint);
        if (landmark != null && com.siirio.jemserver.Landmarks.isBoss(landmark)) {
            MapNetwork.teleportBoss(landmark.id());
            Minecraft.getInstance().setScreen(null);
            return;
        }
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error.official_waypoint_only"), true);
    }
}
