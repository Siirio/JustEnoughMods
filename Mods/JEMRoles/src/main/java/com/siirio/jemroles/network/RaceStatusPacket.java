package com.siirio.jemroles.network;

import com.siirio.jemroles.race.RaceId;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record RaceStatusPacket(boolean open, boolean authenticated, boolean selected, RaceId race,
                               long activeTicks, boolean freeSwitchAvailable, UUID nonce,
                               int spellTier, int spellProgress, int resonance, int waterExposure, int lavaExposure,
                               int lavaCapacity, boolean livingPath) {
    static void encode(RaceStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.open);
        buffer.writeBoolean(packet.authenticated);
        buffer.writeBoolean(packet.selected);
        buffer.writeEnum(packet.race);
        buffer.writeVarLong(packet.activeTicks);
        buffer.writeBoolean(packet.freeSwitchAvailable);
        buffer.writeUUID(packet.nonce);
        buffer.writeVarInt(packet.spellTier);
        buffer.writeVarInt(packet.spellProgress);
        buffer.writeVarInt(packet.resonance);
        buffer.writeVarInt(packet.waterExposure);
        buffer.writeVarInt(packet.lavaExposure);
        buffer.writeVarInt(packet.lavaCapacity);
        buffer.writeBoolean(packet.livingPath);
    }

    static RaceStatusPacket decode(FriendlyByteBuf buffer) {
        return new RaceStatusPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readEnum(RaceId.class), buffer.readVarLong(), buffer.readBoolean(), buffer.readUUID(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readBoolean());
    }
}
