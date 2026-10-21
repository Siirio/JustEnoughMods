package com.siirio.jemserver.smp;

import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record SettlementUiAction(UUID request, UUID town, String action, UUID target, String text, long amount) {
    public void encode(FriendlyByteBuf out) {
        out.writeUUID(request); out.writeUUID(town); out.writeUtf(action, 24);
        out.writeNullable(target, FriendlyByteBuf::writeUUID); out.writeUtf(text == null ? "" : text, 64); out.writeLong(amount);
    }
    public static SettlementUiAction decode(FriendlyByteBuf in) {
        if (in.readableBytes() > 256) throw new io.netty.handler.codec.DecoderException("Settlement action exceeds byte budget");
        var value = new SettlementUiAction(in.readUUID(), in.readUUID(), in.readUtf(24),
                in.readNullable(FriendlyByteBuf::readUUID), in.readUtf(64), in.readLong());
        if (in.isReadable()) throw new io.netty.handler.codec.DecoderException("Trailing settlement action data");
        return value;
    }
    void handle(net.minecraft.server.level.ServerPlayer player) {
        if (!Set.of("rename", "remove", "delete", "buyback", "deposit", "withdraw").contains(action)) return;
        if (!DomainFeatures.loaded("jem_settlements")) return;
        String result = com.siirio.jemserver.compat.settlements.SettlementHostAdapter.action(player, this);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.settlement.action." + result.toLowerCase(java.util.Locale.ROOT)), true);
        if (action.equals("buyback") && result.equals("ACCEPTED")) return;
        SmpNetwork.open(player, "settlements", action.equals("delete") && (result.equals("ACCEPTED")
                || result.equals("REPLAYED")) ? null : town);
    }
}
