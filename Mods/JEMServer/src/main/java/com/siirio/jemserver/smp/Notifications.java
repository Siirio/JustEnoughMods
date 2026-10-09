package com.siirio.jemserver.smp;

import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class Notifications {
    public static void send(MinecraftServer server, UUID playerId, String key, String target) {
        var player = server.getPlayerList().getPlayer(playerId);
        if (player != null)
            player.sendSystemMessage(
                    Component.translatable("jem.smp." + key)
                            .append(Component.literal(" "))
                            .append(
                                    Component.translatable("jem.smp.open")
                                            .withStyle(
                                                    style ->
                                                            style.withClickEvent(
                                                                    new ClickEvent(
                                                                            ClickEvent.Action
                                                                                    .RUN_COMMAND,
                                                                            "/smp " + target)))));
    }

    private Notifications() {}
}
