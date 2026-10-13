package com.siirio.jemeconomy.network;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemidentity.api.DedicatedSmp;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EconomyNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JemEconomy.MOD_ID, "wallet"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).simpleChannel();
    private EconomyNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(WalletActionPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WalletActionPacket::encode).decoder(WalletActionPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && DedicatedSmp.active(player)) handle(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(WalletStatusPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WalletStatusPacket::encode).decoder(WalletStatusPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemeconomy.client.WalletClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
    }

    private static void handle(ServerPlayer player, WalletActionPacket packet) {
        if (packet.action() == WalletActionPacket.Action.WITHDRAW) {
            if (packet.amount() <= 0 || packet.amount() > 1_000_000_000L) return;
            var result = EconomyApi.withdraw(player, packet.amount(), packet.requestId());
            String key = switch (result.status()) {
                case COMMITTED -> "jem.economy.wallet.withdraw_ok";
                case INSUFFICIENT_FUNDS -> "jem.economy.insufficient";
                case NO_SPACE -> "jem.economy.no_space";
                case QUARANTINED -> "jem.economy.physical_quarantined";
                default -> "jem.economy.invalid";
            };
            player.sendSystemMessage(Component.translatable(key, result.value()), true);
        } else {
            var result = EconomyApi.depositAll(player, packet.requestId());
            String key = result.status() == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.COMMITTED
                    ? "jem.economy.wallet.deposit_ok" : result.status() == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.NO_VALID_COINS
                    ? "jem.economy.no_valid_physical" : "jem.economy.invalid";
            player.sendSystemMessage(Component.translatable(key, result.value()), true);
        }
        sendStatus(player, true);
    }

    public static void action(WalletActionPacket.Action action, long amount) { CHANNEL.sendToServer(new WalletActionPacket(action, amount, UUID.randomUUID())); }

    public static void sendStatus(ServerPlayer player, boolean open) {
        if (!DedicatedSmp.active(player) || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        var held = EconomyApi.heldCash(player); int[] counts = new int[5];
        for (CoinDenomination denomination : CoinDenomination.values()) counts[denomination.ordinal()] = held.counts().getOrDefault(denomination, 0);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WalletStatusPacket(open, EconomyApi.balance(player), held.value(), counts,
                held.invalidItems(), EconomyApi.physicalOutstanding(player)));
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && DedicatedSmp.active(player)) sendStatus(player, false);
    }
}
