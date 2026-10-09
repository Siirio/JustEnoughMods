package com.siirio.jemworldbosstiers.network;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.stream.Collectors;

public final class TierNetwork {
    private static final String PROTOCOL = "5";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(JemWorldBossTiers.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private TierNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                TierSyncPacket.class,
                TierSyncPacket::encode,
                TierSyncPacket::decode,
                TierSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet(player.server));
    }

    public static void syncAll(MinecraftServer server) {
        TierSyncPacket packet = packet(server);
        server.getPlayerList().getPlayers().forEach(player -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet));
    }

    private static TierSyncPacket packet(MinecraftServer server) {
        WorldTierData data = WorldTierData.get(server);
        int defeatCount = data.activeDefeatCount();
        int nextThreshold = BalanceRegistry.tierThresholds().stream()
                .filter(threshold -> threshold > defeatCount)
                .findFirst()
                .orElse(-1);
        return new TierSyncPacket(
                data.tier(),
                defeatCount,
                nextThreshold,
                BalanceRegistry.rewards().stream()
                        .filter(reward -> reward.category().equals("weapon") || reward.category().endsWith("_weapon"))
                        .map(reward -> reward.itemId())
                        .collect(Collectors.toUnmodifiableSet())
        );
    }
}
