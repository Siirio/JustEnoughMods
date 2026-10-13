package com.siirio.jemeconomy.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.siirio.jemeconomy.network.EconomyNetwork;

public final class CoinCommands {
    private CoinCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (!DedicatedSmp.accepts(event)) return;
        event.getDispatcher().register(Commands.literal("coins")
                .executes(context -> open(context.getSource().getPlayerOrException()))
                .then(Commands.literal("balance").executes(context -> balance(context.getSource().getPlayerOrException())))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L, 1_000_000_000L))
                                .executes(context -> withdraw(context.getSource().getPlayerOrException(),
                                        LongArgumentType.getLong(context, "amount")))))
                .then(Commands.literal("deposit")
                        .executes(context -> deposit(context.getSource().getPlayerOrException())))
                .then(Commands.literal("send")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L, 1_000_000_000L))
                                        .executes(context -> send(context.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(context, "player"),
                                                LongArgumentType.getLong(context, "amount")))))));
        event.getDispatcher().register(Commands.literal("market").executes(context ->
                com.siirio.jemeconomy.market.MarketRuntime.bulletin(context.getSource().getPlayerOrException())));
    }

    private static int open(ServerPlayer player) {
        EconomyNetwork.sendStatus(player, true);
        return 1;
    }

    private static int balance(ServerPlayer player) {
        if (IdentityPolicy.resolve(player).isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.economy.identity_required"));
            return 0;
        }
        var held = EconomyApi.heldCash(player);
        player.sendSystemMessage(Component.translatable("jem.economy.balance", EconomyApi.balance(player)));
        player.sendSystemMessage(Component.translatable("jem.economy.held", held.value(), held.invalidItems()));
        return 1;
    }

    private static int withdraw(ServerPlayer player, long amount) {
        var result = EconomyApi.withdraw(player, amount, UUID.randomUUID());
        return switch (result.status()) {
            case COMMITTED -> {
                player.sendSystemMessage(Component.translatable("jem.economy.withdrawn", result.value(), breakdown(result.breakdown())));
                yield 1;
            }
            case INSUFFICIENT_FUNDS -> message(player, "jem.economy.insufficient");
            case NO_SPACE -> message(player, "jem.economy.no_space");
            case UNAUTHENTICATED -> message(player, "jem.economy.identity_required");
            case QUARANTINED -> message(player, "jem.economy.physical_quarantined");
            default -> message(player, "jem.economy.invalid");
        };
    }

    private static int deposit(ServerPlayer player) {
        var result = EconomyApi.depositAll(player, UUID.randomUUID());
        return switch (result.status()) {
            case COMMITTED -> {
                player.sendSystemMessage(Component.translatable("jem.economy.deposited", result.value(), result.acceptedItems()));
                if (result.rejectedItems() > 0) player.sendSystemMessage(Component.translatable("jem.economy.invalid_physical", result.rejectedItems()));
                yield 1;
            }
            case NO_VALID_COINS -> message(player, "jem.economy.no_valid_physical");
            case UNAUTHENTICATED -> message(player, "jem.economy.identity_required");
            case INVENTORY_CHANGED -> message(player, "jem.economy.inventory_changed");
            case QUARANTINED -> message(player, "jem.economy.physical_quarantined");
            default -> message(player, "jem.economy.invalid");
        };
    }

    private static int message(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return 0;
    }

    private static String breakdown(java.util.Map<com.siirio.jemeconomy.physical.CoinDenomination, Long> breakdown) {
        return breakdown.entrySet().stream()
                .map(entry -> entry.getValue() + "×" + entry.getKey().id().replace("_coin", ""))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static int send(ServerPlayer sender, ServerPlayer recipient, long amount) {
        TransferResult result = EconomyApi.send(sender, recipient.getUUID(), amount, UUID.randomUUID());
        if (result.status() == TransferResult.Status.COMMITTED) {
            sender.sendSystemMessage(Component.translatable("jem.economy.sent", amount, recipient.getGameProfile().getName()));
            recipient.sendSystemMessage(Component.translatable("jem.economy.received", amount, sender.getGameProfile().getName()));
            return 1;
        }
        if (result.status() == TransferResult.Status.UNAUTHENTICATED) sender.sendSystemMessage(Component.translatable("jem.economy.identity_required"));
        else if (result.status() == TransferResult.Status.INSUFFICIENT_FUNDS) sender.sendSystemMessage(Component.translatable("jem.economy.insufficient"));
        else sender.sendSystemMessage(Component.translatable("jem.economy.invalid"));
        return 0;
    }
}
