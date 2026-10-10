package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record SmpQuery(String tab, String filter, String search, int page, UUID selected, boolean reset) {
    public SmpQuery(String tab, String filter, String search, int page, UUID selected) {
        this(tab, filter, search, page, selected, false);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(tab, 16);
        buffer.writeUtf(filter, SmpProtocol.MAX_FILTER);
        buffer.writeUtf(search, SmpProtocol.MAX_SEARCH);
        buffer.writeVarInt(page);
        buffer.writeNullable(selected, FriendlyByteBuf::writeUUID);
        buffer.writeBoolean(reset);
    }

    public static SmpQuery decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > SmpProtocol.MAX_REQUEST_BYTES)
            throw new io.netty.handler.codec.DecoderException("SMP query exceeds byte budget");
        var query = new SmpQuery(buffer.readUtf(16), buffer.readUtf(SmpProtocol.MAX_FILTER),
                buffer.readUtf(SmpProtocol.MAX_SEARCH), buffer.readVarInt(),
                buffer.readNullable(FriendlyByteBuf::readUUID), buffer.readBoolean());
        if (buffer.isReadable()) throw new io.netty.handler.codec.DecoderException("Trailing SMP query data");
        return query;
    }

    public SmpQuery withoutReset() {
        return reset ? new SmpQuery(tab, filter, search, page, selected) : this;
    }
}
