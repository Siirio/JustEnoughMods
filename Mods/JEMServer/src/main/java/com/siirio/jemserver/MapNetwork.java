package com.siirio.jemserver;

import com.siirio.jemserver.client.xaero.MapClient;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.siirio.jemserver.smp.SmpEnvironment;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jem_server")
public final class MapNetwork {
    private static final String VERSION = "8";
    private static final int MAX_EVENTS = 64;
    private static final int MAX_PROJECTS = 128;
    private static final int MAX_PROJECT_RECTANGLES = 64;
    private static final java.util.Map<UUID, List<EventArea>> LAST_EVENTS = new java.util.HashMap<>();
    private static final String LAST_REQUEST = "jem_server_map_request";
    private static final int REQUEST_INTERVAL = 20;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jem_server", "map"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(MapNetwork::compatible)
            .serverAcceptedVersions(MapNetwork::compatible).simpleChannel();
    public record Death(UUID id, ResourceLocation dimension, BlockPos position) {}
    public record EventArea(UUID id, String activity, ResourceLocation dimension, BlockPos position, int minX, int minZ, int maxX, int maxZ) {}
    public record ProjectRectangle(int minX, int minZ, int maxX, int maxZ) {}
    public record ProjectArea(UUID id, String name, ResourceLocation dimension, int color, List<ProjectRectangle> rectangles) {}
    public record SettlementArea(UUID id, String name, ResourceLocation dimension, BlockPos center, int minX, int minZ, int maxX, int maxZ, boolean owned) {
        public long area() { return (long) (maxX - minX + 1) * (maxZ - minZ + 1); }
    }
    public record Snapshot(List<Death> deaths, List<EventArea> events, List<ProjectArea> projects, List<SettlementArea> settlements) {}
    private record Events(List<EventArea> areas) {}
    private record Request() {}
    private record Publish(String name, ResourceLocation dimension, BlockPos position) {}
    private record EventTeleport(UUID id) {}
    private record BossTeleport(UUID id) {}
    private record ProjectCreate(String name, ResourceLocation dimension, int x1, int z1, int x2, int z2) {}
    private record ProjectExtend(UUID id, ResourceLocation dimension, int x1, int z1, int x2, int z2) {}
    private record SettlementExpand(UUID id, int targetX, int targetZ, UUID request) {}

    private MapNetwork() {}
    private static boolean compatible(String version) { return VERSION.equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version); }

    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(MapNetwork::encode).decoder(MapNetwork::decode)
                .consumerMainThread((packet, context) -> { DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MapClient.accept(packet)); context.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(Request.class, 1, NetworkDirection.PLAY_TO_SERVER).encoder((packet, out) -> {}).decoder(in -> new Request())
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player) && (player.server.getTickCount() < player.getPersistentData().getLong(LAST_REQUEST) || player.server.getTickCount() - player.getPersistentData().getLong(LAST_REQUEST) >= REQUEST_INTERVAL)) {
                        player.getPersistentData().putLong(LAST_REQUEST, player.server.getTickCount());
                        sync(player);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Publish.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, out) -> { out.writeUtf(packet.name(), 80); out.writeResourceLocation(packet.dimension()); out.writeBlockPos(packet.position()); })
                .decoder(in -> new Publish(in.readUtf(80), in.readResourceLocation(), in.readBlockPos()))
                .consumerMainThread((packet, context) -> { var player = context.get().getSender(); if (player != null && SmpEnvironment.active(player) && com.siirio.jemserver.smp.SmpRequests.allow(player)) Landmarks.publish(player, packet.name(), packet.dimension(), packet.position()); context.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(Events.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, out) -> encodeEvents(packet.areas(), out)).decoder(in -> new Events(decodeEvents(in)))
                .consumerMainThread((packet, context) -> { DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MapClient.acceptEvents(packet.areas())); context.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(EventTeleport.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, out) -> out.writeUUID(packet.id())).decoder(in -> new EventTeleport(in.readUUID()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player) && com.siirio.jemserver.smp.SmpRequests.allow(player)) com.siirio.jemserver.smp.events.EventTravel.request(player, packet.id(), false);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BossTeleport.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, out) -> out.writeUUID(packet.id())).decoder(in -> new BossTeleport(in.readUUID()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player) && com.siirio.jemserver.smp.SmpRequests.allow(player))
                        com.siirio.jemserver.smp.events.EventTravel.requestBoss(player, packet.id());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ProjectCreate.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, out) -> { out.writeUtf(packet.name(), 48); out.writeResourceLocation(packet.dimension()); out.writeInt(packet.x1()); out.writeInt(packet.z1()); out.writeInt(packet.x2()); out.writeInt(packet.z2()); })
                .decoder(in -> new ProjectCreate(in.readUtf(48), in.readResourceLocation(), in.readInt(), in.readInt(), in.readInt(), in.readInt()))
                .consumerMainThread((packet, context) -> { var player = context.get().getSender(); if (player != null && SmpEnvironment.active(player)
                        && player.serverLevel().dimension().location().equals(packet.dimension()) && com.siirio.jemserver.smp.SmpRequests.allow(player)) {
                    com.siirio.jemserver.map.ProjectRegionService.createWithRectangle(player, packet.name(), packet.x1(), packet.z1(), packet.x2(), packet.z2()); sync(player);
                } context.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(ProjectExtend.class, 9, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, out) -> { out.writeUUID(packet.id()); out.writeResourceLocation(packet.dimension()); out.writeInt(packet.x1()); out.writeInt(packet.z1()); out.writeInt(packet.x2()); out.writeInt(packet.z2()); })
                .decoder(in -> new ProjectExtend(in.readUUID(), in.readResourceLocation(), in.readInt(), in.readInt(), in.readInt(), in.readInt()))
                .consumerMainThread((packet, context) -> { var player = context.get().getSender(); if (player != null && SmpEnvironment.active(player)
                        && player.serverLevel().dimension().location().equals(packet.dimension()) && com.siirio.jemserver.smp.SmpRequests.allow(player)) {
                    com.siirio.jemserver.map.ProjectRegionService.addRectangle(player, packet.id(), packet.x1(), packet.z1(), packet.x2(), packet.z2()); sync(player);
                } context.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(SettlementExpand.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, out) -> { out.writeUUID(packet.id()); out.writeInt(packet.targetX()); out.writeInt(packet.targetZ()); out.writeUUID(packet.request()); })
                .decoder(in -> new SettlementExpand(in.readUUID(), in.readInt(), in.readInt(), in.readUUID()))
                .consumerMainThread((packet, context) -> { var player = context.get().getSender(); if (player != null && SmpEnvironment.active(player)
                        && com.siirio.jemserver.smp.SmpRequests.allow(player) && com.siirio.jemserver.smp.DomainFeatures.loaded("jem_settlements")) {
                    String result = com.siirio.jemserver.compat.settlements.SettlementHostAdapter.expand(player, packet.id(), packet.targetX(), packet.targetZ(), packet.request());
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.settlement.action." + result.toLowerCase(java.util.Locale.ROOT)), true);
                    sync(player);
                } context.get().setPacketHandled(true); }).add();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.getServer().getTickCount() % REQUEST_INTERVAL != 0) return;
        LandmarkNetwork.tick(event.getServer());
        var players = event.getServer().getPlayerList();
        LAST_EVENTS.keySet().removeIf(id -> players.getPlayer(id) == null);
        for (var player : players.getPlayers()) {
            if (!CHANNEL.isRemotePresent(player.connection.connection)) continue;
            var areas = eventAreas(player);
            if (areas.equals(LAST_EVENTS.get(player.getUUID()))) continue;
            LAST_EVENTS.put(player.getUUID(), areas);
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Events(areas));
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { LAST_EVENTS.clear(); LandmarkNetwork.clear(); }

    private static List<EventArea> eventAreas(ServerPlayer player) {
        long now = System.currentTimeMillis();
        var data = com.siirio.jemserver.smp.SmpData.get(player.server);
        var areas = new java.util.ArrayList<>(data.all("events").stream()
                .filter(row -> "ACTIVE".equals(row.getString("state")) && row.getLong("ends") > now
                        && row.hasUUID("id") && row.contains("position") && row.getInt("radius") > 0
                        && ResourceLocation.tryParse(row.getString("dimension")) != null)
                .filter(row -> java.util.Set.of("BLOOD_MOON", "BOSS_RAID", "RESOURCE_RUSH").contains(row.getString("activity")))
                .map(row -> new EventArea(row.getUUID("id"), row.getString("activity"),
                        new ResourceLocation(row.getString("dimension")), BlockPos.of(row.getLong("position")),
                        com.siirio.jemserver.smp.events.EventRegions.minX(row), com.siirio.jemserver.smp.events.EventRegions.minZ(row),
                        com.siirio.jemserver.smp.events.EventRegions.maxX(row), com.siirio.jemserver.smp.events.EventRegions.maxZ(row))).toList());
        data.all("parties").stream().filter(row -> "ACTIVE".equals(row.getString("state")) && row.hasUUID("structureId")
                        && row.contains("position") && row.getIntArray("structureBounds").length == 6
                        && ResourceLocation.tryParse(row.getString("dimension")) != null)
                .map(row -> {
                    int[] bounds = row.getIntArray("structureBounds");
                    return new EventArea(row.getUUID("structureId"), "BOSS_FIGHT", new ResourceLocation(row.getString("dimension")),
                            BlockPos.of(row.getLong("position")), bounds[0] - 1, bounds[2] - 1, bounds[3] + 1, bounds[5] + 1);
                }).forEach(areas::add);
        return areas.stream().distinct().limit(MAX_EVENTS).toList();
    }

    private static void encodeEvents(List<EventArea> areas, FriendlyByteBuf out) {
        out.writeCollection(areas, (buffer, area) -> {
            buffer.writeUUID(area.id()); buffer.writeUtf(area.activity(), 64); buffer.writeResourceLocation(area.dimension());
            buffer.writeBlockPos(area.position()); buffer.writeInt(area.minX()); buffer.writeInt(area.minZ()); buffer.writeInt(area.maxX()); buffer.writeInt(area.maxZ());
        });
    }

    private static List<EventArea> decodeEvents(FriendlyByteBuf in) {
        return in.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_EVENTS),
                buffer -> new EventArea(buffer.readUUID(), buffer.readUtf(64), buffer.readResourceLocation(), buffer.readBlockPos(),
                        buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
    }

    public static void request() { CHANNEL.sendToServer(new Request()); }
    public static void teleportEvent(UUID id) { CHANNEL.sendToServer(new EventTeleport(id)); }
    public static void teleportBoss(UUID id) { CHANNEL.sendToServer(new BossTeleport(id)); }
    public static void createProject(String name, ResourceLocation dimension, int x1, int z1, int x2, int z2) { CHANNEL.sendToServer(new ProjectCreate(name, dimension, x1, z1, x2, z2)); }
    public static void extendProject(UUID id, ResourceLocation dimension, int x1, int z1, int x2, int z2) { CHANNEL.sendToServer(new ProjectExtend(id, dimension, x1, z1, x2, z2)); }
    public static void expandSettlement(UUID id, int targetX, int targetZ) { CHANNEL.sendToServer(new SettlementExpand(id, targetX, targetZ, UUID.randomUUID())); }

    public static void sync(ServerPlayer player) {
        if (!SmpEnvironment.active(player) || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        var deaths = ServerData.get(player.server).deaths(player.getUUID()).stream().map(death -> new Death(death.id(), death.dimension(), BlockPos.containing(death.x(), death.y(), death.z()))).toList();
        var principal = com.siirio.jemidentity.api.IdentityPolicy.resolve(player);
        var projects = principal.isEmpty() ? java.util.List.<ProjectArea>of()
                : com.siirio.jemserver.map.ProjectRegionData.get(player.server).all().stream()
                        .filter(project -> project.owner().equals(principal.get().accountId())
                                || project.collaborators().contains(principal.get().accountId()))
                        .limit(MAX_PROJECTS)
                        .map(project -> new ProjectArea(project.id(), project.name(), project.dimension(), project.color(),
                                project.rectangles().stream().limit(MAX_PROJECT_RECTANGLES)
                                        .map(rectangle -> new ProjectRectangle(rectangle.minX(), rectangle.minZ(), rectangle.maxX(), rectangle.maxZ()))
                                        .toList()))
                        .toList();
        List<SettlementArea> settlements = com.siirio.jemserver.smp.DomainFeatures.loaded("jem_settlements")
                ? com.siirio.jemserver.compat.settlements.SettlementHostAdapter.areas(player) : List.of();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(deaths, eventAreas(player), projects, settlements));
    }

    private static void encode(Snapshot packet, FriendlyByteBuf out) {
        out.writeCollection(packet.deaths(), (buffer, death) -> { buffer.writeUUID(death.id()); buffer.writeResourceLocation(death.dimension()); buffer.writeBlockPos(death.position()); });
        encodeEvents(packet.events(), out);
        out.writeCollection(packet.projects(), (buffer, project) -> {
            buffer.writeUUID(project.id()); buffer.writeUtf(project.name(), 48); buffer.writeResourceLocation(project.dimension()); buffer.writeInt(project.color());
            buffer.writeCollection(project.rectangles(), (rectangleBuffer, rectangle) -> {
                rectangleBuffer.writeInt(rectangle.minX()); rectangleBuffer.writeInt(rectangle.minZ());
                rectangleBuffer.writeInt(rectangle.maxX()); rectangleBuffer.writeInt(rectangle.maxZ());
            });
        });
        out.writeCollection(packet.settlements(), (buffer, settlement) -> { buffer.writeUUID(settlement.id()); buffer.writeUtf(settlement.name(), 32);
            buffer.writeResourceLocation(settlement.dimension()); buffer.writeBlockPos(settlement.center()); buffer.writeInt(settlement.minX()); buffer.writeInt(settlement.minZ()); buffer.writeInt(settlement.maxX()); buffer.writeInt(settlement.maxZ()); buffer.writeBoolean(settlement.owned()); });
    }

    private static Snapshot decode(FriendlyByteBuf in) {
        return new Snapshot(in.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, 64), buffer -> new Death(buffer.readUUID(), buffer.readResourceLocation(), buffer.readBlockPos())), decodeEvents(in),
                in.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_PROJECTS), buffer -> new ProjectArea(
                        buffer.readUUID(), buffer.readUtf(48), buffer.readResourceLocation(), buffer.readInt(),
                        buffer.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_PROJECT_RECTANGLES), rectangleBuffer ->
                                new ProjectRectangle(rectangleBuffer.readInt(), rectangleBuffer.readInt(), rectangleBuffer.readInt(), rectangleBuffer.readInt())))),
                in.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, 256), buffer -> new SettlementArea(buffer.readUUID(), buffer.readUtf(32),
                        buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean())));
    }
}
