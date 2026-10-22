package com.siirio.jemworldbosstiers.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record AdmissionChoicePacket(UUID token, int choice) {
    public static void encode(AdmissionChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.token);
        buffer.writeByte(packet.choice);
    }
    public static AdmissionChoicePacket decode(FriendlyByteBuf buffer) {
        return new AdmissionChoicePacket(buffer.readUUID(), buffer.readUnsignedByte());
    }
    public static void handle(AdmissionChoicePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && packet.choice >= 0 && packet.choice <= 2) {
                com.siirio.jemworldbosstiers.encounter.StandaloneAdmission.choose(player, packet.token, packet.choice);
            }
        });
        context.setPacketHandled(true);
    }
}
