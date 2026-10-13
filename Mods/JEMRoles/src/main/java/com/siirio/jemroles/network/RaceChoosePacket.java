package com.siirio.jemroles.network;

import com.siirio.jemroles.race.RaceId;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record RaceChoosePacket(RaceId race, UUID nonce) {
    static void encode(RaceChoosePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.race);
        buffer.writeUUID(packet.nonce);
    }

    static RaceChoosePacket decode(FriendlyByteBuf buffer) {
        return new RaceChoosePacket(buffer.readEnum(RaceId.class), buffer.readUUID());
    }
}
