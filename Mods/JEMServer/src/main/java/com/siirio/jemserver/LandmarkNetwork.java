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
    private static final String PROTOCOL = "3";
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
                    if (player != null && SmpEnvironment.active(player)) Landmarks.delete(player, packet.id());
                    context.get().setPacketHandled(true);
                }).add();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LandmarkClient::register);
    }

    private record Remove(java.util.UUID id) {}

    public static void remove(java.util.UUID id) { CHANNEL.sendToServer(new Remove(id)); }

    public static void sync(ServerPlayer player) {
        if (SmpEnvironment.active(player) && CHANNEL.isRemotePresent(player.connection.connection)) {
            List<ServerData.Landmark> landmarks = new java.util.ArrayList<>();
            if (ServerConfig.LANDMARKS_ENABLED.get()) landmarks.addAll(ServerData.get(player.server).landmarks());
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(landmarks));
        }
    }

    public static void syncAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(LandmarkNetwork::sync);
    }

    private record Snapshot(List<ServerData.Landmark> landmarks) {
        private static void encode(Snapshot packet, FriendlyByteBuf buffer) {
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
            return new Snapshot(buffer.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_SNAPSHOT),
                    in -> new ServerData.Landmark(in.readUUID(), in.readUtf(80), in.readUtf(160), in.readUtf(32),
                            in.readResourceLocation(), in.readBlockPos(), in.readUUID(), in.readUtf(64), in.readLong())));
        }

        private static void handle(Snapshot packet, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LandmarkClient.accept(packet.landmarks()));
            context.get().setPacketHandled(true);
        }
    }
}
