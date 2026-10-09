package com.siirio.jemserver.mixin.xaero;

import com.siirio.jemserver.client.xaero.MapClient;

import com.siirio.jemserver.MapNetwork;
import java.util.ArrayList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypointWorlds;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.path.XaeroPath;
import xaero.lib.client.config.ClientConfigManager;

@Pseudo
@Mixin(targets = "xaero.common.gui.GuiAddWaypoint", remap = false)
public abstract class WaypointEditorMixin extends Screen {
    @Shadow private boolean adding;
    @Shadow private ArrayList<Waypoint> waypointsEdited;
    @Shadow private MinimapWorldManager manager;
    @Shadow private GuiWaypointWorlds worlds;
    @Unique private boolean jem$shared;
    protected WaypointEditorMixin(Component title) { super(title); }

    @Inject(method = "m_7856_", at = @At("TAIL"))
    private void jem$visibility(CallbackInfo callback) {
        if (!adding || !MapClient.connected()) return;
        addRenderableWidget(Button.builder(Component.translatable(jem$shared ? "gui.jem_server.shared" : "gui.jem_server.private"), button -> {
            jem$shared = !jem$shared;
            button.setMessage(Component.translatable(jem$shared ? "gui.jem_server.shared" : "gui.jem_server.private"));
        }).bounds(8, 8, 150, 20).build());
    }

    @Inject(method = "lambda$init$5", at = @At("TAIL"))
    private void jem$publish(ClientConfigManager config, Button button, CallbackInfo callback) {
        if (!adding || !jem$shared || !MapClient.connected()) return;
        var world = manager.getWorld((XaeroPath) worlds.getCurrentKey());
        if (world == null || world.getDimId() == null) return;
        for (var waypoint : waypointsEdited) {
            if (!waypoint.isThirdParty()) {
                com.siirio.jemserver.client.XaeroLandmarks.publishing(world, waypoint);
                MapNetwork.publish(waypoint.getName(), world.getDimId().location(), new BlockPos(waypoint.getX(), waypoint.getY(), waypoint.getZ()));
            }
        }
        jem$shared = false;
    }
}
