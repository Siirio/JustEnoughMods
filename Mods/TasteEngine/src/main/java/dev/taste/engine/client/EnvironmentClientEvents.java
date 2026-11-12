package dev.taste.engine.client;

import dev.taste.engine.TasteMod;
import dev.taste.engine.server.ClientEnvironmentState;
import dev.taste.engine.server.EnvironmentChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnvironmentClientEvents {
    private static Connection active;
    private static boolean accepted;

    private EnvironmentClientEvents() { }

    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        active = event.getConnection();
        ClientEnvironmentState.begin(active, () -> {
            var level = Minecraft.getInstance().level;
            return level == null ? null : level.dimension().location();
        });
        accepted = accepted();
        ClientEnvironmentState.preference(accepted);
        EnvironmentChannel.announce(active, accepted);
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = null;
        ClientEnvironmentState.clear();
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null || Minecraft.getInstance().level == null) return;
        boolean next = accepted();
        if (next != accepted) {
            accepted = next;
            ClientEnvironmentState.preference(next);
            EnvironmentChannel.preference(active, next);
        }
    }

    private static boolean accepted() {
        var config = ClientState.store().current();
        return config.requestedEnabled() && config.value("server.acceptEnvironment").getAsBoolean();
    }
}
