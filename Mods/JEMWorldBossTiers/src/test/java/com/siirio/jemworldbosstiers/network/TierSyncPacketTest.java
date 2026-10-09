package com.siirio.jemworldbosstiers.network;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TierSyncPacketTest {
    @Test void rewardStatsRoundTripWithCurrentWorldTier() {
        var item = ResourceLocation.fromNamespaceAndPath("legendary_monsters", "the_great_frost");
        var damage = ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_damage");
        var packet = new TierSyncPacket(5, 32, -1, Set.of(item), Set.of(item), List.of(new TierSyncPacket.RewardStats(item, Map.of(damage, 1.5))));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try { TierSyncPacket.encode(packet, buffer); assertEquals(packet, TierSyncPacket.decode(buffer)); }
        finally { buffer.release(); }
    }
}
