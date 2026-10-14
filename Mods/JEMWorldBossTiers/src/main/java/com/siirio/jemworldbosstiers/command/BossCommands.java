package com.siirio.jemworldbosstiers.command;

import com.siirio.jemworldbosstiers.api.BossRevivalApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class BossCommands {
    private static final int SEARCH_RADIUS = 192;
    private BossCommands() {}

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jem").then(Commands.literal("boss")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("revive").executes(context -> revive(context.getSource())))));
    }

    private static int revive(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var result = BossRevivalApi.reviveNearest(source.getPlayerOrException(), SEARCH_RADIUS);
        if (result == BossRevivalApi.Result.CREATED) {
            source.sendSuccess(() -> Component.translatable("jem.boss.revive.created"), true);
            return 1;
        }
        source.sendFailure(Component.translatable("jem.boss.revive." + result.name().toLowerCase(java.util.Locale.ROOT)));
        return 0;
    }
}
