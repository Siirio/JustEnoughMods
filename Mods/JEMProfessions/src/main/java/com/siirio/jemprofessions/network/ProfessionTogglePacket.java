package com.siirio.jemprofessions.network;

import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.network.FriendlyByteBuf;

public record ProfessionTogglePacket(ProfessionId profession, String nodeId) {
    static void encode(ProfessionTogglePacket packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.profession); buffer.writeUtf(packet.nodeId, 64); }
    static ProfessionTogglePacket decode(FriendlyByteBuf buffer) { return new ProfessionTogglePacket(buffer.readEnum(ProfessionId.class), buffer.readUtf(64)); }
}
