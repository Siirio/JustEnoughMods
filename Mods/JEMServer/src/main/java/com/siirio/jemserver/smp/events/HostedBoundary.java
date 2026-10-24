package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** Server-owned participant admission and arena containment. */
public final class HostedBoundary {
    public static boolean authorized(ServerPlayer player, PartyState party) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return authorizedInScope(player, party); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> authorizedInScope(player, store.parties().bind(party)));
    }

    private static boolean authorizedInScope(ServerPlayer player, PartyState party) {
        if (!party.state().equals("ACTIVE") || !player.isAlive() || player.isSpectator() || !Parties.accepted(party, player.getUUID())) return false;
        PartyMember member = party.member(player.getUUID());
        if (member.eliminated()) return false;
        if (party.hasEventId()) {
            EventState event = SmpData.get(player.server).events().current(party.eventId());
            if (event == null || !event.pendingOutcome().isEmpty() || !new EventSession(event).accepted(player.getUUID())) return false;
        }
        if (party.hasBossEntity() && player.serverLevel().getEntity(party.bossEntity()) instanceof net.minecraft.world.entity.LivingEntity boss
                && com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(boss).stream()
                .noneMatch(status -> status.playerId().equals(player.getUUID()) && !status.state().equals("ELIMINATED"))) return false;
        return true;
    }

    public static UUID actionToken(MinecraftServer server, PartyState party) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return actionTokenInScope(server, party); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> actionTokenInScope(server, store.parties().bind(party)));
    }

    private static UUID actionTokenInScope(MinecraftServer server, PartyState party) {
        if (party.hostedAttempt() != null) return party.hostedAttempt();
        if (party.hasEventId()) {
            EventState event = SmpData.get(server).events().current(party.eventId());
            if (event == null || !event.state().equals("ACTIVE")) return null;
            return event.hasAttemptId() ? event.attemptId() : event.id();
        }
        return party.hostedAttempt() != null ? party.hostedAttempt() : party.hasBossEntity() ? party.bossEntity() : party.id();
    }

    public static boolean matchesAction(MinecraftServer server, PartyState party, UUID token) {
        UUID current = actionToken(server, party);
        return token != null && current != null && current.equals(token);
    }

    public static boolean passable(ServerPlayer player, PartyState party) {
        return authorized(player, party) && !party.member(player.getUUID()).arenaEntered();
    }

    static boolean locked(ServerPlayer player, PartyState party) {
        return authorized(player, party) && party.member(player.getUUID()).arenaEntered();
    }

    public static void observe(ServerPlayer player, PartyState party, net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        if (!authorized(player, party)) return;
        var box = SolidBoundary.movementBox(player);
        if (box.minX < bounds.minX() || box.maxX > bounds.maxX()+1D
                || box.minY < bounds.minY()+SolidBoundary.WALL_THICKNESS || box.maxY > bounds.maxY()+1D-SolidBoundary.WALL_THICKNESS
                || box.minZ < bounds.minZ() || box.maxZ > bounds.maxZ()+1D) return;
        if (!party.member(player.getUUID()).arenaEntered()) {
            var store = SmpData.get(player.server);
            store.edit(() -> {
                var current = store.parties().bind(party);
                current.member(player.getUUID()).arenaEntered(true);
            });
        }
        prompt(player, party, bounds);
    }

    private static final java.util.Map<UUID, Long> NEXT_PROMPT = new java.util.HashMap<>();
    private static final java.util.Map<UUID, Long> FORFEIT_UNTIL = new java.util.HashMap<>();

    private static void prompt(ServerPlayer player, PartyState party, net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        if (distanceToEdge(bounds, player.position()) > 4D) return;
        long now = player.serverLevel().getGameTime();
        if (now < NEXT_PROMPT.getOrDefault(player.getUUID(), 0L)) return;
        boolean leader = party.owner().equals(player.getUUID());
        UUID token = actionToken(player.server, party);
        if (leader && token == null) return;
        String command = leader ? "/smp forfeit " + party.id() + " " + token : "/smp encounter-leave " + party.id();
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(leader ? "jem.event.boundary_forfeit_question" : "jem.event.boundary_leave_question")
                .withStyle(net.minecraft.ChatFormatting.GOLD).append(" ")
                .append(net.minecraft.network.chat.Component.translatable(leader ? "jem.event.boundary_forfeit_confirm" : "jem.event.boundary_leave_confirm")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED).withBold(true)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, command))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                        net.minecraft.network.chat.Component.translatable(leader ? "jem.event.boundary_forfeit_warning" : "jem.event.boundary_leave_warning"))))));
        NEXT_PROMPT.put(player.getUUID(), now + 60L);
        FORFEIT_UNTIL.put(player.getUUID(), now + 100L);
    }

    private static double distanceToEdge(net.minecraft.world.level.levelgen.structure.BoundingBox bounds, net.minecraft.world.phys.Vec3 position) {
        return Math.min(Math.min(position.x - bounds.minX(), bounds.maxX()+1D-position.x),
                Math.min(position.z-bounds.minZ(), bounds.maxZ()+1D-position.z));
    }

    public static boolean canForfeit(ServerPlayer player, PartyState party) {
        if (!locked(player, party) || !party.hasStructureBounds()
                || !party.dimension().equals(player.level().dimension().location().toString())) return false;
        var saved = party.structureBounds();
        var bounds = StructureStaging.arenaShell(new net.minecraft.world.level.levelgen.structure.BoundingBox(
                saved.minX(), saved.minY(), saved.minZ(), saved.maxX(), saved.maxY(), saved.maxZ()));
        var box = SolidBoundary.movementBox(player);
        if (box.minX < bounds.minX() || box.maxX > bounds.maxX()+1D
                || box.minY < bounds.minY() || box.maxY > bounds.maxY()+1D
                || box.minZ < bounds.minZ() || box.maxZ > bounds.maxZ()+1D) return false;
        return distanceToEdge(bounds, player.position()) <= 4D
                || player.serverLevel().getGameTime() <= FORFEIT_UNTIL.getOrDefault(player.getUUID(), 0L);
    }

    public static void clearParty(PartyState party) { SmpRecords.memberIds(party).forEach(HostedBoundary::clear); }
    public static void clear(UUID player) { NEXT_PROMPT.remove(player); FORFEIT_UNTIL.remove(player); }
    public static void clear() { NEXT_PROMPT.clear(); FORFEIT_UNTIL.clear(); }
    private HostedBoundary() {}
}
