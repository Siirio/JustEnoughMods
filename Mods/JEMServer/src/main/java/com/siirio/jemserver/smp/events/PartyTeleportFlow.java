package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PartyTeleportFlow {
    private static final int SEARCH_RADIUS = 6;
    private static final int VERTICAL_SEARCH = 4;

    public static void begin(ServerPlayer host, PartyState party, Collection<UUID> participants) {
        var serverForMutation = host.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { beginInScope(host, party, participants); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> beginInScope(host, store.parties().bind(party), participants));
    }

    private static void beginInScope(ServerPlayer host, PartyState party, Collection<UUID> participants) {
        clear(party);
        party.partyTeleportSession(UUID.randomUUID());
        Set<BlockPos> occupied = new HashSet<>();
        occupied.add(host.blockPosition());
        for (UUID id : participants) {
            ServerPlayer player = host.server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            PartyMember member = party.member(id);
            if (member == null) member = new PartyMember();
            saveReturn(player, member);
            party.addMember(id, member);
        }
        for (UUID id : participants) {
            if (id.equals(host.getUUID())) continue;
            ServerPlayer player = host.server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            BlockPos destination = findNearHost(player, host, occupied);
            if (destination == null) throw new IllegalArgumentException("no_safe_arrival");
            occupied.add(destination);
            player.teleportTo(host.serverLevel(), destination.getX() + .5, destination.getY(), destination.getZ() + .5,
                    player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
        }

    }

    public static void finish(MinecraftServer server, PartyState party) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishInScope(server, party); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishInScope(server, store.parties().bind(party)));
    }

    private static void finishInScope(MinecraftServer server, PartyState party) {
        if (!party.hasPartyTeleportSession()) return;
        for (UUID id : SmpRecords.memberIds(party)) {
            if (!Parties.accepted(party, id)) continue;
            PartyMember member = party.member(id);
            clearArenaAccess(member);
            if (member == null || member.returnPoint() == null) continue;
            member.returnPending(true);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) prompt(player, party.id(),party.partyTeleportSession());
        }

    }

    public static boolean pending(PartyState party, UUID playerId) {
        return party.hasPartyTeleportSession() && party.member(playerId) != null && party.member(playerId).returnPending();
    }

    public static boolean choose(ServerPlayer player, PartyState party, UUID session, boolean returnBack) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return chooseInScope(player, party, session, returnBack); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> chooseInScope(player, store.parties().bind(party), session, returnBack));
    }

    private static boolean chooseInScope(ServerPlayer player, PartyState party, UUID session, boolean returnBack) {
        if (!party.hasPartyTeleportSession()||!party.partyTeleportSession().equals(session)||!pending(party, player.getUUID())) return false;
        PartyMember member = party.member(player.getUUID());
        boolean completed = !returnBack || teleportBack(player, member);
        if (!completed) return false;
        clearReturn(member);
        clearSessionIfResolved(party);

        return true;
    }

    public static boolean returnNow(ServerPlayer player, PartyState party) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return returnNowInScope(player, party); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> returnNowInScope(player, store.parties().bind(party)));
    }

    private static boolean returnNowInScope(ServerPlayer player, PartyState party) {
        PartyMember member = party.member(player.getUUID());
        if (member == null || member.returnPoint() == null || !teleportBack(player, member)) return false;
        clearReturn(member);
        clearSessionIfResolved(party);

        return true;
    }

    public static boolean forceReturn(ServerPlayer player, PartyState party) {
        PartyMember member = party.member(player.getUUID());
        return member != null && member.returnPoint() != null && teleportBack(player, member);
    }

    public static void clearMember(PartyState party, UUID playerId) {
        PartyMember member = party.member(playerId);
        clearReturn(member);
        clearArenaAccess(member);
    }

    public static void clearSession(PartyState party) {
        party.clearPartyTeleportSession();
    }

    public static void clear(PartyState party) {
        party.clearPartyTeleportSession();
        for (UUID id : party.members().keySet()) {
            PartyMember member = party.member(id);
            clearReturn(member);
            clearArenaAccess(member);
        }
    }

    private static void saveReturn(ServerPlayer player, PartyMember member) {
        Vec3 position = player.position();
        member.returnPoint(new WorldPosition(player.serverLevel().dimension().location(), position.x, position.y, position.z, player.getYRot(), player.getXRot()));
    }

    private static BlockPos findNearHost(ServerPlayer player, ServerPlayer host, Set<BlockPos> occupied) {
        ServerLevel level = host.serverLevel();
        BlockPos origin = host.blockPosition();
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                for (int delta = 0; delta <= VERTICAL_SEARCH; delta++) for (int direction : delta == 0 ? new int[]{0} : new int[]{delta, -delta}) {
                    BlockPos candidate = origin.offset(dx, direction, dz);
                    if (occupied.contains(candidate) || !safe(player, level, candidate)) continue;
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean safe(ServerPlayer player, ServerLevel level, BlockPos position) {
        return level.hasChunkAt(position) && level.getWorldBorder().isWithinBounds(position)
                && level.getBlockState(position.below()).isFaceSturdy(level, position.below(), Direction.UP)
                && level.getFluidState(position).isEmpty() && level.getFluidState(position.above()).isEmpty()
                && level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(
                position.getX() + .5, position.getY(), position.getZ() + .5));
    }

    private static boolean teleportBack(ServerPlayer player, PartyMember member) {
        if (member == null || member.returnPoint() == null) return false;
        var point = member.returnPoint();
        ResourceLocation dimension = point.dimension();
        ServerLevel level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return false;
        player.teleportTo(level, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        return true;
    }

    private static void prompt(ServerPlayer player, UUID partyId,UUID session) {
        player.sendSystemMessage(Component.translatable("jem.event.return_question").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" "))
                .append(Component.translatable("jem.event.return").withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/smp event-return " + partyId + " " + session + " return"))))
                .append(Component.literal(" "))
                .append(Component.translatable("jem.event.stay").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/smp event-return " + partyId + " " + session + " stay")))));
    }

    private static void clearReturn(PartyMember member) {
        if (member == null) return;
        member.clearReturnPending(); member.clearReturnPoint();
    }

    private static void clearArenaAccess(PartyMember member) {
        if (member != null) member.clearLegacyArenaBoundary();
    }

    private static void clearSessionIfResolved(PartyState party) {
        boolean pending = SmpRecords.memberIds(party).stream().anyMatch(id ->
                party.member(id).returnPending());
        if (!pending) party.clearPartyTeleportSession();
    }

    private PartyTeleportFlow() {}
}
