package com.siirio.jemtwelveeyes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.world.MapWorld;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

public final class XaeroBossMapScreen {
    private static final String MARKER_SYMBOL = "B";

    private XaeroBossMapScreen() {
    }

    public static void open(int x, int y, int z, ResourceLocation dimension, String markerNameKey) {
        Minecraft minecraft = Minecraft.getInstance();
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (minecraft.player == null || session == null || !session.isUsable()) {
            unavailable(minecraft);
            return;
        }
        MapProcessor processor = session.getMapProcessor();
        MapWorld mapWorld = processor.getMapWorld();
        if (mapWorld == null) {
            unavailable(minecraft);
            return;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimension);
        if (mapWorld.getDimension(dimensionKey) == null) {
            mapWorld.createDimensionUnsynced(dimensionKey);
        }
        createMarker(x, y, z, dimensionKey, markerNameKey);
        mapWorld.setFutureDimensionId(dimensionKey);
        GuiMap screen = new GuiMap(minecraft.screen, minecraft.screen, processor, minecraft.player);
        if (!center(screen, x, z)) {
            unavailable(minecraft);
            return;
        }
        minecraft.setScreen(screen);
    }

    private static void createMarker(int x, int y, int z, ResourceKey<Level> dimension, String markerNameKey) {
        var minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) {
            return;
        }
        var root = minimapSession.getWorldManager().getCurrentRootContainer();
        if (root == null) {
            return;
        }
        MinimapWorld targetWorld = null;
        for (MinimapWorld world : root.getAllWorldsIterable()) {
            if (dimension.equals(world.getDimId())) {
                targetWorld = world;
                break;
            }
        }
        if (targetWorld == null) {
            return;
        }
        var waypointSet = targetWorld.getCurrentWaypointSet();
        var stale = new ArrayList<Waypoint>();
        for (Waypoint waypoint : waypointSet.getWaypoints()) {
            if (markerNameKey.equals(waypoint.getName())) {
                stale.add(waypoint);
            }
        }
        waypointSet.removeAll(stale);
        waypointSet.add(new Waypoint(x, y, z, markerNameKey, MARKER_SYMBOL, WaypointColor.LIGHT_BLUE));
        try {
            minimapSession.getWorldManagerIO().saveWorld(targetWorld);
        } catch (IOException ignored) {
        }
    }

    private static boolean center(GuiMap screen, int x, int z) {
        try {
            set(screen, "cameraX", x);
            set(screen, "cameraZ", z);
            set(screen, "shouldResetCameraPos", false);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static void set(GuiMap screen, String name, Object value) throws ReflectiveOperationException {
        Field field = GuiMap.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(screen, value);
    }

    private static void unavailable(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.jemcompat.xaero_unavailable"), false);
        }
    }
}
