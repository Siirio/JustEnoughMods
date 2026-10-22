package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** Session authorization only; boss arenas have no movement boundary. */
public final class HostedBoundary {
    public static boolean authorized(ServerPlayer player, PartyState party) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return authorizedInScope(player, party); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> authorizedInScope(player, store.parties().bind(party)));
    }

    private static boolean authorizedInScope(ServerPlayer player, PartyState party) {
        if (!party.state().equals("ACTIVE") || !Parties.accepted(party, player.getUUID())) return false;
        PartyMember member = party.member(player.getUUID());
        if (member.eliminated()) return false;
        if (party.hasEventId()) {
            EventState event = SmpData.get(player.server).events().current(party.eventId());
            if (event == null || event.member(player.getUUID()) != null && event.member(player.getUUID()).eliminated()) return false;
        }
        if (party.hasBossEntity() && player.serverLevel().getEntity(party.bossEntity()) instanceof net.minecraft.world.entity.LivingEntity boss
                && com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(boss).stream()
                .anyMatch(status -> status.playerId().equals(player.getUUID()) && status.state().equals("ELIMINATED"))) return false;
        return true;
    }

    public static UUID actionToken(MinecraftServer server, PartyState party) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return actionTokenInScope(server, party); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> actionTokenInScope(server, store.parties().bind(party)));
    }

    private static UUID actionTokenInScope(MinecraftServer server, PartyState party) {
        if (party.hasEventId()) {
            EventState event = SmpData.get(server).events().current(party.eventId());
            if (event == null || !event.state().equals("ACTIVE")) return null;
            return event.hasAttemptId() ? event.attemptId() : event.id();
        }
        return party.hasBossEntity() ? party.bossEntity() : party.id();
    }

    public static boolean matchesAction(MinecraftServer server, PartyState party, UUID token) {
        UUID current = actionToken(server, party);
        return token != null && current != null && current.equals(token);
    }

    public static boolean canForfeit(ServerPlayer player, PartyState party) {
        return authorized(player, party);
    }

    public static void clearParty(PartyState party) {}
    public static void clear(UUID player) {}
    public static void clear() {}
    private HostedBoundary() {}
}
