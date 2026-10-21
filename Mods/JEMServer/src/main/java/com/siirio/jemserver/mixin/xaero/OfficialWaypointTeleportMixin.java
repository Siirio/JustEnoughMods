package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.client.xaero.OfficialMarkerTravel;
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
public abstract class OfficialWaypointTeleportMixin {
    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void jem$officialTeleport(Waypoint waypoint, MinimapWorld world, Screen screen, boolean confirm, CallbackInfo callback) {
        if (OfficialMarkerTravel.request(waypoint)) callback.cancel();
    }
}
