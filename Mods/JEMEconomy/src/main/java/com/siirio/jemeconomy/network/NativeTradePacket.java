package com.siirio.jemeconomy.network;

import com.siirio.jemeconomy.merchant.NativeTradeQuote;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public record NativeTradePacket(int containerId, int merchantId, int selected, int revision, List<NativeTradeQuote> quotes,
                                long wallet, long remaining, long updateTicks, String status) {
    public static final int MAX_OFFERS = 4096;
    public void encode(FriendlyByteBuf buffer) {
        if (quotes.size() > MAX_OFFERS) throw new IllegalArgumentException("Native offer projection too large");
        buffer.writeVarInt(containerId); buffer.writeVarInt(merchantId); buffer.writeVarInt(selected); buffer.writeVarInt(revision);
        buffer.writeVarInt(quotes.size());
        for (var quote : quotes) {
            buffer.writeVarInt(quote.offerIndex()); buffer.writeEnum(quote.direction()); buffer.writeLong(quote.copper());
            buffer.writeBoolean(quote.available()); buffer.writeUtf(quote.reasonKey(), 128);
        }
        buffer.writeLong(wallet); buffer.writeLong(remaining); buffer.writeLong(updateTicks); buffer.writeUtf(status, 128);
    }
    public static NativeTradePacket decode(FriendlyByteBuf buffer) {
        int container = buffer.readVarInt(), merchant = buffer.readVarInt(), selected = buffer.readVarInt(), revision = buffer.readVarInt(), count = buffer.readVarInt();
        if (container < 0 || revision < 0 || count < 0 || count > MAX_OFFERS || selected < 0 || (count > 0 && selected >= count))
            throw new IllegalArgumentException("Invalid native trade projection");
        List<NativeTradeQuote> quotes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = buffer.readVarInt(); var direction = buffer.readEnum(NativeTradeQuote.Direction.class);
            long copper = buffer.readLong(); boolean available = buffer.readBoolean(); String reason = buffer.readUtf(128);
            if (index != i || copper < 0) throw new IllegalArgumentException("Invalid native trade quote");
            quotes.add(new NativeTradeQuote(index, direction, copper, available, reason));
        }
        return new NativeTradePacket(container, merchant, selected, revision, List.copyOf(quotes), buffer.readLong(), buffer.readLong(),
                buffer.readLong(), buffer.readUtf(128));
    }
}
