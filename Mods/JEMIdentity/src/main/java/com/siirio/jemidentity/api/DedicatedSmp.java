package com.siirio.jemidentity.api;

import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

/** The single lifecycle boundary for services that exist only on a dedicated JEM server. */
public final class DedicatedSmp {
    public static boolean active(MinecraftServer server) {
        return server != null && server.isDedicatedServer();
    }

    public static boolean active(ServerPlayer player) {
        return player != null && active(player.getServer());
    }

    public static boolean accepts(RegisterCommandsEvent event) {
        return event.getCommandSelection() == Commands.CommandSelection.DEDICATED;
    }

    private DedicatedSmp() {
    }
}
