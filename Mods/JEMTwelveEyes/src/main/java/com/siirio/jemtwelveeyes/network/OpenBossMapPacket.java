package com.siirio.jemtwelveeyes.network;

import com.siirio.jemtwelveeyes.client.CampaignMapClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

record OpenBossMapPacket(int x, int y, int z, ResourceLocation dimension, String markerNameKey) {
    private static final int MAX_MARKER_NAME_LENGTH = 256;

    static void encode(OpenBossMapPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeUtf(packet.markerNameKey, MAX_MARKER_NAME_LENGTH);
    }

    static OpenBossMapPacket decode(FriendlyByteBuf buffer) {
        return new OpenBossMapPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readResourceLocation(),
                buffer.readUtf(MAX_MARKER_NAME_LENGTH)
        );
    }

    static void handle(OpenBossMapPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CampaignMapClient.open(
                packet.x, packet.y, packet.z, packet.dimension, packet.markerNameKey
        ));
        context.get().setPacketHandled(true);
    }
}
