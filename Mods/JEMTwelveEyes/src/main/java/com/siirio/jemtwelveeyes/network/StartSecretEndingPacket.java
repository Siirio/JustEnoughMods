package com.siirio.jemtwelveeyes.network;

import com.siirio.jemtwelveeyes.CampaignEndingEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

record StartSecretEndingPacket() {
    static void encode(StartSecretEndingPacket packet, FriendlyByteBuf buffer) { }
    static StartSecretEndingPacket decode(FriendlyByteBuf buffer) { return new StartSecretEndingPacket(); }
    static void handle(StartSecretEndingPacket packet, Supplier<NetworkEvent.Context> context) {
        MinecraftForge.EVENT_BUS.post(new CampaignEndingEvent());
        context.get().setPacketHandled(true);
    }
}
