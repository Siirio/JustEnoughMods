package com.siirio.jemserver.smp.events;

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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class EventTravel {
    private static final int CHUNK_TICKET_RADIUS = 2;
    private static final TicketType<Integer> EVENT_TRAVEL_TICKET = TicketType.create("jem_event_travel", Integer::compareTo);
    private static final Map<UUID, PendingTravel> PENDING = new HashMap<>();

    private EventTravel() {
    }

    public static void request(ServerPlayer player, UUID eventId, boolean inside) {
        var event = SmpData.get(player.server).find("events", eventId);
        if (event == null || SmpData.closed(event)) return;
        inside=inside&&event.getString("activity").equals("RESOURCE_RUSH");
        ServerLevel level = EventRegions.level(player.server, event);
        if (level == null) return;
        BlockPos anchor = inside ? BlockPos.of(event.getLong("position")) : outsideAnchor(event, player.blockPosition());
        ChunkPos chunk = new ChunkPos(anchor);
        PendingTravel previous = PENDING.put(player.getUUID(), new PendingTravel(eventId, inside, level, chunk, anchor, player.getId()));
        if (previous != null) previous.release();
        level.getChunkSource().addRegionTicket(EVENT_TRAVEL_TICKET, chunk, CHUNK_TICKET_RADIUS, player.getId());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        Iterator<Map.Entry<UUID, PendingTravel>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            PendingTravel travel = entry.getValue();
            if (player == null) {
                travel.release();
                iterator.remove();
                continue;
            }
            var row = SmpData.get(event.getServer()).find("events", travel.eventId());
            if (row == null || SmpData.closed(row)) {
                travel.release();
                iterator.remove();
                continue;
            }
            var target = travel.inside()
                    ? EventRegions.safeInside(travel.level(), row, travel.anchor())
                    : EventRegions.safeOutside(travel.level(), row, travel.anchor());
            if (target.isEmpty()) continue;
            BlockPos pos = target.get();
            player.teleportTo(travel.level(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            travel.release();
            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppingEvent event) {
        PENDING.values().forEach(PendingTravel::release);
        PENDING.clear();
    }

    private static BlockPos outsideAnchor(net.minecraft.nbt.CompoundTag event, BlockPos center) {
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

    private record PendingTravel(UUID eventId, boolean inside, ServerLevel level, ChunkPos chunk, BlockPos anchor, int playerId) {
        private void release() {
            level.getChunkSource().removeRegionTicket(EVENT_TRAVEL_TICKET, chunk, CHUNK_TICKET_RADIUS, playerId);
        }
    }
}
