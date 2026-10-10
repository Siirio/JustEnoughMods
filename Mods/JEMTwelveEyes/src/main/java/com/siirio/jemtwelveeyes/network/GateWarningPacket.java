package com.siirio.jemtwelveeyes.network;

import com.siirio.jemtwelveeyes.client.GateWarningOverlay;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

record GateWarningPacket(Component title, Component details, int durationTicks) {
    private static final int MAX_COMPONENT_LENGTH = 32767;

    static void encode(GateWarningPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(Component.Serializer.toJson(packet.title), MAX_COMPONENT_LENGTH);
        buffer.writeUtf(Component.Serializer.toJson(packet.details), MAX_COMPONENT_LENGTH);
        buffer.writeVarInt(packet.durationTicks);
    }

    static GateWarningPacket decode(FriendlyByteBuf buffer) {
        Component title = Component.Serializer.fromJson(buffer.readUtf(MAX_COMPONENT_LENGTH));
        Component details = Component.Serializer.fromJson(buffer.readUtf(MAX_COMPONENT_LENGTH));
        return new GateWarningPacket(
                title == null ? Component.empty() : title,
                details == null ? Component.empty() : details,
                buffer.readVarInt()
        );
    }

    static void handle(GateWarningPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GateWarningOverlay.show(
                packet.title,
                packet.details,
                packet.durationTicks
        ));
        context.get().setPacketHandled(true);
    }
}
