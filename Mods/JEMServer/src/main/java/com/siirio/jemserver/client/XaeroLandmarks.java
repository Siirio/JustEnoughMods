package com.siirio.jemserver.client;

import com.siirio.jemserver.ServerData;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.world.MinimapWorld;

public final class XaeroLandmarks {
    private static final net.minecraft.resources.ResourceLocation ORIGIN = new net.minecraft.resources.ResourceLocation("jem_server", "landmarks");
    private record Applied(int revision, net.minecraft.resources.ResourceLocation dimension) {}
    private static final Map<xaero.hud.minimap.waypoint.set.WaypointSet, Applied> APPLIED = new IdentityHashMap<>();
    private record Shared(java.util.UUID id, xaero.hud.minimap.waypoint.set.WaypointSet set) {}
    private static final Map<Waypoint, Shared> SHARED_IDS = new IdentityHashMap<>();
    private static final Map<Waypoint, MinimapWorld> PENDING = new IdentityHashMap<>();
    private static final int MAX_PENDING_PUBLICATIONS = 32;

    public static void publishing(MinimapWorld world, Waypoint waypoint) {
        if (PENDING.size() >= MAX_PENDING_PUBLICATIONS) PENDING.clear();
        PENDING.put(waypoint, world);
    }
    private static List<ServerData.Landmark> snapshot;
    private static int revision;

    private XaeroLandmarks() {}

    public static ServerData.Landmark shared(Object waypoint) {
        Shared shared = SHARED_IDS.get(waypoint);
        return shared == null || snapshot == null ? null : snapshot.stream().filter(value -> value.id().equals(shared.id())).findFirst().orElse(null);
    }

    static void accept(List<ServerData.Landmark> landmarks) {
        snapshot = List.copyOf(landmarks);
        revision++;
        APPLIED.replaceAll((set, applied) -> {
            apply(set, applied.dimension());
            return new Applied(revision, applied.dimension());
        });
        var player = Minecraft.getInstance().player;
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (player == null || session == null) return;
        PENDING.entrySet().removeIf(entry -> {
            var waypoint = entry.getKey();
            var world = entry.getValue();
            boolean published = landmarks.stream().anyMatch(landmark -> landmark.creator().equals(player.getUUID()) && landmark.name().equals(waypoint.getName())
                    && world.getDimId() != null && landmark.dimension().equals(world.getDimId().location()) && landmark.position().equals(new net.minecraft.core.BlockPos(waypoint.getX(), waypoint.getY(), waypoint.getZ())));
            if (!published) return false;
            for (var set : world.getIterableWaypointSets()) set.remove(waypoint);
            try { session.getWorldManagerIO().saveWorld(world); }
            catch (java.io.IOException failure) { com.mojang.logging.LogUtils.getLogger().warn("Could not save published waypoint cleanup", failure); }
            return true;
        });
    }

    static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        snapshot = null;
        APPLIED.keySet().forEach(XaeroLandmarks::clearShared);
        APPLIED.clear();
        PENDING.clear();
        revision = 0;
    }

    static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || snapshot == null || Minecraft.getInstance().level == null) return;
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) return;
        var manager = session.getWorldManager();
        var current = manager.getAutoWorld();
        if (current == null) return;
        var set = current.getCurrentWaypointSet();
        if (set == null || APPLIED.containsKey(set) && APPLIED.get(set).revision() == revision || current.getDimId() == null) return;
        apply(set, current.getDimId().location());
        if (APPLIED.size() >= MAX_PENDING_PUBLICATIONS && !APPLIED.containsKey(set)) {
            var oldest = APPLIED.keySet().iterator().next();
            clearShared(oldest);
            APPLIED.remove(oldest);
        }
        APPLIED.put(set, new Applied(revision, current.getDimId().location()));
    }

    private static void apply(xaero.hud.minimap.waypoint.set.WaypointSet set, net.minecraft.resources.ResourceLocation dimension) {
        clearShared(set);
        for (var landmark : snapshot) {
            if (!landmark.dimension().equals(dimension)) continue;
            var pos = landmark.position();
            String name = landmark.nameKey().isEmpty() ? landmark.name() : Component.translatable(landmark.nameKey()).getString();
            Waypoint waypoint = new Waypoint(pos.getX(), pos.getY(), pos.getZ(), name, "◆", color(landmark.category()));
            waypoint.setThirdPartyOrigin(ORIGIN);
            waypoint.setTemporary(true);
            SHARED_IDS.put(waypoint, new Shared(landmark.id(), set));
            set.add(waypoint);
        }
    }

    private static void clearShared(xaero.hud.minimap.waypoint.set.WaypointSet set) {
        java.util.List<Waypoint> shared = new java.util.ArrayList<>();
        for (Waypoint waypoint : set.getWaypoints()) if (ORIGIN.equals(waypoint.getThirdPartyOrigin())) shared.add(waypoint);
        SHARED_IDS.entrySet().removeIf(entry -> entry.getValue().set() == set);
        set.removeAll(shared);
    }

    private static WaypointColor color(String category) {
        return switch (category) {
            case "scenery" -> WaypointColor.GREEN;
            case "building" -> WaypointColor.AQUA;
            case "shop" -> WaypointColor.GOLD;
            case "danger" -> WaypointColor.RED;
            default -> WaypointColor.PURPLE;
        };
    }
}
