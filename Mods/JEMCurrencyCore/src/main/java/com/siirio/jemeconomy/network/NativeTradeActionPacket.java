package com.siirio.jemeconomy.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.ClickType;

/** Acknowledges displayed terms, never supplies a price, account or item stack. */
public record NativeTradeActionPacket(int containerId, int revision, int selected, int button, ClickType type, UUID requestId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId); buffer.writeVarInt(revision); buffer.writeVarInt(selected);
        buffer.writeVarInt(button); buffer.writeEnum(type); buffer.writeUUID(requestId);
    }
    public static NativeTradeActionPacket decode(FriendlyByteBuf buffer) {
        return new NativeTradeActionPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readEnum(ClickType.class), buffer.readUUID());
    }
}
