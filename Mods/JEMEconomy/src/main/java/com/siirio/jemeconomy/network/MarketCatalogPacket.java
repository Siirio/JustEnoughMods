package com.siirio.jemeconomy.network;

import com.siirio.jemeconomy.api.MarketCatalogSnapshot;
import com.siirio.jemeconomy.api.MarketItemSnapshot;
import com.siirio.jemeconomy.policy.ItemPolicy;
import java.util.ArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record MarketCatalogPacket(boolean open, MarketCatalogSnapshot snapshot) {
    static void encode(MarketCatalogPacket packet, FriendlyByteBuf buffer) {
        var value = packet.snapshot;
        buffer.writeBoolean(packet.open); buffer.writeVarLong(value.revision()); buffer.writeVarInt(value.page()); buffer.writeVarInt(value.pages());
        buffer.writeUtf(value.search(), 64); buffer.writeUtf(value.category(), 16); buffer.writeVarLong(value.walletBalance());
        for (int count : value.physicalCounts()) buffer.writeVarInt(count);
        buffer.writeVarLong(value.ticksUntilUpdate()); buffer.writeUtf(value.errorKey(), 96); buffer.writeVarInt(value.items().size());
        for (MarketItemSnapshot row : value.items()) {
            buffer.writeResourceLocation(row.id()); buffer.writeItem(row.stack()); buffer.writeVarLong(row.buyCopper()); buffer.writeVarLong(row.sellCopper());
            buffer.writeEnum(row.category()); buffer.writeVarInt(row.accountSellRemaining()); buffer.writeVarInt(row.globalSellRemaining());
            buffer.writeBoolean(row.buyAvailable()); buffer.writeBoolean(row.sellAvailable());
        }
    }
    static MarketCatalogPacket decode(FriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean(); long revision = buffer.readVarLong(); int page = buffer.readVarInt(); int pages = buffer.readVarInt();
        String search = buffer.readUtf(64), category = buffer.readUtf(16); long balance = buffer.readVarLong();
        int[] physical = new int[5]; for (int index = 0; index < 5; index++) physical[index] = buffer.readVarInt();
        long next = buffer.readVarLong(); String error = buffer.readUtf(96); int size = Math.min(16, buffer.readVarInt());
        var rows = new ArrayList<MarketItemSnapshot>(size);
        for (int index = 0; index < size; index++) {
            ResourceLocation id = buffer.readResourceLocation(); var stack = buffer.readItem(); long buy = buffer.readVarLong(), sell = buffer.readVarLong();
            var group = buffer.readEnum(ItemPolicy.MarketCategory.class); int account = buffer.readVarInt(), global = buffer.readVarInt();
            rows.add(new MarketItemSnapshot(id, stack, buy, sell, group, account, global, buffer.readBoolean(), buffer.readBoolean()));
        }
        return new MarketCatalogPacket(open, new MarketCatalogSnapshot(revision, page, pages, search, category, balance, physical, next, rows, error));
    }
}
