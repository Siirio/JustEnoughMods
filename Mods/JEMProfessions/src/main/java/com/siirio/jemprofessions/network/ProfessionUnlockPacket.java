package com.siirio.jemprofessions.network;

import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.network.FriendlyByteBuf;

public record ProfessionUnlockPacket(ProfessionId profession, String node) {
    static void encode(ProfessionUnlockPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.profession); buffer.writeUtf(packet.node, 64);
    }
    static ProfessionUnlockPacket decode(FriendlyByteBuf buffer) {
        return new ProfessionUnlockPacket(buffer.readEnum(ProfessionId.class), buffer.readUtf(64));
    }
}
