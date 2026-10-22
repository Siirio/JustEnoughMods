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

    private record ActionPacket(SmpAction action) {}

    public static void register() {
        CHANNEL.messageBuilder(SmpQuery.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SmpQuery::encode).decoder(SmpQuery::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player)) {
                        if (packet.tab().equals("settlements")) {
                            if (DomainFeatures.loaded("jem_settlements")) com.siirio.jemserver.compat.settlements.SettlementHostAdapter.open(player, packet.selected());
                        } else SmpSubscriptions.query(player, packet);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> packet.action().encode(buffer)).decoder(buffer -> new ActionPacket(SmpAction.decode(buffer))).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player)) SmpRequests.handle(player, packet.action());
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
        CHANNEL.messageBuilder(HomePreview.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HomePreview::encode).decoder(HomePreview::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemserver.client.XaeroLandmarks.showPrivateHome(packet.dimension(), packet.position()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SmpModuleOpen.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SmpModuleOpen::encode).decoder(SmpModuleOpen::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player)) packet.open(player);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SettlementUiAction.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SettlementUiAction::encode).decoder(SettlementUiAction::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && SmpEnvironment.active(player) && SmpRequests.allow(player)) packet.handle(player);
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void revive(UUID target) { CHANNEL.sendToServer(new SmpRevive(target)); }
    public static void query(SmpQuery query) { CHANNEL.sendToServer(query); }
    public static void action(SmpAction action) { CHANNEL.sendToServer(new ActionPacket(action)); }
    public static void openModule(SmpModuleOpen.Module module) { CHANNEL.sendToServer(new SmpModuleOpen(module)); }
    public static void settlement(SettlementUiAction action) { CHANNEL.sendToServer(action); }
    public static void open(ServerPlayer player, String tab, UUID selected) { open(player, new SmpQuery(tab, "", "", 0, selected)); }
    public static void open(ServerPlayer player, SmpQuery query) {
        if (!SmpEnvironment.active(player)) return;
        if (query.tab().equals("settlements")) {
            if (DomainFeatures.loaded("jem_settlements")) com.siirio.jemserver.compat.settlements.SettlementHostAdapter.open(player, query.selected());
        } else SmpSubscriptions.open(player, query);
    }
    public static void close(ServerPlayer player) { SmpSubscriptions.close(player); }
    public static void shopRemoved(MinecraftServer server, UUID shopId) { SmpSubscriptions.shopRemoved(server, shopId); }
    public static void tick(MinecraftServer server) { SmpSubscriptions.tick(server); }
    static void homePreview(ServerPlayer player, net.minecraft.resources.ResourceLocation dimension, net.minecraft.core.BlockPos position) {
        if (CHANNEL.isRemotePresent(player.connection.connection))
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HomePreview(dimension, position));
    }

    static void send(ServerPlayer player, SmpUpdate update) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), update);
    }

    private SmpNetwork() {}
}
