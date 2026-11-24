package com.siirio.jemtwelveeyes.network;

import com.siirio.jemtwelveeyes.postend.PostEndEvents;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

record CompleteSecretEndingPacket() {
    static void encode(CompleteSecretEndingPacket packet, FriendlyByteBuf buffer) { }
    static CompleteSecretEndingPacket decode(FriendlyByteBuf buffer) { return new CompleteSecretEndingPacket(); }
    static void handle(CompleteSecretEndingPacket packet, Supplier<NetworkEvent.Context> context) {
        var sender = context.get().getSender();
        if (sender != null) PostEndEvents.finishSecretEnding(sender);
        context.get().setPacketHandled(true);
    }
}
