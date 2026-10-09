package com.siirio.jemserver.compat;

import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class TpaCompat {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void commands(RegisterCommandsEvent event) {
        var accept=Commands.literal("accept").executes(context->accept(context.getSource().getPlayerOrException()));
        event.getDispatcher().register(Commands.literal("tpa").then(accept));
        event.getDispatcher().register(Commands.literal("tpaccept").executes(context->accept(context.getSource().getPlayerOrException())));
    }

    private static int accept(ServerPlayer receiver) {
        try {
            prune(receiver);
            Class.forName("fox.mods.tpa.core.TpaManager").getMethod("acceptRequest",net.minecraft.world.entity.player.Player.class).invoke(null,receiver);
            return Command.SINGLE_SUCCESS;
        } catch(ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void prune(ServerPlayer receiver) {
        Path directory=FMLPaths.CONFIGDIR.get().resolve("TPA").resolve("data");
        if(!Files.isDirectory(directory)) return;
        try(var paths=Files.list(directory)) {
            paths.filter(path->path.getFileName().toString().endsWith(".json")).forEach(path->prune(receiver,path));
        } catch(java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void prune(ServerPlayer receiver,Path path) {
        try {
            var value=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(!receiver.getStringUUID().equals(value.get("receiver").getAsString())) return;
            UUID sender=UUID.fromString(value.get("sender").getAsString());
            if(value.get("expiryDate").getAsLong()<System.currentTimeMillis() || receiver.server.getPlayerList().getPlayer(sender)==null) Files.deleteIfExists(path);
        } catch(Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private TpaCompat() {}
}
