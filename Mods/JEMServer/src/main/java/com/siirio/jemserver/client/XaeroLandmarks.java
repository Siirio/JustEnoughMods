package com.siirio.jemserver.client;

import com.siirio.jemserver.Landmarks;
import com.siirio.jemserver.ServerData;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

public final class XaeroLandmarks {
    private static final ResourceLocation LEGACY_ORIGIN = new ResourceLocation("jem_server", "landmarks");
    private static final ResourceLocation BOSS_ORIGIN = new ResourceLocation("jem_server", "boss_discoveries");
    private static final ResourceLocation PERSONAL_ORIGIN = new ResourceLocation("jem_server", "personal_landmarks");
    private static final ResourceLocation EVENT_ORIGIN = new ResourceLocation("jem_server", "current_events");
    private static final String SYMBOL = "◆";
    private record Applied(int revision, ResourceLocation dimension) {}
    private static final Map<MinimapWorldContainer, Applied> APPLIED = new IdentityHashMap<>();
    private record Shared(java.util.UUID id, MinimapWorldContainer container) {}
    private static final Map<Waypoint, Shared> SHARED_IDS = new IdentityHashMap<>();
    private static final Map<Waypoint, Shared> EVENT_IDS = new IdentityHashMap<>();
    private static List<com.siirio.jemserver.MapNetwork.EventArea> events = List.of();
    private static List<ServerData.Landmark> snapshot;
    private static int revision;
    private static MinimapWorldRootContainer preparedRoot;
    private static int preparedRevision;
    private static boolean mapPrepared;

    private XaeroLandmarks() {}

    public static void showPrivateHome(ResourceLocation dimension, net.minecraft.core.BlockPos position) {
        if (dimension == null || position == null) return;
        com.siirio.jemserver.client.smp.TemporaryWaypoints.showPreview(
                java.util.UUID.nameUUIDFromBytes(("jem:private-home:" + dimension + ":" + position.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                Component.translatable("jem.smp.home.marker").getString(), dimension, position);
        if (!com.siirio.jemserver.client.xaero.XaeroMapPreview.open(dimension, position)) {
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable("jem.smp.home.map_unavailable", dimension.toString(),
                    position.getX(), position.getY(), position.getZ()), false);
        }
    }

    public static ServerData.Landmark shared(Object waypoint) {
        Shared shared = SHARED_IDS.get(waypoint);
        return shared == null || snapshot == null ? null : snapshot.stream().filter(value -> value.id().equals(shared.id())).findFirst().orElse(null);
    }

    public static java.util.UUID event(Object waypoint) {
        Shared shared = EVENT_IDS.get(waypoint);
        return shared == null ? null : events.stream().filter(value -> value.id().equals(shared.id())).map(com.siirio.jemserver.MapNetwork.EventArea::id).findFirst().orElse(null);
    }

    public static void acceptEvents(List<com.siirio.jemserver.MapNetwork.EventArea> update) {
        events = update.stream().filter(value -> java.util.Set.of("BLOOD_MOON", "BOSS_RAID", "RESOURCE_RUSH").contains(value.activity())).toList();
        revision++;
        mapPrepared = false;
        synchronize();
    }

    static void accept(List<ServerData.Landmark> landmarks) {
        snapshot = List.copyOf(landmarks);
        revision++;
        mapPrepared = false;
        synchronize();
    }

    static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        snapshot = null;
        APPLIED.keySet().forEach(XaeroLandmarks::clearShared);
        APPLIED.clear();
        SHARED_IDS.clear();
        EVENT_IDS.clear();
        events = List.of();
        revision = 0;
        preparedRoot = null;
        preparedRevision = 0;
        mapPrepared = false;
    }

    static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || snapshot == null || Minecraft.getInstance().level == null) return;
        synchronize();
    }

    private static void synchronize() {
        if (Minecraft.getInstance().player == null || snapshot == null) return;
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) return;
        var manager = session.getWorldManager();
        var root = manager.getAutoRootContainer();
        if (root == null || !root.isConfigLoaded()) return;
        if (root == preparedRoot && revision == preparedRevision) {
            if (!mapPrepared && net.minecraftforge.fml.ModList.get().isLoaded("xaeroworldmap")) mapPrepared = refreshMapWaypoints();
            return;
        }
        if (root != preparedRoot) {
            APPLIED.keySet().forEach(XaeroLandmarks::clearShared);
            APPLIED.clear();
            mapPrepared = false;
        }
        for (var container : root.getSubContainers()) {
            if (container.getThirdPartyWaypointManager() != null) apply(container, container.getEquivalentDimId());
        }
        java.util.stream.Stream.concat(snapshot.stream().map(ServerData.Landmark::dimension), events.stream().map(com.siirio.jemserver.MapNetwork.EventArea::dimension)).distinct().forEach(dimension -> {
            var key = ResourceKey.create(Registries.DIMENSION, dimension);
            var path = root.getPath().resolve(session.getDimensionHelper().getDimensionDirectoryName(key));
            apply(manager.addWorldContainer(path), dimension);
        });
        preparedRoot = root;
        preparedRevision = revision;
        if (net.minecraftforge.fml.ModList.get().isLoaded("xaeroworldmap")) mapPrepared = refreshMapWaypoints();
    }

    private static boolean refreshMapWaypoints() {
        var session = xaero.map.WorldMapSession.getCurrentSession();
        if (session == null || !session.isUsable()) return false;
        if (session.getMapProcessor().getMapWorld() == null || xaero.map.mods.SupportMods.xaeroMinimap == null) return false;
        xaero.map.mods.SupportMods.xaeroMinimap.requestWaypointsRefresh();
        return true;
    }

    private static void apply(MinimapWorldContainer container, ResourceLocation dimension) {
        var applied = APPLIED.get(container);
        if (applied != null && applied.revision() == revision && applied.dimension().equals(dimension)) return;
        clearShared(container);
        var player = Minecraft.getInstance().player;
        var manager = container.getThirdPartyWaypointManager();
        for (var landmark : snapshot) {
            if (!landmark.dimension().equals(dimension)) continue;
            boolean boss = Landmarks.isBoss(landmark);
            if (!boss && (player == null || !landmark.creator().equals(player.getUUID()))) continue;
            var pos = landmark.position();
            String name = landmark.nameKey().isEmpty() ? landmark.name() : Component.translatable(landmark.nameKey()).getString();
            Waypoint waypoint = new Waypoint(pos.getX(), pos.getY(), pos.getZ(), name, SYMBOL, boss ? WaypointColor.RED : color(landmark.category()));
            waypoint.setTemporary(true);
            manager.get(boss ? BOSS_ORIGIN : PERSONAL_ORIGIN).add(landmark.id().toString(), waypoint);
            SHARED_IDS.put(waypoint, new Shared(landmark.id(), container));
        }
        for (var event : events) {
            if (!event.dimension().equals(dimension)) continue;
            var pos = event.position();
            var waypoint = new Waypoint(pos.getX(), pos.getY(), pos.getZ(),
                    Component.translatable("jem.smp." + event.activity()).getString(), SYMBOL, WaypointColor.GOLD);
            waypoint.setTemporary(true);
            manager.get(EVENT_ORIGIN).add(event.id().toString(), waypoint);
            EVENT_IDS.put(waypoint, new Shared(event.id(), container));
        }
        APPLIED.put(container, new Applied(revision, dimension));
    }

    private static void clearShared(MinimapWorldContainer container) {
        for (var world : container.getAllWorldsIterable()) {
            for (var set : world.getIterableWaypointSets()) {
                java.util.List<Waypoint> shared = new java.util.ArrayList<>();
                for (Waypoint waypoint : set.getWaypoints()) {
                    var origin = waypoint.getThirdPartyOrigin();
                    if (LEGACY_ORIGIN.equals(origin) || BOSS_ORIGIN.equals(origin) || PERSONAL_ORIGIN.equals(origin) || EVENT_ORIGIN.equals(origin)) shared.add(waypoint);
                }
                set.removeAll(shared);
            }
        }
        var manager = container.getThirdPartyWaypointManager();
        manager.clearOrigin(LEGACY_ORIGIN);
        manager.clearOrigin(BOSS_ORIGIN);
        manager.clearOrigin(PERSONAL_ORIGIN);
        manager.clearOrigin(EVENT_ORIGIN);
        SHARED_IDS.entrySet().removeIf(entry -> entry.getValue().container() == container);
        EVENT_IDS.entrySet().removeIf(entry -> entry.getValue().container() == container);
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
