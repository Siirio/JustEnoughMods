package com.siirio.jemworldbosstiers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record TierSyncPacket(int tier, int defeatCount, int nextThreshold, Set<ResourceLocation> bossWeapons) {
    private static final int MAX_REWARDS = 1024;

    public static void encode(TierSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.tier);
        buffer.writeVarInt(packet.defeatCount);
        buffer.writeVarInt(packet.nextThreshold);
        buffer.writeCollection(packet.bossWeapons, FriendlyByteBuf::writeResourceLocation);
    }

    public static TierSyncPacket decode(FriendlyByteBuf buffer) {
        int tier = buffer.readVarInt();
        int defeatCount = buffer.readVarInt();
        int nextThreshold = buffer.readVarInt();
        Set<ResourceLocation> bossWeapons = buffer.readCollection(FriendlyByteBuf.limitValue(HashSet::new, MAX_REWARDS), FriendlyByteBuf::readResourceLocation);
        return new TierSyncPacket(tier, defeatCount, nextThreshold, bossWeapons);
    }

    public static void handle(TierSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientTierState.update(
                packet.tier,
                packet.defeatCount,
                packet.nextThreshold,
                packet.bossWeapons
        ));
        context.setPacketHandled(true);
    }
}
