package com.siirio.jemeconomy.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CoinCommands {
    private CoinCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("coins")
                .executes(context -> balance(context.getSource().getPlayerOrException()))
                .then(Commands.literal("send")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L, 1_000_000_000L))
                                        .executes(context -> send(context.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(context, "player"),
                                                LongArgumentType.getLong(context, "amount")))))));
    }

    private static int balance(ServerPlayer player) {
        if (!player.getServer().isDedicatedServer()) {
            player.sendSystemMessage(Component.translatable("jem.economy.server_only"));
            return 0;
        }
        if (IdentityPolicy.resolve(player).isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.economy.identity_required"));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("jem.economy.balance", EconomyApi.balance(player)));
        return 1;
    }

    private static int send(ServerPlayer sender, ServerPlayer recipient, long amount) {
        TransferResult result = EconomyApi.send(sender, recipient.getUUID(), amount, UUID.randomUUID());
        if (result.status() == TransferResult.Status.COMMITTED) {
            sender.sendSystemMessage(Component.translatable("jem.economy.sent", amount, recipient.getGameProfile().getName(), result.receiptId()));
            recipient.sendSystemMessage(Component.translatable("jem.economy.received", amount, sender.getGameProfile().getName(), result.receiptId()));
            return 1;
        }
        if (result.status() == TransferResult.Status.UNAUTHENTICATED) sender.sendSystemMessage(Component.translatable("jem.economy.identity_required"));
        else if (result.status() == TransferResult.Status.INSUFFICIENT_FUNDS) sender.sendSystemMessage(Component.translatable("jem.economy.insufficient"));
        else sender.sendSystemMessage(Component.translatable("jem.economy.invalid"));
        return 0;
    }
}
