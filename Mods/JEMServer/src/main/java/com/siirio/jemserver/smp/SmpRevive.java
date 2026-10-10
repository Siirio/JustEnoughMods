package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record SmpRevive(UUID target) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(target);
    }

    public static SmpRevive decode(FriendlyByteBuf buffer) {
        return new SmpRevive(buffer.readUUID());
    }
}
