package com.siirio.jemroles.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceService;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.siirio.jemroles.network.RolesNetwork;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class RoleCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private RoleCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jem")
                .then(Commands.literal("role").executes(context -> open(context.getSource().getPlayerOrException()))));
        event.getDispatcher().register(Commands.literal("jemrole")
                .executes(context -> show(context.getSource().getPlayerOrException()))
                .then(Commands.literal("choose")
                        .then(Commands.argument("race", StringArgumentType.word())
                                .executes(context -> choose(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "race")))))
                .then(Commands.literal("inspect").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> inspect(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("set").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("race", StringArgumentType.word())
                                        .then(Commands.literal("confirm").executes(context -> administrativeSet(
                                                context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "race"))))))));
    }

    private static int open(ServerPlayer player) {
        RolesNetwork.sendStatus(player, true);
        return 1;
    }

    private static int show(ServerPlayer player) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.roles.identity_required"));
            return 0;
        }
        var snapshot = RaceApi.get(player.getServer(), principal.get().accountId());
        if (snapshot.isEmpty()) player.sendSystemMessage(Component.translatable("jem.roles.usage"));
        else player.sendSystemMessage(Component.translatable("jem.roles.current", raceName(snapshot.get().race())));
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
        else player.sendSystemMessage(Component.translatable("jem.roles.chosen", raceName(race.get())));
        return result == RaceService.Result.CHOSEN || result == RaceService.Result.CHANGED || result == RaceService.Result.UNCHANGED ? 1 : 0;
    }

    private static int inspect(ServerPlayer actor, ServerPlayer target) {
        var snapshot = RaceService.snapshot(target);
        actor.sendSystemMessage(snapshot.<Component>map(value -> Component.translatable("jem.roles.inspect",
                        target.getDisplayName(), raceName(value.race()), value.activeTicks(), value.freeSwitchAvailable()))
                .orElseGet(() -> Component.translatable("jem.roles.inspect_unset", target.getDisplayName())));
        return 1;
    }

    private static int administrativeSet(ServerPlayer actor, ServerPlayer target, String raw) {
        var race = RaceId.parse(raw);
        if (race.isEmpty()) {
            actor.sendSystemMessage(Component.translatable("jem.roles.invalid"));
            return 0;
        }
        RaceService.Result result = RaceService.administrativeSet(target, race.get());
        if (result == RaceService.Result.UNAUTHENTICATED) {
            actor.sendSystemMessage(Component.translatable("jem.roles.identity_required"));
            return 0;
        }
        LOGGER.info("JEM Roles administrative mutation actor={} target={} race={} result={}",
                actor.getGameProfile().getId(), target.getGameProfile().getId(), race.get(), result);
        actor.sendSystemMessage(Component.translatable("jem.roles.admin_set", target.getDisplayName(), raceName(race.get())));
        RolesNetwork.sendStatus(target, false);
        return 1;
    }

    private static Component raceName(RaceId race) {
        return Component.translatable("jem.roles.race." + race.name().toLowerCase());
    }
}
