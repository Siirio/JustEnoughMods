package com.siirio.jemserver;

import com.siirio.jemserver.client.xaero.MapClient;
import com.siirio.jemserver.claims.Claims;
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

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jem_server", value = Dist.DEDICATED_SERVER)
public final class MapNetwork {
    private static final String VERSION = "3";
    private static final int MAX_EVENTS = 64;
    private static List<EventArea> lastEvents = List.of();
    private static final int MAX_CLAIMS = 16384;
    private static final String LAST_REQUEST = "jem_server_map_request";
    private static final int REQUEST_INTERVAL = 20;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jem_server", "map"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(MapNetwork::compatible)
            .serverAcceptedVersions(MapNetwork::compatible).simpleChannel();
    public record Claim(UUID id, UUID owner, String name, ResourceLocation dimension, int minX, int minZ, int maxX, int maxZ, int color) {}
    public record Death(UUID id, ResourceLocation dimension, BlockPos position) {}
    public record EventArea(UUID id, String activity, ResourceLocation dimension, BlockPos position, int radius) {}
    public record Snapshot(List<Claim> claims, List<Death> deaths, List<EventArea> events) {}
    private record Events(List<EventArea> areas) {}
    private record Request() {}
    private record Publish(String name, ResourceLocation dimension, BlockPos position) {}
    private record Teleport(UUID id) {}
    private record Unclaim(UUID id) {}
    private record EventTeleport(UUID id) {}

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
        CHANNEL.messageBuilder(Teleport.class, 3, NetworkDirection.PLAY_TO_SERVER).encoder((packet, out) -> out.writeUUID(packet.id())).decoder(in -> new Teleport(in.readUUID()))
                .consumerMainThread((packet, context) -> { var player = context.get().getSender(); if (player != null && SmpEnvironment.active(player) && com.siirio.jemserver.smp.SmpRequests.allow(player)) DeathHistory.teleport(player, packet.id()); context.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(Unclaim.class, 4, NetworkDirection.PLAY_TO_SERVER).encoder((packet, out) -> out.writeUUID(packet.id())).decoder(in -> new Unclaim(in.readUUID()))
                .consumerMainThread((packet, context) -> { var player = context.get().getSender(); if (player != null && SmpEnvironment.active(player) && com.siirio.jemserver.smp.SmpRequests.allow(player)) Claims.confirmDelete(player, packet.id()); context.get().setPacketHandled(true); }).add();
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
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.getServer().getTickCount() % REQUEST_INTERVAL != 0) return;
        var areas = eventAreas(event.getServer());
        if (areas.equals(lastEvents)) return;
        lastEvents = areas;
        for (var player : event.getServer().getPlayerList().getPlayers())
            if (CHANNEL.isRemotePresent(player.connection.connection))
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Events(areas));
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { lastEvents = List.of(); }

    public static List<EventArea> eventAreas(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        return com.siirio.jemserver.smp.SmpData.get(server).all("events").stream()
                .filter(row -> "ACTIVE".equals(row.getString("state")) && row.getLong("ends") > now
                        && row.hasUUID("id") && row.contains("position") && row.getInt("radius") > 0
                        && ResourceLocation.tryParse(row.getString("dimension")) != null)
                .limit(MAX_EVENTS).map(row -> new EventArea(row.getUUID("id"), row.getString("activity"),
                        new ResourceLocation(row.getString("dimension")), BlockPos.of(row.getLong("position")), row.getInt("radius"))).toList();
    }

    private static void encodeEvents(List<EventArea> areas, FriendlyByteBuf out) {
        out.writeCollection(areas, (buffer, area) -> {
            buffer.writeUUID(area.id()); buffer.writeUtf(area.activity(), 64); buffer.writeResourceLocation(area.dimension());
            buffer.writeBlockPos(area.position()); buffer.writeVarInt(area.radius());
        });
    }

    private static List<EventArea> decodeEvents(FriendlyByteBuf in) {
        return in.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_EVENTS),
                buffer -> new EventArea(buffer.readUUID(), buffer.readUtf(64), buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readVarInt()));
    }

    public static void request() { CHANNEL.sendToServer(new Request()); }
    public static void publish(String name, ResourceLocation dimension, BlockPos position) { CHANNEL.sendToServer(new Publish(name, dimension, position)); }
    public static void teleport(UUID id) { CHANNEL.sendToServer(new Teleport(id)); }
    public static void unclaim(UUID id) { CHANNEL.sendToServer(new Unclaim(id)); }
    public static void teleportEvent(UUID id) { CHANNEL.sendToServer(new EventTeleport(id)); }

    public static void sync(ServerPlayer player) {
        if (!SmpEnvironment.active(player) || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        var claims = Claims.mapTerritories(player).stream().limit(MAX_CLAIMS)
                .map(claim -> new Claim(claim.id(), claim.owner(), claim.name(), claim.dimension(), claim.minX(), claim.minZ(), claim.maxX(), claim.maxZ(), claim.color())).toList();
        var deaths = ServerData.get(player.server).deaths(player.getUUID()).stream().map(death -> new Death(death.id(), death.dimension(), BlockPos.containing(death.x(), death.y(), death.z()))).toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(claims, deaths, eventAreas(player.server)));
    }

    private static void encode(Snapshot packet, FriendlyByteBuf out) {
        out.writeCollection(packet.claims(), (buffer, claim) -> {
            buffer.writeUUID(claim.id()); buffer.writeUUID(claim.owner()); buffer.writeUtf(claim.name(), 256); buffer.writeResourceLocation(claim.dimension());
            buffer.writeInt(claim.minX()); buffer.writeInt(claim.minZ()); buffer.writeInt(claim.maxX()); buffer.writeInt(claim.maxZ()); buffer.writeInt(claim.color());
        });
        out.writeCollection(packet.deaths(), (buffer, death) -> { buffer.writeUUID(death.id()); buffer.writeResourceLocation(death.dimension()); buffer.writeBlockPos(death.position()); });
        encodeEvents(packet.events(), out);
    }

    private static Snapshot decode(FriendlyByteBuf in) {
        return new Snapshot(in.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_CLAIMS), buffer -> new Claim(buffer.readUUID(), buffer.readUUID(), buffer.readUtf(256), buffer.readResourceLocation(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt())),
                in.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, 64), buffer -> new Death(buffer.readUUID(), buffer.readResourceLocation(), buffer.readBlockPos())), decodeEvents(in));
    }
}
