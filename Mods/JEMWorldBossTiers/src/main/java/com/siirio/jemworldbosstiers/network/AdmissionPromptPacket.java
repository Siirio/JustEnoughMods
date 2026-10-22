package com.siirio.jemworldbosstiers.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

public record AdmissionPromptPacket(UUID token, Component boss, Component blocked, boolean raid) {
    public static void encode(AdmissionPromptPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.token);
        buffer.writeComponent(packet.boss);
        buffer.writeBoolean(packet.blocked != null);
        if (packet.blocked != null) buffer.writeComponent(packet.blocked);
        buffer.writeBoolean(packet.raid);
    }
    public static AdmissionPromptPacket decode(FriendlyByteBuf buffer) {
        return new AdmissionPromptPacket(buffer.readUUID(), buffer.readComponent(),
                buffer.readBoolean() ? buffer.readComponent() : null, buffer.readBoolean());
    }
    public static void handle(AdmissionPromptPacket packet, Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.siirio.jemworldbosstiers.client.StandaloneEncounterScreen.open(packet)));
        supplier.get().setPacketHandled(true);
    }
}
