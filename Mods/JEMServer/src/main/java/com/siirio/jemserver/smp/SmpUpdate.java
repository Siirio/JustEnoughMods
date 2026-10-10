package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;

public record SmpUpdate(CompoundTag view, boolean open, UUID request, String error,
                        UUID session, long baseRevision, long revision) {
    public SmpUpdate(CompoundTag view, boolean open, UUID request, String error) {
        this(view, open, request, error, null, 0, 0);
    }

    public void encode(FriendlyByteBuf buffer) {
        int start = buffer.writerIndex();
        buffer.writeNbt(view);
        buffer.writeBoolean(open);
        buffer.writeNullable(request, FriendlyByteBuf::writeUUID);
        buffer.writeUtf(error, 128);
        buffer.writeNullable(session, FriendlyByteBuf::writeUUID);
        buffer.writeVarLong(baseRevision);
        buffer.writeVarLong(revision);
        if (buffer.writerIndex() - start > SmpProtocol.MAX_VIEW_BYTES)
            throw new io.netty.handler.codec.EncoderException("SMP view exceeds byte budget");
    }

    public static SmpUpdate decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > SmpProtocol.MAX_VIEW_BYTES)
            throw new io.netty.handler.codec.DecoderException("SMP view exceeds byte budget");
        var view = buffer.readNbt(new NbtAccounter(SmpProtocol.MAX_VIEW_BYTES));
        if (view == null) throw new io.netty.handler.codec.DecoderException("Missing SMP view");
        return new SmpUpdate(view, buffer.readBoolean(), buffer.readNullable(FriendlyByteBuf::readUUID),
                buffer.readUtf(128), buffer.readNullable(FriendlyByteBuf::readUUID),
                buffer.readVarLong(), buffer.readVarLong());
    }
}
