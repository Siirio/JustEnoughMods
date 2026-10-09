package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.Parties;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
    private static final String SESSION = "partyTeleportSession";
    private static final String RETURN_PENDING = "partyReturnPending";
    private static final String RETURN_DIMENSION = "partyReturnDimension";
    private static final String RETURN_X = "partyReturnX";
    private static final String RETURN_Y = "partyReturnY";
    private static final String RETURN_Z = "partyReturnZ";
    private static final String RETURN_YAW = "partyReturnYaw";
    private static final String RETURN_PITCH = "partyReturnPitch";
    private static final int SEARCH_RADIUS = 6;
    private static final int VERTICAL_SEARCH = 4;

    public static void begin(ServerPlayer host, CompoundTag party, Collection<UUID> participants) {
        clear(party);
        party.putUUID(SESSION, UUID.randomUUID());
        Set<BlockPos> occupied = new HashSet<>();
        occupied.add(host.blockPosition());
        for (UUID id : participants) {
            ServerPlayer player = host.server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            CompoundTag member = SmpRecords.members(party).getCompound(id.toString());
            saveReturn(player, member);
            SmpRecords.members(party).put(id.toString(), member);
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
        SmpData.get(host.server).changed(party);
    }

    public static void finish(MinecraftServer server, CompoundTag party) {
        if (!party.hasUUID(SESSION)) return;
        for (UUID id : SmpRecords.memberIds(party)) {
            if (!Parties.accepted(party, id)) continue;
            CompoundTag member = SmpRecords.members(party).getCompound(id.toString());
            clearArenaAccess(member);
            if (!member.contains(RETURN_DIMENSION)) continue;
            member.putBoolean(RETURN_PENDING, true);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) prompt(player, party.getUUID("id"),party.getUUID(SESSION));
        }
        SmpData.get(server).changed(party);
    }

    public static boolean pending(CompoundTag party, UUID playerId) {
        return party.hasUUID(SESSION) && SmpRecords.members(party).getCompound(playerId.toString()).getBoolean(RETURN_PENDING);
    }

    public static boolean choose(ServerPlayer player, CompoundTag party, UUID session, boolean returnBack) {
        if (!party.hasUUID(SESSION)||!party.getUUID(SESSION).equals(session)||!pending(party, player.getUUID())) return false;
        CompoundTag member = SmpRecords.members(party).getCompound(player.getStringUUID());
        boolean completed = !returnBack || teleportBack(player, member);
        if (!completed) return false;
        clearReturn(member);
        clearSessionIfResolved(party);
        SmpData.get(player.server).changed(party);
        return true;
    }

    public static boolean returnNow(ServerPlayer player, CompoundTag party) {
        CompoundTag member = SmpRecords.members(party).getCompound(player.getStringUUID());
        if (!member.contains(RETURN_DIMENSION) || !teleportBack(player, member)) return false;
        clearReturn(member);
        clearSessionIfResolved(party);
        SmpData.get(player.server).changed(party);
        return true;
    }

    public static void clear(CompoundTag party) {
        party.remove(SESSION);
        for (String id : SmpRecords.members(party).getAllKeys()) {
            CompoundTag member = SmpRecords.members(party).getCompound(id);
            clearReturn(member);
            clearArenaAccess(member);
        }
    }

    private static void saveReturn(ServerPlayer player, CompoundTag member) {
        Vec3 position = player.position();
        member.putString(RETURN_DIMENSION, player.serverLevel().dimension().location().toString());
        member.putDouble(RETURN_X, position.x);
        member.putDouble(RETURN_Y, position.y);
        member.putDouble(RETURN_Z, position.z);
        member.putFloat(RETURN_YAW, player.getYRot());
        member.putFloat(RETURN_PITCH, player.getXRot());
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

    private static boolean teleportBack(ServerPlayer player, CompoundTag member) {
        ResourceLocation dimension = ResourceLocation.tryParse(member.getString(RETURN_DIMENSION));
        ServerLevel level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return false;
        player.teleportTo(level, member.getDouble(RETURN_X), member.getDouble(RETURN_Y), member.getDouble(RETURN_Z),
                member.getFloat(RETURN_YAW), member.getFloat(RETURN_PITCH));
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

    private static void clearReturn(CompoundTag member) {
        member.remove(RETURN_PENDING);
        member.remove(RETURN_DIMENSION);
        member.remove(RETURN_X);
        member.remove(RETURN_Y);
        member.remove(RETURN_Z);
        member.remove(RETURN_YAW);
        member.remove(RETURN_PITCH);
    }

    private static void clearArenaAccess(CompoundTag member) {
        member.remove("arenaEntered");
        member.remove("arenaInsideX");
        member.remove("arenaInsideY");
        member.remove("arenaInsideZ");
    }

    private static void clearSessionIfResolved(CompoundTag party) {
        boolean pending = SmpRecords.memberIds(party).stream().anyMatch(id ->
                SmpRecords.members(party).getCompound(id.toString()).getBoolean(RETURN_PENDING));
        if (!pending) party.remove(SESSION);
    }

    private PartyTeleportFlow() {}
}
