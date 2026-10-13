package com.siirio.jemserver.map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

public final class ProjectCommands {
    private ProjectCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var project = Commands.literal("project");
        project.then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(context -> report(context.getSource(), ProjectRegionService.create(
                        context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"))))));
        project.then(Commands.literal("add").then(Commands.argument("project", UuidArgument.uuid())
                .then(Commands.argument("x1", IntegerArgumentType.integer())
                .then(Commands.argument("z1", IntegerArgumentType.integer())
                .then(Commands.argument("x2", IntegerArgumentType.integer())
                .then(Commands.argument("z2", IntegerArgumentType.integer())
                        .executes(context -> report(context.getSource(), ProjectRegionService.addRectangle(
                                context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "project"),
                                IntegerArgumentType.getInteger(context, "x1"), IntegerArgumentType.getInteger(context, "z1"),
                                IntegerArgumentType.getInteger(context, "x2"), IntegerArgumentType.getInteger(context, "z2"))))))))));
        project.then(Commands.literal("remove").then(Commands.argument("project", UuidArgument.uuid())
                .then(Commands.argument("rectangle", IntegerArgumentType.integer(0))
                        .executes(context -> report(context.getSource(), ProjectRegionService.removeRectangle(
                                context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "project"),
                                IntegerArgumentType.getInteger(context, "rectangle")))))));
        dispatcher.register(Commands.literal("jem").then(project));
    }

    private static int report(CommandSourceStack source, ProjectRegionService.Result result) {
        source.sendSuccess(() -> Component.literal("Project region: " + result.name()), false);
        return result == ProjectRegionService.Result.ACCEPTED ? 1 : 0;
    }
}
