package com.siirio.jemserver;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModList;

final class PackSyncCommands {
    private static final String AUTOMODPACK_MOD_ID = "automodpack_mod";
    private static final String COMMAND = "automodpack generate";

    static void register(RegisterCommandsEvent event) {
        if (event.getCommandSelection() != Commands.CommandSelection.DEDICATED || !ModList.get().isLoaded(AUTOMODPACK_MOD_ID)) return;
        event.getDispatcher().register(Commands.literal("jempack")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("publish").executes(context -> run(context.getSource(), COMMAND)))
                .then(Commands.literal("preview").executes(context -> run(context.getSource(), COMMAND + " preview")))
                .then(Commands.literal("history").executes(context -> run(context.getSource(), COMMAND + " history")))
                .then(Commands.literal("rollback")
                        .then(Commands.argument("generation", IntegerArgumentType.integer(1))
                                .executes(context -> run(context.getSource(), COMMAND + " revert "
                                        + IntegerArgumentType.getInteger(context, "generation") + " confirm")))));
    }

    private static int run(CommandSourceStack source, String command) {
        return source.getServer().getCommands().performPrefixedCommand(source, command);
    }

    private PackSyncCommands() {}
}
