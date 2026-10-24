package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.SmpData;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server")
public final class EventTravel {
    enum Destination { EVENT_OUTSIDE, RESOURCE_INSIDE, BOSS_STRUCTURE }
    static final int CHUNK_TICKET_RADIUS = 2;
    static final TicketType<Integer> EVENT_TRAVEL_TICKET = TicketType.create("jem_event_travel", Integer::compareTo);
    private static final Map<UUID, PendingEventTravel> PENDING = new HashMap<>();

    private EventTravel() {
    }

    public static void request(ServerPlayer player, UUID eventId, boolean inside) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { requestInScope(player, eventId, inside); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> requestInScope(player, eventId, inside));
    }

    private static void requestInScope(ServerPlayer player, UUID eventId, boolean inside) {
        if (!eligiblePlayer(player)) return;
        var event = SmpData.get(player.server).events().current(eventId);
        if (!eligibleEvent(player, event)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error.official_destination_unavailable"));
            return;
        }
        inside=inside&&event.activity().equals("RESOURCE_RUSH");
        ServerLevel level = EventRegions.level(player.server, event);
        if (level == null) return;
        BlockPos anchor = inside ? BlockPos.of(event.position()) : outsideAnchor(event, player.blockPosition());
        queue(player, eventId, inside ? Destination.RESOURCE_INSIDE : Destination.EVENT_OUTSIDE, level, anchor);
    }

    public static void requestBoss(ServerPlayer player, UUID destinationId) {
        if (!eligiblePlayer(player)) return;
        var target = bossTarget(player, destinationId);
        if (target == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error.official_destination_unavailable"));
            return;
        }
        BlockPos anchor = StructureStaging.outsideAnchor(shell(target.arena().bounds()), player.blockPosition());
        queue(player, destinationId, Destination.BOSS_STRUCTURE, target.level(), anchor);
    }

    private static void queue(ServerPlayer player, UUID destinationId, Destination type, ServerLevel level, BlockPos anchor) {
        ChunkPos chunk = new ChunkPos(anchor);
        PendingEventTravel previous = PENDING.put(player.getUUID(), new PendingEventTravel(destinationId, type, level, chunk, anchor, player.getId(), level.getGameTime() + EventRules.TRAVEL_TIMEOUT_TICKS.get()));
        if (previous != null) previous.release();
        level.getChunkSource().addRegionTicket(EVENT_TRAVEL_TICKET, chunk, CHUNK_TICKET_RADIUS, player.getId());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        var serverForMutation = event.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { tickInScope(event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> tickInScope(event));
    }

    private static void tickInScope(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        Iterator<Map.Entry<UUID, PendingEventTravel>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            PendingEventTravel travel = entry.getValue();
            if (player == null || player.getId() != travel.playerId() || !eligiblePlayer(player)
                    || travel.level().getGameTime() >= travel.expiresAtTick()) {
                if (player != null) player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error.no_safe_arrival"));
                travel.release();
                iterator.remove();
                continue;
            }
            if (travel.type() == Destination.BOSS_STRUCTURE && bossTarget(player, travel.destinationId()) == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error.official_destination_unavailable"));
                travel.release();
                iterator.remove();
                continue;
            }
            if (travel.type() != Destination.BOSS_STRUCTURE) {
                var row = SmpData.get(event.getServer()).events().current(travel.destinationId());
                if (!eligibleEvent(player, row) || EventRegions.level(event.getServer(), row) != travel.level()) {
                    travel.release();
                    iterator.remove();
                    continue;
                }
            }
            BlockPos pos = resolve(event.getServer(), player, travel);
            if (pos == null) continue;
            if (!safeArrival(player, travel.level(), pos)) continue;
            travel.release();
            iterator.remove();
            Vec3 destination = Vec3.atBottomCenterOf(pos);
            BossTeleportSafety.Decision decision = BossTeleportSafety.validate(player, travel.level(), destination);
            if (decision.matched() && !destination.equals(decision.destination())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error.teleport_contained"));
                continue;
            }
            player.closeContainer();
            player.stopRiding();
            BossTeleportSafety.apply(player, travel.level(), destination, player.getYRot(), player.getXRot());
        }
    }

    private static BlockPos resolve(net.minecraft.server.MinecraftServer server, ServerPlayer player, PendingEventTravel travel) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return resolveInScope(server, player, travel); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> resolveInScope(server, player, travel));
    }

    private static BlockPos resolveInScope(net.minecraft.server.MinecraftServer server, ServerPlayer player, PendingEventTravel travel) {
        if (travel.type() == Destination.BOSS_STRUCTURE) {
            BossTarget target = bossTarget(player, travel.destinationId());
            if (target == null || target.level() != travel.level()) return null;
            return StructureStaging.safeOutside(player, target.level(), shell(target.arena().bounds()), travel.anchor());
        }
        var row = SmpData.get(server).events().current(travel.destinationId());
        if (row == null || SmpData.closed(row)) return null;
        if (travel.type() == Destination.RESOURCE_INSIDE)
            return EventRegions.safeInside(travel.level(), row, travel.anchor()).orElse(null);
        int centerY = BlockPos.of(row.position()).getY();
        int minY = row.hasMinY() ? row.minY() : centerY;
        int maxY = row.hasMaxY() ? row.maxY() : centerY;
        var bounds = new net.minecraft.world.level.levelgen.structure.BoundingBox(EventRegions.minX(row), minY, EventRegions.minZ(row),
                EventRegions.maxX(row), maxY, EventRegions.maxZ(row));
        return StructureStaging.safeOutside(player, travel.level(), bounds, travel.anchor());
    }

    private static BossTarget bossTarget(ServerPlayer player, UUID destinationId) {
        if (!eligiblePlayer(player)) return null;
        var landmark = com.siirio.jemserver.ServerData.get(player.server).landmark(destinationId);
        var arena = com.siirio.jemworldbosstiers.api.RaidArenaApi.arena(player.server, destinationId).orElse(null);
        if (landmark == null || !com.siirio.jemserver.Landmarks.isBoss(landmark) || arena == null
                || !landmark.dimension().equals(arena.dimension()) || !arena.bounds().isInside(landmark.position())
                || !com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(player, arena.entityType())) return null;
        ServerLevel level = player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, arena.dimension()));
        return level == null ? null : new BossTarget(level, arena);
    }

    private static net.minecraft.world.level.levelgen.structure.BoundingBox shell(net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        return new net.minecraft.world.level.levelgen.structure.BoundingBox(bounds.minX() - 1, bounds.minY() - 1, bounds.minZ() - 1,
                bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppingEvent event) {
        PENDING.values().forEach(PendingEventTravel::release);
        PENDING.clear();
    }

    private static BlockPos outsideAnchor(EventState event, BlockPos center) {
        int left=Math.abs(center.getX()-EventRegions.minX(event));
        int right=Math.abs(EventRegions.maxX(event)-center.getX());
        int top=Math.abs(center.getZ()-EventRegions.minZ(event));
        int bottom=Math.abs(EventRegions.maxZ(event)-center.getZ());
        int edge=Math.min(Math.min(left,right),Math.min(top,bottom));
        int x=net.minecraft.util.Mth.clamp(center.getX(),EventRegions.minX(event),EventRegions.maxX(event));
        int z=net.minecraft.util.Mth.clamp(center.getZ(),EventRegions.minZ(event),EventRegions.maxZ(event));
        if(edge==left) return new BlockPos(EventRegions.minX(event)-2,center.getY(),z);
        if(edge==right) return new BlockPos(EventRegions.maxX(event)+2,center.getY(),z);
        if(edge==top) return new BlockPos(x,center.getY(),EventRegions.minZ(event)-2);
        return new BlockPos(x,center.getY(),EventRegions.maxZ(event)+2);
    }

    public static void cancel(UUID playerId) {
        PendingEventTravel pending = PENDING.remove(playerId);
        if (pending != null) pending.release();
    }

    @SubscribeEvent
    public static void changedDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        cancel(event.getEntity().getUUID());
    }

    private record BossTarget(ServerLevel level, com.siirio.jemworldbosstiers.api.RaidArenaApi.Arena arena) {}

    private static boolean eligiblePlayer(ServerPlayer player) {
        return com.siirio.jemserver.smp.SmpEnvironment.active(player) && player.isAlive() && !player.isSpectator()
                && !player.isSleeping() && !player.isPassenger() && !player.isVehicle() && com.siirio.jemidentity.api.IdentityPolicy.resolve(player).isPresent();
    }

    private static boolean eligibleEvent(ServerPlayer player, EventState event) {
        return EventRuntime.available(player.server, event) && event.pendingOutcome().isEmpty() && "ACTIVE".equals(event.state())
                && event.ends() > System.currentTimeMillis() && event.hasPosition()
                && java.util.Set.of("BLOOD_MOON", "BOSS_RAID", "RESOURCE_RUSH").contains(event.activity())
                && new EventSession(event).accepted(player.getUUID());
    }

    private static boolean safeArrival(ServerPlayer player, ServerLevel level, BlockPos position) {
        var box = player.getDimensions(player.getPose()).makeBoundingBox(position.getX() + .5, position.getY(), position.getZ() + .5);
        if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box)) return false;
        for (BlockPos occupied : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7)))
            if (!level.hasChunkAt(occupied) || !level.getFluidState(occupied).isEmpty()) return false;
        return level.noCollision(player, box) && StructureStaging.safeOutside(player, level, position);
    }
}
