package com.siirio.jemroles.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceService;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RoleCommands {
    private RoleCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jem")
                .then(Commands.literal("role").executes(context -> show(context.getSource().getPlayerOrException()))));
        event.getDispatcher().register(Commands.literal("jemrole")
                .executes(context -> show(context.getSource().getPlayerOrException()))
                .then(Commands.literal("choose")
                        .then(Commands.argument("race", StringArgumentType.word())
                                .executes(context -> choose(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "race"))))));
    }

    private static int show(ServerPlayer player) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.roles.identity_required"));
            return 0;
        }
        var snapshot = RaceApi.get(player.getServer(), principal.get().accountId());
        if (snapshot.isEmpty()) player.sendSystemMessage(Component.translatable("jem.roles.usage"));
        else player.sendSystemMessage(Component.translatable("jem.roles.current", snapshot.get().race().name().toLowerCase()));
        return 1;
    }

    private static int choose(ServerPlayer player, String raw) {
        var race = RaceId.parse(raw);
        if (race.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.roles.invalid"));
            return 0;
        }
        RaceService.Result result = RaceService.choose(player, race.get());
        if (result == RaceService.Result.UNAUTHENTICATED) player.sendSystemMessage(Component.translatable("jem.roles.identity_required"));
        else if (result == RaceService.Result.CHANGE_LOCKED) player.sendSystemMessage(Component.translatable("jem.roles.change_locked"));
        else player.sendSystemMessage(Component.translatable("jem.roles.chosen", race.get().name().toLowerCase()));
        return result == RaceService.Result.CHOSEN || result == RaceService.Result.CHANGED || result == RaceService.Result.UNCHANGED ? 1 : 0;
    }
}
