package com.siirio.jemprofessions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.puffish.skillsmod.api.SkillsAPI;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class ProfessionCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private ProfessionCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jem")
                .then(Commands.literal("professions").executes(context -> open(context.getSource().getPlayerOrException()))));
        var root = Commands.literal("jemprof")
                .executes(context -> open(context.getSource().getPlayerOrException()))
                .then(Commands.literal("inspect").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(context ->
                                show(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("grantxp").requires(source -> source.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("profession", StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                .executes(context -> grant(context.getSource().getPlayerOrException(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "profession"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))));
        event.getDispatcher().register(root);
    }

    private static int open(ServerPlayer player) { SkillsAPI.openScreen(player); return 1; }

    private static int show(ServerPlayer actor, ServerPlayer target) {
        var principal = IdentityPolicy.resolve(target);
        if (principal.isEmpty()) {
            actor.sendSystemMessage(Component.translatable("jem.professions.identity_required"));
            return 0;
        }
        var record = ProfessionSavedData.get(target.getServer()).findOrEmpty(principal.get().accountId());
        actor.sendSystemMessage(Component.translatable("jem.professions.inspect_header", target.getDisplayName()));
        for (ProfessionId id : ProfessionId.values()) {
            actor.sendSystemMessage(Component.translatable("jem.professions.row", professionName(id), ProfessionApi.level(target, id), ProfessionApi.xp(target, id)));
        }
        return 1;
    }

    private static int grant(ServerPlayer actor, ServerPlayer target, String raw, int amount) {
        var id = ProfessionId.parse(raw);
        if (id.isEmpty()) {
            actor.sendSystemMessage(Component.translatable("jem.professions.invalid"));
            return 0;
        }
        ProfessionApi.administrativeCredit(target, id.get(), amount);
        LOGGER.info("JEM Professions administrative XP actor={} target={} profession={} amount={}", actor.getUUID(), target.getUUID(), id.get(), amount);
        actor.sendSystemMessage(Component.translatable("jem.professions.granted", amount, professionName(id.get()), target.getDisplayName()));
        return 1;
    }

    private static Component professionName(ProfessionId profession) {
        return Component.translatable("jem.professions.name." + profession.name().toLowerCase());
    }
}
