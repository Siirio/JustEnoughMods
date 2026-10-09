package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.NavigationPoint;
import com.siirio.jemserver.smp.NavigationPoints;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;

import java.util.*;

public final class TemporaryWaypoints {
    private static final ResourceLocation ORIGIN = new ResourceLocation("jem_server", "temporary");
    private static List<NavigationPoint> points = List.of();
    private static final Map<xaero.hud.minimap.waypoint.set.WaypointSet, Integer> APPLIED =
            new IdentityHashMap<>();
    private static int revision;
    private static NavigationPoint localPoint;
    private static boolean localMapOpened;

    public static void accept(NavigationPoints update) {
        points = List.copyOf(update.entries());
        revision++;
    }

    public static void showPreview(UUID id, String label, ResourceLocation dimension, net.minecraft.core.BlockPos position) {
        localPoint = new NavigationPoint("shops", id, label, dimension, position, Long.MAX_VALUE);
        localMapOpened = false;
        revision++;
    }

    public static void clearLocal() {
        if (localPoint == null) return;
        localPoint = null;
        localMapOpened = false;
        revision++;
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (ModList.get().isLoaded("xaerominimap"))
            APPLIED.keySet().forEach(TemporaryWaypoints::clear);
        APPLIED.clear();
        points = List.of();
        localPoint = null;
        localMapOpened = false;
        revision++;
    }

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().level == null || !ModList.get().isLoaded("xaerominimap")) return;
        boolean localChanged = false;
        if (localPoint != null) {
            if (Minecraft.getInstance().screen instanceof xaero.map.gui.GuiMap) localMapOpened = true;
            else if (localMapOpened) {
                localPoint = null;
                localMapOpened = false;
                revision++;
                localChanged = true;
            }
        }
        if (!localChanged && Minecraft.getInstance().level.getGameTime() % 20 != 0) return;
        long now = System.currentTimeMillis();
        if (points.stream().anyMatch(p -> p.expires() <= now)) {
            points = points.stream().filter(p -> p.expires() > now).toList();
            revision++;
        }
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) return;
        var world = session.getWorldManager().getAutoWorld();
        if (world == null || world.getDimId() == null) return;
        var set = world.getCurrentWaypointSet();
        if (set == null || APPLIED.getOrDefault(set, -1) == revision) return;
        clear(set);
        for (var point : points) {
            if (!point.dimension().equals(world.getDimId().location())) continue;
            var pos = point.position();
            var waypoint =
                    new Waypoint(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            point.label(),
                            "◆",
                            WaypointColor.GOLD);
            waypoint.setThirdPartyOrigin(ORIGIN);
            waypoint.setTemporary(true);
            set.add(waypoint);
        }
        if (localPoint != null && localPoint.dimension().equals(world.getDimId().location())) {
            var pos = localPoint.position();
            var waypoint = new Waypoint(pos.getX(), pos.getY(), pos.getZ(), localPoint.label(), "◆", WaypointColor.GOLD);
            waypoint.setThirdPartyOrigin(ORIGIN);
            waypoint.setTemporary(true);
            set.add(waypoint);
        }
        if (APPLIED.size() >= 64 && !APPLIED.containsKey(set)) {
            var oldest = APPLIED.keySet().iterator().next();
            clear(oldest);
            APPLIED.remove(oldest);
        }
        APPLIED.put(set, revision);
    }

    private static void clear(xaero.hud.minimap.waypoint.set.WaypointSet set) {
        var remove = new ArrayList<Waypoint>();
        for (var waypoint : set.getWaypoints())
            if (ORIGIN.equals(waypoint.getThirdPartyOrigin())) remove.add(waypoint);
        set.removeAll(remove);
    }

    private TemporaryWaypoints() {}
}
