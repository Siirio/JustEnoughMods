package com.siirio.jemroles.network;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.JemRoles;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceService;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RolesNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(JemRoles.MOD_ID, "main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private RolesNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(RaceChoosePacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RaceChoosePacket::encode).decoder(RaceChoosePacket::decode)
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        RaceService.Result result = RaceService.choose(player, packet.race(), packet.nonce());
                        if (result == RaceService.Result.CHANGE_LOCKED) {
                            player.sendSystemMessage(Component.translatable("jem.roles.change_locked"));
                        } else if (result == RaceService.Result.UNAUTHENTICATED) {
                            player.sendSystemMessage(Component.translatable("jem.roles.identity_required"));
                        }
                        sendStatus(player, true);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(RaceStatusPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RaceStatusPacket::encode).decoder(RaceStatusPacket::decode)
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> com.siirio.jemroles.client.RoleClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void choose(RaceId race, UUID nonce) {
        CHANNEL.sendToServer(new RaceChoosePacket(race, nonce));
    }

    public static void sendStatus(ServerPlayer player, boolean open) {
        var principal = IdentityPolicy.resolve(player);
        UUID nonce = principal.isPresent() ? RaceService.issueNonce(player) : new UUID(0L, 0L);
        var snapshot = principal.flatMap(value -> RaceApi.get(player.getServer(), value.accountId()));
        RaceStatusPacket packet = snapshot
                .map(value -> new RaceStatusPacket(open, true, true, value.race(), value.activeTicks(),
                        value.freeSwitchAvailable(), nonce))
                .orElseGet(() -> new RaceStatusPacket(open, principal.isPresent(), false, RaceId.HUMAN, 0L, true, nonce));
        if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        } else if (open) {
            player.sendSystemMessage(Component.translatable("jem.roles.client_required"));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var principal = IdentityPolicy.resolve(player);
        boolean needsChoice = principal.isPresent() && RaceApi.get(player.getServer(), principal.get().accountId()).isEmpty();
        sendStatus(player, needsChoice);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RaceService.clearNonce(player);
    }
}
