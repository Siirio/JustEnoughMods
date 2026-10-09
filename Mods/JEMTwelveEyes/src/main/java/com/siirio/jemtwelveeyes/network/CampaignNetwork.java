package com.siirio.jemtwelveeyes.network;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import com.siirio.jemtwelveeyes.client.GateWarningOverlay;
import com.siirio.jemtwelveeyes.CampaignEndingEvent;
import net.minecraftforge.common.MinecraftForge;
import com.siirio.jemtwelveeyes.client.CampaignMapClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

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

    private record StartSecretEndingPacket() {
        private static void encode(StartSecretEndingPacket packet, FriendlyByteBuf buffer) { }
        private static StartSecretEndingPacket decode(FriendlyByteBuf buffer) { return new StartSecretEndingPacket(); }
        private static void handle(StartSecretEndingPacket packet, Supplier<NetworkEvent.Context> context) {
            MinecraftForge.EVENT_BUS.post(new CampaignEndingEvent());
            context.get().setPacketHandled(true);
        }
    }

    private record GateWarningPacket(Component title, Component details, int durationTicks) {
        private static final int MAX_COMPONENT_LENGTH = 32767;

        private static void encode(GateWarningPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(Component.Serializer.toJson(packet.title), MAX_COMPONENT_LENGTH);
            buffer.writeUtf(Component.Serializer.toJson(packet.details), MAX_COMPONENT_LENGTH);
            buffer.writeVarInt(packet.durationTicks);
        }

        private static GateWarningPacket decode(FriendlyByteBuf buffer) {
            Component title = Component.Serializer.fromJson(buffer.readUtf(MAX_COMPONENT_LENGTH));
            Component details = Component.Serializer.fromJson(buffer.readUtf(MAX_COMPONENT_LENGTH));
            return new GateWarningPacket(
                    title == null ? Component.empty() : title,
                    details == null ? Component.empty() : details,
                    buffer.readVarInt()
            );
        }

        private static void handle(GateWarningPacket packet, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GateWarningOverlay.show(
                    packet.title,
                    packet.details,
                    packet.durationTicks
            ));
            context.get().setPacketHandled(true);
        }
    }

    private record OpenBossMapPacket(int x, int y, int z, ResourceLocation dimension, String markerNameKey) {
        private static void encode(OpenBossMapPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.x);
            buffer.writeInt(packet.y);
            buffer.writeInt(packet.z);
            buffer.writeResourceLocation(packet.dimension);
            buffer.writeUtf(packet.markerNameKey);
        }

        private static OpenBossMapPacket decode(FriendlyByteBuf buffer) {
            return new OpenBossMapPacket(
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readResourceLocation(),
                    buffer.readUtf()
            );
        }

        private static void handle(OpenBossMapPacket packet, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CampaignMapClient.open(
                    packet.x, packet.y, packet.z, packet.dimension, packet.markerNameKey
            ));
            context.get().setPacketHandled(true);
        }
    }
}
