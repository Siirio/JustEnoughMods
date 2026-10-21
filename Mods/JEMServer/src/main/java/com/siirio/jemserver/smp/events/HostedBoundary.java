package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** Session authorization only; boss arenas have no movement boundary. */
public final class HostedBoundary {
    public static boolean authorized(ServerPlayer player, CompoundTag party) {
        if (!party.getString("state").equals("ACTIVE") || !Parties.accepted(party, player.getUUID())) return false;
        CompoundTag member = SmpRecords.members(party).getCompound(player.getStringUUID());
        if (member.getBoolean("eliminated")) return false;
        if (party.hasUUID("eventId")) {
            CompoundTag event = SmpData.get(player.server).find("events", party.getUUID("eventId"));
            if (event == null || SmpRecords.members(event).getCompound(player.getStringUUID()).getBoolean("eliminated")) return false;
        }
        if (party.hasUUID("bossEntity") && player.serverLevel().getEntity(party.getUUID("bossEntity")) instanceof net.minecraft.world.entity.LivingEntity boss
                && com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(boss).stream()
                .anyMatch(status -> status.playerId().equals(player.getUUID()) && status.state().equals("ELIMINATED"))) return false;
        return true;
    }

    public static UUID actionToken(MinecraftServer server, CompoundTag party) {
        if (party.hasUUID("eventId")) {
            CompoundTag event = SmpData.get(server).find("events", party.getUUID("eventId"));
            if (event == null || !event.getString("state").equals("ACTIVE")) return null;
            return event.hasUUID("attemptId") ? event.getUUID("attemptId") : event.getUUID("id");
        }
        return party.hasUUID("bossEntity") ? party.getUUID("bossEntity") : party.getUUID("id");
    }

    public static boolean matchesAction(MinecraftServer server, CompoundTag party, UUID token) {
        UUID current = actionToken(server, party);
        return token != null && current != null && current.equals(token);
    }

    public static boolean canForfeit(ServerPlayer player, CompoundTag party) {
        return authorized(player, party);
    }

    public static void clearParty(CompoundTag party) {}
    public static void clear(UUID player) {}
    public static void clear() {}
    private HostedBoundary() {}
}
