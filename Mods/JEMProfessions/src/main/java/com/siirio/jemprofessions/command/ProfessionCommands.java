package com.siirio.jemprofessions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ProfessionCommands {
    private ProfessionCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jem")
                .then(Commands.literal("professions").executes(context -> show(context.getSource().getPlayerOrException()))));
        event.getDispatcher().register(Commands.literal("jemprofession")
                .executes(context -> show(context.getSource().getPlayerOrException()))
                .then(Commands.literal("grantxp").requires(source -> source.hasPermission(4))
                        .then(Commands.argument("profession", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(context -> grant(context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "profession"),
                                                IntegerArgumentType.getInteger(context, "amount")))))));
    }

    private static int show(ServerPlayer player) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.professions.identity_required"));
            return 0;
        }
        var record = ProfessionSavedData.get(player.getServer()).findOrEmpty(principal.get().accountId());
        player.sendSystemMessage(Component.translatable("jem.professions.header"));
        for (ProfessionId id : ProfessionId.values()) {
            player.sendSystemMessage(Component.translatable("jem.professions.row", id.name().toLowerCase(), record.level(id), record.xp(id)));
        }
        return 1;
    }

    private static int grant(ServerPlayer player, String raw, int amount) {
        var id = ProfessionId.parse(raw);
        if (id.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jem.professions.invalid"));
            return 0;
        }
        ProfessionApi.administrativeCredit(player, id.get(), amount);
        player.sendSystemMessage(Component.translatable("jem.professions.granted", amount, id.get().name().toLowerCase()));
        return 1;
    }
}
