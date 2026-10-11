package com.siirio.jemserver;

import com.siirio.jemserver.client.LandmarkClient;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.siirio.jemserver.smp.SmpEnvironment;

public final class LandmarkNetwork {
    private static final java.util.Map<java.util.UUID, java.util.Deque<Snapshot>> PENDING = new java.util.HashMap<>();
    private static final String PROTOCOL = "4";
    private static final int MAX_SNAPSHOT = 4096;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("jem_server", "landmarks"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(LandmarkNetwork::compatible)
            .serverAcceptedVersions(LandmarkNetwork::compatible)
            .simpleChannel();

    private LandmarkNetwork() {}

    private static boolean compatible(String version) {
        return PROTOCOL.equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version);
    }

    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode).consumerMainThread(Snapshot::handle).add();
        CHANNEL.messageBuilder(Remove.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, out) -> out.writeUUID(packet.id())).decoder(in -> new Remove(in.readUUID()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player) && com.siirio.jemserver.smp.SmpRequests.allow(player)) Landmarks.delete(player, packet.id());
                    context.get().setPacketHandled(true);
                }).add();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LandmarkClient::register);
    }

    private record Remove(java.util.UUID id) {}

    public static void remove(java.util.UUID id) { CHANNEL.sendToServer(new Remove(id)); }

    public static void sync(ServerPlayer player) {
        if (!SmpEnvironment.active(player) || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        List<ServerData.Landmark> landmarks = ServerConfig.LANDMARKS_ENABLED.get()
                ? ServerData.get(player.server).landmarks(player.getUUID()) : List.of();
        java.util.Deque<Snapshot> packets = new java.util.ArrayDeque<>();
        int size = ServerConfig.LANDMARK_SYNC_BATCH_SIZE.get();
        if (landmarks.isEmpty()) packets.add(new Snapshot(List.of(), List.of(), true));
        for (int start = 0; start < landmarks.size(); start += size)
            packets.add(new Snapshot(List.copyOf(landmarks.subList(start, Math.min(start + size, landmarks.size()))), List.of(), start == 0));
        PENDING.put(player.getUUID(), packets);
    }

    public static void changed(MinecraftServer server, ServerData.Landmark landmark) {
        if (!ServerConfig.LANDMARKS_ENABLED.get()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (Landmarks.isBoss(landmark) || landmark.creator().equals(player.getUUID()))
                send(player, new Snapshot(List.of(landmark), List.of(), false));
    }

    public static void removed(ServerPlayer player, java.util.UUID id) {
        send(player, new Snapshot(List.of(), List.of(id), false));
    }

    public static void consolidated(MinecraftServer server, List<java.util.UUID> ids) {
        int size = ServerConfig.LANDMARK_SYNC_BATCH_SIZE.get();
        for (int start = 0; start < ids.size(); start += size) {
            Snapshot packet = new Snapshot(List.of(), List.copyOf(ids.subList(start, Math.min(start + size, ids.size()))), false);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, packet);
        }
    }

    private static void send(ServerPlayer player, Snapshot packet) {
        if (!SmpEnvironment.active(player) || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        var pending = PENDING.get(player.getUUID());
        if (pending == null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        else pending.addLast(packet);
    }

    public static void tick(MinecraftServer server) {
        var iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !CHANNEL.isRemotePresent(player.connection.connection)) { iterator.remove(); continue; }
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), entry.getValue().removeFirst());
            if (entry.getValue().isEmpty()) iterator.remove();
        }
    }

    public static void clear() { PENDING.clear(); }

    private record Snapshot(List<ServerData.Landmark> landmarks, List<java.util.UUID> removed, boolean reset) {
        private static void encode(Snapshot packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.reset());
            buffer.writeCollection(packet.removed(), FriendlyByteBuf::writeUUID);
            buffer.writeCollection(packet.landmarks(), (out, value) -> {
                out.writeUUID(value.id());
                out.writeUtf(value.name());
                out.writeUtf(value.nameKey());
                out.writeUtf(value.category());
                out.writeResourceLocation(value.dimension());
                out.writeBlockPos(value.position());
                out.writeUUID(value.creator());
                out.writeUtf(value.creatorName());
                out.writeLong(value.createdAt());
            });
        }

        private static Snapshot decode(FriendlyByteBuf buffer) {
            boolean reset = buffer.readBoolean();
            List<java.util.UUID> removed = buffer.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_SNAPSHOT), FriendlyByteBuf::readUUID);
            return new Snapshot(buffer.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_SNAPSHOT),
                    in -> new ServerData.Landmark(in.readUUID(), in.readUtf(80), in.readUtf(160), in.readUtf(32),
                            in.readResourceLocation(), in.readBlockPos(), in.readUUID(), in.readUtf(64), in.readLong())), removed, reset);
        }

        private static void handle(Snapshot packet, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LandmarkClient.accept(packet.landmarks(), packet.removed(), packet.reset()));
            context.get().setPacketHandled(true);
        }
    }
}
