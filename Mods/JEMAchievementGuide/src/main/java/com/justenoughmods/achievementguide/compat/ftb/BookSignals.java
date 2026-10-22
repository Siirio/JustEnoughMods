package com.justenoughmods.achievementguide.compat.ftb;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Read-only adapters to the currently owning domains, including login reconciliation. */
final class BookSignals {
    private static final String CAMPAIGN = "com.siirio.jemtwelveeyes.api.CampaignApi";
    private BookSignals() {}

    static long progress(ServerPlayer player, String signal, String argument) {
        if (signal.startsWith("race_")) {
            if (!QuestSignals.loaded("jem_roles")) return 0;
            Optional<?> snapshot = (Optional<?>)call("com.siirio.jemroles.api.RaceApi", "get",
                    new Class<?>[]{ServerPlayer.class}, player);
            if (snapshot.isEmpty()) return 0;
            Object race = snapshot.get();
            if (argument != null && !argument.isEmpty() && !call(race, "race").toString().equals(argument)) return 0;
            return switch (signal) {
                case "race_chosen" -> 1;
                case "race_uses" -> ((Number)call(race, "spellProgress")).longValue();
                case "race_tier" -> ((Number)call(race, "spellTier")).longValue();
                default -> throw new IllegalArgumentException("Unknown race quest signal " + signal);
            };
        }
        if (signal.startsWith("campaign_")) {
            if (!QuestSignals.loaded("jem_twelve_eyes")) return 0;
            Object result = switch (signal) {
                case "campaign_main" -> call(CAMPAIGN, "mainDefeated", new Class<?>[]{MinecraftServer.class, ResourceLocation.class}, player.server, new ResourceLocation(argument));
                case "campaign_complete" -> call(CAMPAIGN, "complete", new Class<?>[]{MinecraftServer.class}, player.server);
                case "campaign_revealed" -> call(CAMPAIGN, "postEndRevealed", new Class<?>[]{MinecraftServer.class}, player.server);
                case "campaign_post_end" -> call(CAMPAIGN, "postEndDefeated", new Class<?>[]{MinecraftServer.class, ResourceLocation.class}, player.server, new ResourceLocation(argument));
                case "campaign_prerequisite" -> prerequisite(player.server, argument);
                default -> throw new IllegalArgumentException("Unknown campaign quest signal " + signal);
            };
            return Boolean.TRUE.equals(result) ? 1 : 0;
        }
        if (signal.startsWith("event_")) {
            if (!QuestSignals.events(player.server)) return 0;
            Object profile = call("com.siirio.jemserver.smp.Profiles", "snapshot",
                    new Class<?>[]{MinecraftServer.class, UUID.class}, player.server, player.getUUID());
            return switch (signal) {
                case "event_raid" -> ((Number)call(profile, "raidsCompleted")).longValue();
                case "event_blood_moon" -> ((Number)call(profile, "bloodMoonClears")).longValue();
                case "event_activity" -> {
                    Object count = ((Map<?, ?>)call(profile, "eventStats")).get(argument);
                    yield count == null ? 0 : ((Number)count).longValue();
                }
                default -> throw new IllegalArgumentException("Unknown event quest signal " + signal);
            };
        }
        if (signal.equals("towns") || signal.equals("residents")) {
            if (!QuestSignals.loaded("jem_settlements")) return 0;
            List<?> towns = (List<?>)call("com.siirio.jemsettlements.api.SettlementApi", "ownedTowns", new Class<?>[]{ServerPlayer.class}, player);
            if (signal.equals("towns")) return towns.size();
            Object data = call("com.siirio.jemsettlements.town.SettlementSavedData", "get", new Class<?>[]{MinecraftServer.class}, player.server);
            long maximum = 0;
            for (Object town : towns) maximum = Math.max(maximum, ((Collection<?>)call(data, "residents", new Class<?>[]{UUID.class}, call(town, "id"))).size());
            return maximum;
        }
        if (signal.equals("mounts")) {
            if (!QuestSignals.loaded("jem_settlements")) return 0;
            return ((Number)call("com.siirio.jemsettlements.api.SettlementApi", "collectedMountTypes",
                    new Class<?>[]{ServerPlayer.class}, player)).longValue();
        }
        throw new IllegalArgumentException("Unknown authored quest signal " + signal);
    }

    private static boolean prerequisite(MinecraftServer server, String key) {
        Object prerequisite = call("com.siirio.jemcompat.gate.CampaignPrerequisite", "byKey", new Class<?>[]{String.class}, key);
        Object data = call("com.siirio.jemcompat.gate.CampaignSavedData", "get", new Class<?>[]{MinecraftServer.class}, server);
        return (Boolean)call(data, "prerequisiteDefeated", new Class<?>[]{prerequisite.getClass()}, prerequisite);
    }

    static Object call(String owner, String method, Class<?>[] signature, Object... arguments) {
        try { return Class.forName(owner).getMethod(method, signature).invoke(null, arguments); }
        catch (InvocationTargetException failure) { throw new IllegalStateException("Quest source failed: " + owner + "." + method, failure.getCause()); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Quest source API changed: " + owner + "." + method, failure); }
    }

    static Object call(Object owner, String method) { return call(owner, method, new Class<?>[0]); }
    static Object call(Object owner, String method, Class<?>[] signature, Object... arguments) {
        try { return owner.getClass().getMethod(method, signature).invoke(owner, arguments); }
        catch (InvocationTargetException failure) { throw new IllegalStateException("Quest source failed: " + method, failure.getCause()); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Quest source API changed: " + method, failure); }
    }

}
