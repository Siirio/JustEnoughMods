package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record SmpAction(UUID request, SmpTable table, UUID id, int revision,
                        SmpActionKind kind, SmpActionInput input) {
    public SmpAction {
        boolean valid = kind == SmpActionKind.CREATE ? input instanceof SmpPartyRequest
                : kind == SmpActionKind.MESSAGE ? input instanceof SmpMessageRequest
                : kind.playerInput(table) ? input instanceof SmpPlayerRequest : input == SmpNoInput.INSTANCE;
        if (!valid) throw new IllegalArgumentException("Action input does not match kind");
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(request);
        buffer.writeEnum(table);
        buffer.writeNullable(id, FriendlyByteBuf::writeUUID);
        buffer.writeVarInt(revision);
        buffer.writeEnum(kind);
        if (input instanceof SmpPartyRequest party) {
            buffer.writeUtf(party.activity(), SmpProtocol.MAX_LANGUAGE);
            buffer.writeUtf(party.title(), SmpProtocol.MAX_TITLE);
            buffer.writeUtf(party.description(), SmpProtocol.MAX_DESCRIPTION);
            buffer.writeUtf(party.language(), SmpProtocol.MAX_LANGUAGE);
            buffer.writeVarInt(party.slots());
            buffer.writeLong(party.scheduled());
            buffer.writeBoolean(party.approval());
            buffer.writeBoolean(party.solo());
        } else if (input instanceof SmpMessageRequest message) {
            buffer.writeUtf(message.message(), SmpProtocol.MAX_MESSAGE);
        } else if (input instanceof SmpPlayerRequest player) {
            buffer.writeUUID(player.player());
        }
    }

    public static SmpAction decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > SmpProtocol.MAX_REQUEST_BYTES)
            throw new io.netty.handler.codec.DecoderException("SMP action exceeds byte budget");
        UUID request = buffer.readUUID();
        var table = buffer.readEnum(SmpTable.class);
        UUID id = buffer.readNullable(FriendlyByteBuf::readUUID);
        int revision = buffer.readVarInt();
        var kind = buffer.readEnum(SmpActionKind.class);
        SmpActionInput input = kind == SmpActionKind.CREATE
                ? new SmpPartyRequest(buffer.readUtf(SmpProtocol.MAX_LANGUAGE), buffer.readUtf(SmpProtocol.MAX_TITLE),
                    buffer.readUtf(SmpProtocol.MAX_DESCRIPTION), buffer.readUtf(SmpProtocol.MAX_LANGUAGE),
                    buffer.readVarInt(), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean())
                : kind == SmpActionKind.MESSAGE ? new SmpMessageRequest(buffer.readUtf(SmpProtocol.MAX_MESSAGE))
                : kind.playerInput(table) ? new SmpPlayerRequest(buffer.readUUID()) : SmpNoInput.INSTANCE;
        if (buffer.isReadable()) throw new io.netty.handler.codec.DecoderException("Trailing SMP action data");
        return new SmpAction(request, table, id, revision, kind, input);
    }
}
