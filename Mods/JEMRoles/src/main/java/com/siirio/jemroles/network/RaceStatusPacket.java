package com.siirio.jemroles.network;

import com.siirio.jemroles.race.RaceId;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record RaceStatusPacket(boolean open, boolean authenticated, boolean selected, RaceId race,
                               long activeTicks, boolean freeSwitchAvailable, UUID nonce) {
    static void encode(RaceStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.open);
        buffer.writeBoolean(packet.authenticated);
        buffer.writeBoolean(packet.selected);
        buffer.writeEnum(packet.race);
        buffer.writeVarLong(packet.activeTicks);
        buffer.writeBoolean(packet.freeSwitchAvailable);
        buffer.writeUUID(packet.nonce);
    }

    static RaceStatusPacket decode(FriendlyByteBuf buffer) {
        return new RaceStatusPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readEnum(RaceId.class), buffer.readVarLong(), buffer.readBoolean(), buffer.readUUID());
    }
}
