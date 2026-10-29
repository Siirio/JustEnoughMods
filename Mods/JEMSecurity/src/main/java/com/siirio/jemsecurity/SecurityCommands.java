package com.siirio.jemsecurity;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.siirio.jemsecurity.network.SecurityNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SecurityCommands {
    private SecurityCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("jemsecurity").requires(source -> source.hasPermission(3))
                .then(literal("status").then(argument("player", EntityArgument.player())
                        .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(literal("history").then(argument("player", EntityArgument.player())
                        .executes(context -> history(context.getSource(), EntityArgument.getPlayer(context, "player"), 10))
                        .then(argument("limit", IntegerArgumentType.integer(1, 50))
                                .executes(context -> history(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        IntegerArgumentType.getInteger(context, "limit"))))))
                .then(literal("request-log").then(argument("player", EntityArgument.player())
                        .executes(context -> requestLog(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        var state = SecurityNetwork.state(player.getUUID());
        String attestation = state == null ? "missing" : "received, mods=" + state.mods().size() + ", packs=" + state.packs().size();
        source.sendSuccess(() -> Component.literal("JEM Security: " + player.getGameProfile().getName()
                + ", attestation=" + attestation + ", suspicion=" + SecurityEvents.suspicion(player.getUUID())), false);
        return 1;
    }

    private static int history(CommandSourceStack source, ServerPlayer player, int limit) {
        var lines = SecurityAuditLog.recent(source.getServer(), player.getUUID(), limit);
        source.sendSuccess(() -> Component.literal("JEM Security history: " + player.getGameProfile().getName()
                + " (" + lines.size() + ")"), false);
        for (String line : lines) {
            String bounded = line.length() <= 900 ? line : line.substring(0, 900);
            source.sendSuccess(() -> Component.literal(bounded), false);
        }
        return lines.size();
    }

    private static int requestLog(CommandSourceStack source, ServerPlayer player) {
        SecurityNetwork.requestLog(player);
        source.sendSuccess(() -> Component.literal("Sanitized latest.log consent request sent to "
                + player.getGameProfile().getName()), false);
        return 1;
    }
}
