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
        SmpRecords.require(!party.hasPartyTeleportSession(), "return_pending");
        var ids = java.util.List.copyOf(participants);
        SmpRecords.require(!ids.isEmpty() && new HashSet<>(ids).size() == ids.size()
                && ids.contains(host.getUUID()), "party_not_ready");
        var destinations = new java.util.LinkedHashMap<UUID, BlockPos>();
        Set<BlockPos> occupied = new HashSet<>();
        occupied.add(host.blockPosition());
        var occupiedBoxes = new java.util.ArrayList<net.minecraft.world.phys.AABB>();
        occupiedBoxes.add(host.getBoundingBox());
        for (UUID id : ids) {
            ServerPlayer player = host.server.getPlayerList().getPlayer(id);
            PartyMember member = party.member(id);
            SmpRecords.require(player != null && member != null && member.accepted() && member.ready()
                    && !member.eliminated() && player.isAlive() && !player.isSpectator()
                    && !player.isPassenger() && !player.isVehicle(), "party_not_ready");
            if (id.equals(host.getUUID())) continue;
            BlockPos target = findNearHost(player, host, occupied, occupiedBoxes);
            SmpRecords.require(target != null, "no_safe_arrival");
            occupied.add(target); destinations.put(id, target);
            occupiedBoxes.add(player.getDimensions(player.getPose()).makeBoundingBox(target.getX() + .5, target.getY(), target.getZ() + .5));
        }
        party.partyTeleportSession(UUID.randomUUID());
        for (UUID id : ids) {
            var player = host.server.getPlayerList().getPlayer(id);
            var member = party.member(id);
            saveReturn(player, member);
            // Persist intent before the first side effect. Startup recovery uses the same return owner.
            member.returnPending(true);
        }
        SmpData.get(host.server).flush(host.server);
        var transfers = new java.util.ArrayList<Runnable>();
        var rollbacks = new java.util.ArrayList<Runnable>();
        for (var entry : destinations.entrySet()) {
            var player = host.server.getPlayerList().getPlayer(entry.getKey());
            var target = Vec3.atBottomCenterOf(entry.getValue());
            transfers.add(() -> move(player, host.serverLevel(), target, player.getYRot(), player.getXRot()));
            rollbacks.add(() -> {
                if (!teleportBack(player, party.member(player.getUUID())))
                    throw new IllegalStateException("return_pending");
            });
        }
        transferAll(transfers, rollbacks);
    }

    /** Includes a failing native transfer because it may have moved before throwing. */
    static void transferAll(java.util.List<Runnable> transfers, java.util.List<Runnable> rollbacks) {
        int index = 0;
        try {
            for (; index < transfers.size(); index++) transfers.get(index).run();
        } catch (RuntimeException failure) {
            for (int rollback = Math.min(index, rollbacks.size() - 1); rollback >= 0; rollback--) {
                try { rollbacks.get(rollback).run(); }
                catch (RuntimeException recovery) { failure.addSuppressed(recovery); }
            }
            throw failure;
        }
    }

    public static void rollback(MinecraftServer server, PartyState party) {
        if (!party.hasPartyTeleportSession()) return;
        for (UUID id : SmpRecords.memberIds(party)) {
            var member = party.member(id);
            if (member.returnPoint() == null) continue;
            member.returnPending(true);
            var player = server.getPlayerList().getPlayer(id);
            if (player != null && player.isAlive()) returnNow(player, party);
        }
        SmpData.get(server).flush(server);
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
            if (discardNativeDeathReturn(member)) continue;
            if (player != null && player.isAlive()) {
                if (party.state().equals("COMPLETED") || party.pendingOutcome().equals("COMPLETED")) prompt(player, party.id(), party.partyTeleportSession());
                else if (!returnNow(player, party)) throw new IllegalStateException("return_pending");
            }
        }
        clearSessionIfResolved(party);
        SmpData.get(server).flush(server);
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
        if (!party.state().equals("COMPLETED") || !party.pendingOutcome().isEmpty()
                || !party.hasPartyTeleportSession() || !party.partyTeleportSession().equals(session)
                || !pending(party, player.getUUID()) || party.member(player.getUUID()).nativeDeath()) return false;
        PartyMember member = party.member(player.getUUID());
        boolean completed = returnBack ? teleportBack(player, member) : StructureStaging.safeTerminalExit(player, party);
        if (!completed) return false;
        clearReturnDurably(player, party, member);
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
        if (member == null || member.returnPoint() == null) return false;
        boolean preferHome = party.state().equals("FAILED") || party.pendingOutcome().equals("FAILED") || party.state().equals("ACTIVE");
        if (!(preferHome && teleportHome(player)) && !teleportBack(player, member)) return false;
        clearReturnDurably(player, party, member);
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

    private static BlockPos findNearHost(ServerPlayer player, ServerPlayer host, Set<BlockPos> occupied,
                                             java.util.List<net.minecraft.world.phys.AABB> occupiedBoxes) {
        ServerLevel level = host.serverLevel();
        BlockPos origin = host.blockPosition();
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                for (int delta = 0; delta <= VERTICAL_SEARCH; delta++) for (int direction : delta == 0 ? new int[]{0} : new int[]{delta, -delta}) {
                    BlockPos candidate = origin.offset(dx, direction, dz);
                    var box = player.getDimensions(player.getPose()).makeBoundingBox(candidate.getX() + .5, candidate.getY(), candidate.getZ() + .5);
                    if (occupied.contains(candidate) || occupiedBoxes.stream().anyMatch(box::intersects)
                            || !safe(player, level, candidate)) continue;
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean safe(ServerPlayer player, ServerLevel level, BlockPos position) {
        return safeDestination(player, level, Vec3.atBottomCenterOf(position));
    }

    public static boolean safeDestination(ServerPlayer player, ServerLevel level, Vec3 position) {
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isVehicle()) return false;
        var box = player.getDimensions(player.getPose()).makeBoundingBox(position.x, position.y, position.z);
        if (box.minY < level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box)) return false;
        for (BlockPos occupied : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7))) {
            if (!level.hasChunkAt(occupied) || !level.getFluidState(occupied).isEmpty()
                    || com.siirio.jemserver.DeathHistory.dangerous(level.getBlockState(occupied))) return false;
        }
        BlockPos floor = BlockPos.containing(position.x, box.minY - .01, position.z);
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && !com.siirio.jemserver.DeathHistory.dangerous(level.getBlockState(floor))
                && level.noCollision(player, box);
    }

    private static boolean teleportHome(ServerPlayer player) {
        WorldPosition home = HomeTravel.configuredReturn(player);
        if (home == null) return false;
        var level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, home.dimension()));
        if (level == null) return false;
        var target = new Vec3(home.x(), home.y(), home.z());
        if (!safeDestination(player, level, target)) return false;
        move(player, level, target, home.yaw(), home.pitch());
        return true;
    }

    private static boolean teleportBack(ServerPlayer player, PartyMember member) {
        if (member == null || member.returnPoint() == null || !player.isAlive()) return false;
        var point = member.returnPoint();
        ResourceLocation dimension = point.dimension();
        ServerLevel level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return false;
        Vec3 destination = new Vec3(point.x(), point.y(), point.z());
        if (!safeDestination(player, level, destination)) {
            destination = com.siirio.jemserver.DeathHistory.nearestSafe(player, level,
                    BlockPos.containing(destination), 6);
            if (destination == null || !safeDestination(player, level, destination)) return false;
        }
        move(player, level, destination, point.yaw(), point.pitch());
        return true;
    }

    private static void move(ServerPlayer player, ServerLevel level, Vec3 target, float yaw, float pitch) {
        BossTeleportSafety.apply(player, level, target, yaw, pitch);
        if (player.serverLevel() != level || player.position().distanceToSqr(target) > .01)
            throw new IllegalStateException("transfer_rejected");
    }

    private static void clearReturnDurably(ServerPlayer player, PartyState party, PartyMember member) {
        SmpData.flushPlayerPosition(player);
        WorldPosition origin = member.returnPoint();
        UUID session = party.partyTeleportSession();
        clearReturn(member); clearSessionIfResolved(party);
        try { SmpData.get(player.server).flush(player.server); }
        catch (RuntimeException failure) {
            member.returnPoint(origin); member.returnPending(true); party.partyTeleportSession(session);
            throw failure;
        }
    }

    public static void login(ServerPlayer player) {
        var store = SmpData.get(player.server);
        store.edit(() -> {
            for (var party : store.parties().currentRows()) {
                if (!pending(party, player.getUUID()) || party.state().equals("ACTIVE")) continue;
                var member = party.member(player.getUUID());
                if (member.nativeDeath()) { clearReturnDurably(player, party, member); continue; }
                if (party.state().equals("COMPLETED") || party.pendingOutcome().equals("COMPLETED")) prompt(player, party.id(), party.partyTeleportSession());
                else returnNow(player, party);
            }
        });
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

    static boolean discardNativeDeathReturn(PartyMember member) {
        if (!member.nativeDeath()) return false;
        clearReturn(member);
        return true;
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
                party.member(id).returnPoint() != null);
        if (!pending) party.clearPartyTeleportSession();
    }

    private PartyTeleportFlow() {}
}
