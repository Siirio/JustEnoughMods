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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RolesNetwork {
    private static final String VERSION = "4";
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
                        sendStatus(player, result == RaceService.Result.CHANGE_LOCKED
                                || result == RaceService.Result.UNAUTHENTICATED);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(RaceStatusPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RaceStatusPacket::encode).decoder(RaceStatusPacket::decode)
                .consumerMainThread((packet, context) -> {
                    // Reserved v4 decoder: current status publication belongs to SandboxNetwork.
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(RaceActionPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RaceActionPacket::encode).decoder(RaceActionPacket::decode)
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) com.siirio.jemroles.trait.RaceAbilities.handle(player, packet.action());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(LegacyRaceVfxPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LegacyRaceVfxPacket::encode).decoder(LegacyRaceVfxPacket::decode)
                .consumerNetworkThread((packet, context) -> { context.get().setPacketHandled(true); }).add();
    }

    public static void choose(RaceId race, UUID nonce) {
        CHANNEL.sendToServer(new RaceChoosePacket(race, nonce));
    }

    public static void action(RaceActionPacket.Action action) { CHANNEL.sendToServer(new RaceActionPacket(action)); }

    public static void sendStatus(ServerPlayer player, boolean open) {
        RaceService.initialize(player);
        if (open) com.siirio.jemvisualsandbox.workbench.RaceExperience.openScreen(player);
        else com.siirio.jemvisualsandbox.workbench.RaceExperience.snapshot(player);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var principal = IdentityPolicy.resolve(player);
        boolean needsChoice = principal.isPresent() && RaceApi.get(player.getServer(), principal.get().accountId()).isEmpty();
        sendStatus(player, needsChoice);
    }

    @SubscribeEvent
    public static void enforceChoice(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            var principal = IdentityPolicy.resolve(player);
            if (principal.isPresent() && RaceApi.get(player.getServer(), principal.get().accountId()).isEmpty()) sendStatus(player, true);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RaceService.clearNonce(player);
    }
}
