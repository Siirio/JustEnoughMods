package com.siirio.jemeconomy.network;

import com.siirio.jemcurrency.JemCurrencyCore;
import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemcurrency.CurrencyAuthority;
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
    private static final String VERSION = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JemCurrencyCore.LEGACY_NAMESPACE, "wallet"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(EconomyNetwork::acceptVersion)
            .serverAcceptedVersions(EconomyNetwork::acceptVersion).simpleChannel();
    private EconomyNetwork() {}

    private static boolean acceptVersion(String version) {
        return VERSION.equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version);
    }

    public static boolean supported() {
        return Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                () -> () -> com.siirio.jemeconomy.client.CurrencyClientConnection.remote(CHANNEL)));
    }

    public static void register() {
        CHANNEL.messageBuilder(ContainerCurrencyPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ContainerCurrencyPacket::encode).decoder(ContainerCurrencyPacket::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (fullMarket(player) && com.siirio.jemidentity.api.IdentityPolicy.resolve(player).isPresent()
                            && player.containerMenu.containerId == packet.containerId()
                            && player.containerMenu instanceof com.siirio.jemeconomy.api.CurrencyContainerAccess access
                            && access.permitsCurrencyAction(player)) {
                        String result = "";
                        if (packet.action() == ContainerCurrencyPacket.Action.WITHDRAW) {
                            var outcome = EconomyApi.withdraw(player, packet.amount(), packet.requestId());
                            result = "jem.economy.exchange." + outcome.status().name().toLowerCase(java.util.Locale.ROOT);
                        } else if (packet.action() == ContainerCurrencyPacket.Action.EXCHANGE) {
                            var outcome = EconomyApi.exchangePhysical(player, packet.source(), packet.count(), packet.target(), packet.requestId());
                            result = "jem.economy.exchange." + outcome.status().name().toLowerCase(java.util.Locale.ROOT);
                        }
                        if (packet.action() == ContainerCurrencyPacket.Action.DEPOSIT) {
                            var outcome = EconomyApi.depositAll(player, packet.requestId());
                            result = "jem.economy.exchange." + outcome.status().name().toLowerCase(java.util.Locale.ROOT);
                        }
                        if (packet.action() == ContainerCurrencyPacket.Action.SEND) {
                            var recipient = player.server.getPlayerList().getPlayerByName(packet.recipient());
                            var account = recipient == null ? java.util.Optional.<com.siirio.jemidentity.api.VerifiedPrincipal>empty() : com.siirio.jemidentity.api.IdentityPolicy.resolve(recipient);
                            result = "jem.economy.invalid";
                            if (account.isPresent() && recipient != player && packet.amount() > 0 && packet.amount() <= 1_000_000_000L) {
                                var sent = EconomyApi.send(player, account.get().accountId(), packet.amount(), packet.requestId());
                                result = sent.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED
                                        || sent.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED ? "jem.economy.exchange.committed" : "jem.economy.insufficient";
                            }
                        }
                        player.inventoryMenu.broadcastChanges(); player.containerMenu.broadcastChanges();
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ContainerCurrencyStatusPacket(
                                packet.containerId(), packet.requestId(), EconomyApi.balance(player), result));
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ContainerCurrencyStatusPacket.class, 9, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ContainerCurrencyStatusPacket::encode).decoder(ContainerCurrencyStatusPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof com.siirio.jemeconomy.client.CurrencyContainerScreen screen)
                            screen.acceptCurrency(packet);
                    });
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(WalletActionPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WalletActionPacket::encode).decoder(WalletActionPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && CurrencyAuthority.active(player)
                            && player.containerMenu instanceof com.siirio.jemeconomy.api.CurrencyContainerAccess access
                            && access.permitsCurrencyAction(player)) handle(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(WalletStatusPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WalletStatusPacket::encode).decoder(WalletStatusPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemeconomy.client.WalletClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(NativeTradePacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NativeTradePacket::encode).decoder(NativeTradePacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemeconomy.client.NativeTradeClient.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(NativeTradeActionPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NativeTradeActionPacket::encode).decoder(NativeTradeActionPacket::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && CurrencyAuthority.active(player)
                            && player.containerMenu instanceof com.siirio.jemeconomy.merchant.NativeMerchantAccess access
                            && access.jem$session() != null) access.jem$session().takeQuoted(packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(MarketQueryPacket.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MarketQueryPacket::encode).decoder(MarketQueryPacket::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (fullMarket(player)) sendCatalog(player, packet.open(), packet.search(), packet.category(), packet.page(), "");
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(MarketCatalogPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MarketCatalogPacket::encode).decoder(MarketCatalogPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemeconomy.client.MarketClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(MarketActionPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MarketActionPacket::encode).decoder(MarketActionPacket::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (fullMarket(player)) handleMarket(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(CoinExchangePacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CoinExchangePacket::encode).decoder(CoinExchangePacket::decode).consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (fullMarket(player)) handleExchange(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void sendTrade(ServerPlayer player, NativeTradePacket packet) {
        if (CurrencyAuthority.active(player) && CHANNEL.isRemotePresent(player.connection.connection))
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static boolean fullMarket(ServerPlayer player) {
        return player != null && DedicatedSmp.active(player) && net.minecraftforge.fml.ModList.get().isLoaded("jem_economy");
    }

    public static void tradeAction(NativeTradeActionPacket packet) { if (supported()) CHANNEL.sendToServer(packet); }
    public static void marketQuery(String search, String category, int page, boolean open) {
        if (!supported()) return;
        CHANNEL.sendToServer(new MarketQueryPacket(search, category, page, open));
    }
    public static void marketAction(MarketActionPacket.Action action, ResourceLocation item, int units, long revision,
                                    String search, String category, int page) {
        if (!supported()) return;
        CHANNEL.sendToServer(new MarketActionPacket(action, item, units, revision, UUID.randomUUID(), search, category, page));
    }
    public static void exchange(CoinDenomination source, int count, CoinDenomination target, String search, String category, int page) {
        if (!supported()) return;
        CHANNEL.sendToServer(new CoinExchangePacket(source, count, target, UUID.randomUUID(), search, category, page));
    }

    public static void containerCurrency(ContainerCurrencyPacket packet) {
        if (supported()) CHANNEL.sendToServer(packet);
    }

    private static void handleMarket(ServerPlayer player, MarketActionPacket packet) {
        String error = "";
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(packet.item());
        if (packet.units() <= 0 || packet.units() > (packet.action() == MarketActionPacket.Action.BULK_BUY ? 4096 : 64) || packet.action() == MarketActionPacket.Action.SELL && item == null) error = "jem.economy.market.invalid";
        else if (com.siirio.jemeconomy.market.MarketSavedData.get(player.getServer()).revision(player.getServer()) != packet.revision())
            error = "jem.economy.market.stale";
        else if (packet.action() == MarketActionPacket.Action.BUY || packet.action() == MarketActionPacket.Action.BULK_BUY) {
            var result = EconomyApi.purchaseMarketItem(player, packet.item(), packet.units(), packet.revision(), packet.requestId(),packet.action() == MarketActionPacket.Action.BULK_BUY);
            if (result.status() != EconomyApi.MarketPurchaseStatus.COMMITTED && result.status() != EconomyApi.MarketPurchaseStatus.REPLAYED)
                error = "jem.economy.market.buy." + result.status().name().toLowerCase(java.util.Locale.ROOT);
        } else if (packet.action() == MarketActionPacket.Action.SELL) {
            var result = EconomyApi.processNpcSale(player, new net.minecraft.world.item.ItemStack(item), packet.units(), packet.requestId());
            if (result.status() != EconomyApi.NpcSaleStatus.COMMITTED && result.status() != EconomyApi.NpcSaleStatus.REPLAYED)
                error = "jem.economy.market.sell." + result.status().name().toLowerCase(java.util.Locale.ROOT);
        }
        sendCatalog(player, false, packet.search(), packet.category(), packet.page(), error);
        sendStatus(player, false);
    }

    private static void handleExchange(ServerPlayer player, CoinExchangePacket packet) {
        var result = EconomyApi.exchangePhysical(player, packet.source(), packet.sourceCount(), packet.target(), packet.requestId());
        String error = result.status() == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.COMMITTED
                || result.status() == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.REPLAYED ? ""
                : "jem.economy.exchange." + result.status().name().toLowerCase(java.util.Locale.ROOT);
        sendCatalog(player, false, packet.search(), packet.category(), packet.page(), error);
        sendStatus(player, false);
    }

    private static void sendCatalog(ServerPlayer player, boolean open, String search, String category, int page, String error) {
        var snapshot = EconomyApi.marketCatalog(player, search, category, Math.max(0, Math.min(page, 10_000)));
        if (!error.isBlank()) snapshot = new com.siirio.jemeconomy.api.MarketCatalogSnapshot(snapshot.revision(), snapshot.page(), snapshot.pages(),
                snapshot.search(), snapshot.category(), snapshot.walletBalance(), snapshot.physicalCounts(), snapshot.ticksUntilUpdate(), snapshot.items(), error);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MarketCatalogPacket(open, snapshot));
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
            player.sendSystemMessage(Component.translatable(key, com.siirio.jemeconomy.physical.CoinFormatter.format(result.value())), true);
        } else if (packet.action() == WalletActionPacket.Action.SEND) {
            if (packet.amount() <= 0 || packet.amount() > 1_000_000_000L) return;
            ServerPlayer recipient = player.getServer().getPlayerList().getPlayerByName(packet.recipient());
            if (recipient == null || recipient.getUUID().equals(player.getUUID())) player.sendSystemMessage(Component.translatable("jem.economy.invalid"), true);
            else {
                var result = EconomyApi.send(player, recipient.getUUID(), packet.amount(), packet.requestId());
                if (result.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED) {
                    player.sendSystemMessage(Component.translatable("jem.economy.sent", com.siirio.jemeconomy.physical.CoinFormatter.format(packet.amount()), recipient.getGameProfile().getName()), true);
                    recipient.sendSystemMessage(Component.translatable("jem.economy.received", com.siirio.jemeconomy.physical.CoinFormatter.format(packet.amount()), player.getGameProfile().getName()), true);
                } else player.sendSystemMessage(Component.translatable(result.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.INSUFFICIENT_FUNDS
                        ? "jem.economy.insufficient" : "jem.economy.invalid"), true);
            }
        } else {
            var result = EconomyApi.depositAll(player, packet.requestId());
            String key = result.status() == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.COMMITTED
                    ? "jem.economy.wallet.deposit_ok" : result.status() == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.NO_VALID_COINS
                    ? "jem.economy.no_valid_physical" : "jem.economy.invalid";
            player.sendSystemMessage(Component.translatable(key, com.siirio.jemeconomy.physical.CoinFormatter.format(result.value())), true);
        }
        sendStatus(player, false);
    }

    public static void action(WalletActionPacket.Action action, long amount) { action(action, amount, ""); }
    public static void action(WalletActionPacket.Action action, long amount, String recipient) {
        if (!supported()) return;
        CHANNEL.sendToServer(new WalletActionPacket(action, amount, recipient == null ? "" : recipient, UUID.randomUUID()));
    }

    public static void sendStatus(ServerPlayer player, boolean open) {
        if (!CurrencyAuthority.active(player) || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        var held = EconomyApi.heldCash(player); int[] counts = new int[5];
        for (CoinDenomination denomination : CoinDenomination.values()) counts[denomination.ordinal()] = held.counts().getOrDefault(denomination, 0);
        var bulletin = EconomyApi.marketBulletin(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WalletStatusPacket(open, EconomyApi.balance(player), held.value(), counts,
                held.invalidItems(), EconomyApi.physicalOutstanding(player), bulletin.primaryGroup(), bulletin.secondaryGroup(), bulletin.ticksUntilUpdate(),
                bulletin.accountRemaining(), bulletin.globalRemaining()));
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CurrencyAuthority.active(player)) sendStatus(player, false);
    }
}
