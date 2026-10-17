package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class RaceCommands {
    private RaceCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("jemrace")
            .executes(c->{c.getSource().sendSuccess(()->Component.literal("F6: origin and mastery. /jemrace developer on|off: separate local developer mode."),false);return 1;})
            .then(Commands.literal("developer")
                .requires(source->source.getEntity() instanceof net.minecraft.server.level.ServerPlayer p&&RaceExperience.allowed(p))
                .then(Commands.literal("on").executes(c->{RaceExperience.developer(c.getSource().getPlayerOrException(),true);return 1;}))
                .then(Commands.literal("tier").then(Commands.argument("tier",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,3))
                    .executes(c->{RaceExperience.developerTier(c.getSource().getPlayerOrException(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"tier"));return 1;})))
                .then(Commands.literal("bloom").then(Commands.argument("upgrade",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,3))
                    .executes(c->{RaceExperience.developerBloom(c.getSource().getPlayerOrException(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"upgrade"));return 1;})))
                .then(Commands.literal("off").executes(c->{RaceExperience.developer(c.getSource().getPlayerOrException(),false);return 1;}))));
    }
}
