package com.siirio.jemtwelveeyes.network;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CampaignNetwork {
    private static final String PROTOCOL_VERSION = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(JEMTwelveEyes.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private CampaignNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(OpenBossMapPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenBossMapPacket::encode)
                .decoder(OpenBossMapPacket::decode)
                .consumerMainThread(OpenBossMapPacket::handle)
                .add();
        CHANNEL.messageBuilder(GateWarningPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GateWarningPacket::encode)
                .decoder(GateWarningPacket::decode)
                .consumerMainThread(GateWarningPacket::handle)
                .add();
        CHANNEL.messageBuilder(StartSecretEndingPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StartSecretEndingPacket::encode)
                .decoder(StartSecretEndingPacket::decode)
                .consumerMainThread(StartSecretEndingPacket::handle)
                .add();
    }

    public static void openBossMap(ServerPlayer player, BlockPos position, ResourceLocation dimension, String markerNameKey) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenBossMapPacket(
                position.getX(), position.getY(), position.getZ(), dimension, markerNameKey
        ));
    }

    public static void showGateWarning(ServerPlayer player, Component title, Component details, int durationTicks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GateWarningPacket(title, details, durationTicks));
    }

    public static void startSecretEnding(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StartSecretEndingPacket());
    }
}
