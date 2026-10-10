package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SmpNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jem_server", "smp_v2"))
            .networkProtocolVersion(() -> SmpProtocol.VERSION)
            .clientAcceptedVersions(SmpProtocol.VERSION::equals).serverAcceptedVersions(SmpProtocol.VERSION::equals).simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(SmpQuery.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SmpQuery::encode).decoder(SmpQuery::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player)) SmpSubscriptions.query(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SmpAction.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SmpAction::encode).decoder(SmpAction::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player)) SmpRequests.handle(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SmpUpdate.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SmpUpdate::encode).decoder(SmpUpdate::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemserver.client.smp.SmpClient.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SmpRevive.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SmpRevive::encode).decoder(SmpRevive::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player)) SmpRequests.revive(player, packet.target());
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void revive(UUID target) { CHANNEL.sendToServer(new SmpRevive(target)); }
    public static void query(SmpQuery query) { CHANNEL.sendToServer(query); }
    public static void action(SmpAction action) { CHANNEL.sendToServer(action); }
    public static void open(ServerPlayer player, String tab, UUID selected) { open(player, new SmpQuery(tab, "", "", 0, selected)); }
    public static void open(ServerPlayer player, SmpQuery query) { if (SmpEnvironment.active(player)) SmpSubscriptions.open(player, query); }
    public static void close(ServerPlayer player) { SmpSubscriptions.close(player); }
    public static void logout(ServerPlayer player) { close(player); SmpRequests.logout(player); }
    public static void clear() { SmpSubscriptions.clear(); SmpRequests.clear(); }
    public static void shopRemoved(MinecraftServer server, UUID shopId) { SmpSubscriptions.shopRemoved(server, shopId); }
    public static void tick(MinecraftServer server) { SmpSubscriptions.tick(server); }

    static void send(ServerPlayer player, SmpUpdate update) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), update);
    }

    private SmpNetwork() {}
}
