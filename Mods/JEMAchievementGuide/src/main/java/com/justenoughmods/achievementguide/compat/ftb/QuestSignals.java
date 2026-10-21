package com.justenoughmods.achievementguide.compat.ftb;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** Read-only optional domain boundaries; this guide owns no gameplay state. */
final class QuestSignals {
    private QuestSignals() {}

    static boolean economy(MinecraftServer server) {
        return loaded("jem_economy") && flag("com.siirio.jemeconomy.api.EconomyApi", "isEnabled", server);
    }

    static boolean events(MinecraftServer server) {
        return server.isDedicatedServer() && loaded("jem_server")
                && flag("com.siirio.jemserver.smp.events.EventRuntime", "enabled", server);
    }

    static boolean campaign(MinecraftServer server) {
        return loaded("jem_twelve_eyes") && flag("com.siirio.jemtwelveeyes.api.CampaignApi", "isAvailable", server);
    }

    static boolean campaignComplete(ServerPlayer player) {
        return campaign(player.server)
                && flag("com.siirio.jemtwelveeyes.api.CampaignApi", "isActive", player.server)
                && flag("com.siirio.jemtwelveeyes.api.CampaignApi", "complete", player.server);
    }

    static String race(ServerPlayer player) {
        if (!loaded("jem_roles")) return "";
        Optional<?> snapshot = (Optional<?>)call("com.siirio.jemroles.api.RaceApi", "get",
                new Class<?>[]{ServerPlayer.class}, player);
        if (snapshot.isEmpty()) return "";
        try {
            return ((Enum<?>)snapshot.get().getClass().getMethod("race").invoke(snapshot.get())).name();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Canonical race snapshot API is incompatible", failure);
        }
    }

    static boolean loaded(String id) { return ModList.get().isLoaded(id); }

    private static boolean flag(String owner, String method, MinecraftServer server) {
        return (Boolean)call(owner, method, new Class<?>[]{MinecraftServer.class}, server);
    }

    private static Object call(String owner, String method, Class<?>[] signature, Object argument) {
        try {
            return Class.forName(owner).getMethod(method, signature).invoke(null, argument);
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException("Canonical quest signal failed: " + owner + "." + method, failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Canonical quest signal API is incompatible: " + owner + "." + method, failure);
        }
    }
}
