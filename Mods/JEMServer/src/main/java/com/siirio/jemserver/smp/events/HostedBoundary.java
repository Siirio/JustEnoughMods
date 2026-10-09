package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.Parties;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HostedBoundary {
    private static final String ENTERED = "arenaEntered";
    private static final String ENTRANCE_DIMENSION = "arenaEntranceDimension";
    private static final String ENTRANCE_X = "arenaEntranceX";
    private static final String ENTRANCE_Y = "arenaEntranceY";
    private static final String ENTRANCE_Z = "arenaEntranceZ";
    private static final double FORFEIT_DISTANCE = 4D;
    private static final long PROMPT_COOLDOWN_TICKS = 60L;
    private static final long FORFEIT_CONFIRMATION_TICKS = 100L;
    private static final Map<UUID, Long> NEXT_PROMPT = new HashMap<>();
    private static final Map<UUID, Long> FORFEIT_UNTIL = new HashMap<>();

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

    public static void captureEntrances(MinecraftServer server, CompoundTag party, java.util.Collection<UUID> participants) {
        Boundary boundary = boundary(server, party);
        if (boundary == null) return;
        for (UUID id : participants) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || player.serverLevel() != boundary.level() || boundary.bounds().isInside(player.blockPosition())) continue;
            CompoundTag member = SmpRecords.members(party).getCompound(id.toString());
            member.putString(ENTRANCE_DIMENSION, boundary.level().dimension().location().toString());
            member.putDouble(ENTRANCE_X, player.getX());
            member.putDouble(ENTRANCE_Y, player.getY());
            member.putDouble(ENTRANCE_Z, player.getZ());
        }
        SmpData.get(server).changed(party);
    }

    public static void observe(ServerPlayer player, CompoundTag party, BoundingBox bounds) {
        if (!authorized(player, party) || !bounds.isInside(player.blockPosition())) return;
        CompoundTag member = SmpRecords.members(party).getCompound(player.getStringUUID());
        if (!member.getBoolean(ENTERED)) {
            member.putBoolean(ENTERED, true);
            SmpData.get(player.server).changed(party);
        }
        if (distanceToEdge(bounds, player.position()) <= FORFEIT_DISTANCE) prompt(player, party);
    }

    public static boolean canForfeit(ServerPlayer player, CompoundTag party) {
        Boundary boundary = boundary(player.server, party);
        if (boundary == null || player.serverLevel() != boundary.level()) return false;
        CompoundTag member = SmpRecords.members(party).getCompound(player.getStringUUID());
        if (!member.getBoolean(ENTERED) || !boundary.bounds().isInside(player.blockPosition())) return false;
        return distanceToEdge(boundary.bounds(), player.position()) <= FORFEIT_DISTANCE
                || player.serverLevel().getGameTime() <= FORFEIT_UNTIL.getOrDefault(player.getUUID(), 0L);
    }

    public static void clear(UUID player) {
        NEXT_PROMPT.remove(player);
        FORFEIT_UNTIL.remove(player);
    }

    public static void clear() {
        NEXT_PROMPT.clear();
        FORFEIT_UNTIL.clear();
    }

    private static void prompt(ServerPlayer player, CompoundTag party) {
        long now = player.serverLevel().getGameTime();
        if (now < NEXT_PROMPT.getOrDefault(player.getUUID(), 0L)) return;
        boolean leader = party.getUUID("owner").equals(player.getUUID());
        String command = (leader ? "/smp forfeit " : "/smp encounter-leave ") + party.getUUID("id");
        player.sendSystemMessage(Component.translatable(leader ? "jem.event.boundary_forfeit_question" : "jem.event.boundary_leave_question")
                .withStyle(ChatFormatting.GOLD).append(" ")
                .append(Component.translatable(leader ? "jem.event.boundary_forfeit_confirm" : "jem.event.boundary_leave_confirm")
                        .withStyle(style -> style.withColor(ChatFormatting.RED).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable(leader ? "jem.event.boundary_forfeit_warning" : "jem.event.boundary_leave_warning"))))));
        NEXT_PROMPT.put(player.getUUID(), now + PROMPT_COOLDOWN_TICKS);
        FORFEIT_UNTIL.put(player.getUUID(), now + FORFEIT_CONFIRMATION_TICKS);
    }

    private static double distanceToEdge(BoundingBox bounds, Vec3 position) {
        return Math.min(Math.min(position.x - bounds.minX(), bounds.maxX() + 1D - position.x),
                Math.min(position.z - bounds.minZ(), bounds.maxZ() + 1D - position.z));
    }

    private static Boundary boundary(MinecraftServer server, CompoundTag party) {
        ResourceLocation dimension = ResourceLocation.tryParse(party.getString("dimension"));
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        int[] structure = party.getIntArray("structureBounds");
        return level == null || structure.length != 6 ? null
                : new Boundary(level, new BoundingBox(structure[0], structure[1], structure[2], structure[3], structure[4], structure[5]));
    }

    private record Boundary(ServerLevel level, BoundingBox bounds) {}

    private HostedBoundary() {}
}
