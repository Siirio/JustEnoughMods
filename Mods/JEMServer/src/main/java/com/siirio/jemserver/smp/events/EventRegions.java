package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

public final class EventRegions {
    private static final double ENTRY_REJECTION=.35;
    public static boolean contains(EventState event, ServerLevel level, BlockPos pos) {
        if (!event.dimension().equals(level.dimension().location().toString()))
            return false;
        var structure = event.structureBounds();
        return pos.getX() >= minX(event) && pos.getX() <= maxX(event) && pos.getZ() >= minZ(event) && pos.getZ() <= maxZ(event)
                && (structure == null || pos.getY() >= structure.minY() && pos.getY() <= structure.maxY());
    }
    public static boolean contains(EventState event, ServerLevel level, AABB box) {
        if (!event.dimension().equals(level.dimension().location().toString())) return false;
        var structure = event.structureBounds();
        return box.minX >= minX(event) && box.maxX <= maxX(event) + 1D
                && box.minZ >= minZ(event) && box.maxZ <= maxZ(event) + 1D
                && (structure == null || box.minY >= structure.minY() && box.maxY <= structure.maxY() + 1D);
    }
    public static int minX(EventState row) { return row.hasMinX() ? row.minX() : BlockPos.of(row.position()).getX()-row.radius(); }
    public static int maxX(EventState row) { return row.hasMaxX() ? row.maxX() : BlockPos.of(row.position()).getX()+row.radius(); }
    public static int minZ(EventState row) { return row.hasMinZ() ? row.minZ() : BlockPos.of(row.position()).getZ()-row.radius(); }
    public static int maxZ(EventState row) { return row.hasMaxZ() ? row.maxZ() : BlockPos.of(row.position()).getZ()+row.radius(); }

    public static ServerLevel level(MinecraftServer server, EventState event) {
        ResourceLocation dimension = ResourceLocation.tryParse(event.dimension());
        return dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }

    public static void locate(EventState event, ServerLevel level, BlockPos center,
                              int minX, int minZ, int maxX, int maxZ) {
        locate(event, level.dimension().location().toString(), center, minX, minZ, maxX, maxZ);
    }

    public static void locate(EventState event, String dimension, BlockPos center,
                              int minX, int minZ, int maxX, int maxZ) {
        event.dimension( dimension);
        event.position( center.asLong());
        event.radius( Math.max(maxX - minX + 1, maxZ - minZ + 1) / 2);
        event.minX( minX);
        event.maxX( maxX);
        event.minZ( minZ);
        event.maxZ( maxZ);
    }

    public static void locate(PartyState party, String dimension, BlockPos center,
                              int minX, int minZ, int maxX, int maxZ) {
        party.dimension(dimension); party.position(center.asLong());
        party.radius(Math.max(maxX - minX + 1, maxZ - minZ + 1) / 2);
        party.minX(minX); party.maxX(maxX); party.minZ(minZ); party.maxZ(maxZ);
    }

    public static boolean near(EventState event, ServerLevel level, BlockPos pos) {
        if (!event.dimension().equals(level.dimension().location().toString())) return false;
        int xDistance=Math.max(Math.max(minX(event)-pos.getX(),0),pos.getX()-maxX(event));
        int zDistance=Math.max(Math.max(minZ(event)-pos.getZ(),0),pos.getZ()-maxZ(event));
        var structure = event.structureBounds();
        int yDistance = structure != null ? Math.max(Math.max(structure.minY() - pos.getY(), 0), pos.getY() - structure.maxY()) : 0;
        return Math.max(yDistance,Math.max(xDistance,zDistance))<=EventRules.ENTRY_PROMPT_DISTANCE.get();
    }


    public static void eject(ServerPlayer player, EventState event) {
        var safe=safeOutside(player.serverLevel(),event,player.blockPosition());
        if(safe.isPresent()) {
            var pos=safe.get();
            player.teleportTo(player.serverLevel(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYRot(),player.getXRot());
            player.setDeltaMovement(0,0,0);
            player.fallDistance=0;
            return;
        }
        var previous=BlockPos.containing(player.xo,player.yo,player.zo);
        if(!contains(event,player.serverLevel(),previous)) {
            player.teleportTo(player.serverLevel(),player.xo,player.yo,player.zo,player.getYRot(),player.getXRot());
            var motion=player.getDeltaMovement();
            player.setDeltaMovement(-motion.x*ENTRY_REJECTION,motion.y,-motion.z*ENTRY_REJECTION);
            player.fallDistance=0;
            return;
        }
        double left=Math.abs(player.getX()-minX(event));
        double right=Math.abs(maxX(event)-player.getX());
        double top=Math.abs(player.getZ()-minZ(event));
        double bottom=Math.abs(maxZ(event)-player.getZ());
        double edge=Math.min(Math.min(left,right),Math.min(top,bottom));
        double x=player.getX(),z=player.getZ();
        if(edge==left) x=minX(event)-.5;
        else if(edge==right) x=maxX(event)+1.5;
        else if(edge==top) z=minZ(event)-.5;
        else z=maxZ(event)+1.5;
        player.teleportTo(player.serverLevel(),x,player.getY(),z,player.getYRot(),player.getXRot());
        player.fallDistance=0;
    }

    public static void eject(Mob mob,EventState event) {
        if(!(mob.level() instanceof ServerLevel level)) return;
        var safe=safeOutside(level,event,mob.blockPosition());
        if(safe.isEmpty()) return;
        var pos=safe.get();
        mob.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        mob.setDeltaMovement(0,0,0);
        mob.getNavigation().stop();
        mob.fallDistance=0;
    }

    public static void ejectOutsiders(ServerLevel level,EventState event) {
        var structure = event.structureBounds();
        int minY = structure != null ? structure.minY() : level.getMinBuildHeight();
        int maxY = structure != null ? structure.maxY() + 1 : level.getMaxBuildHeight();
        var bounds=new net.minecraft.world.phys.AABB(minX(event),minY,minZ(event),maxX(event)+1,maxY,maxZ(event)+1);
        for(var mob:level.getEntitiesOfClass(Mob.class,bounds,candidate->!BloodMoon.belongs(event,candidate)
                &&!RaidEvent.belongs(event,candidate)&&!EventSession.admitted(event,candidate))) eject(mob,event);
    }

    public static boolean returnOrigin(ServerPlayer player, EventMember member) {
        ResourceLocation dimension=ResourceLocation.tryParse(member.originDimension());
        if(dimension==null) return false;
        var target=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
        if(target==null) return false;
        player.teleportTo(target,member.originX(),member.originY(),member.originZ(),member.originYaw(),member.originPitch());
        player.fallDistance=0;
        return true;
    }

    public static Optional<BlockPos> safeInside(ServerLevel level,EventState event,BlockPos preferred) {
        var center=BlockPos.of(event.position());
        return safe(level,event,preferred,true).or(()->safe(level,event,center,true));
    }

    public static Optional<BlockPos> safeOutside(ServerLevel level,EventState event,BlockPos preferred) {
        int x=preferred.getX(),z=preferred.getZ();
        int left=Math.abs(x-minX(event)),right=Math.abs(maxX(event)-x),top=Math.abs(z-minZ(event)),bottom=Math.abs(maxZ(event)-z);
        int edge=Math.min(Math.min(left,right),Math.min(top,bottom));
        BlockPos anchor=edge==left?new BlockPos(minX(event)-2,preferred.getY(),z)
                :edge==right?new BlockPos(maxX(event)+2,preferred.getY(),z)
                :edge==top?new BlockPos(x,preferred.getY(),minZ(event)-2)
                :new BlockPos(x,preferred.getY(),maxZ(event)+2);
        return safe(level,event,anchor,false);
    }

    private static Optional<BlockPos> safe(ServerLevel level,EventState event,BlockPos anchor,boolean inside) {
        for(int radius=0;radius<=8;radius++) for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
                if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius) continue;
                int x=anchor.getX()+dx,z=anchor.getZ()+dz;
                boolean constrained=inside&&event.hasMinY()&&event.hasMaxY();
                int minY=constrained?Math.max(level.getMinBuildHeight()+1,event.minY()):level.getMinBuildHeight()+1;
                int maxY=constrained?Math.min(level.getMaxBuildHeight()-2,event.maxY()):level.getMaxBuildHeight()-2;
                int anchorY=Math.max(minY,Math.min(maxY,anchor.getY()));
                var column=new BlockPos(x,anchorY,z);
                if(!level.hasChunkAt(column)) continue;
                if(!constrained) {
                    var position=new BlockPos(x,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
                    if(isSafe(level,event,position,inside)) return Optional.of(position);
                    continue;
                }
                for(int delta=0;delta<=maxY-minY;delta++) for(int direction:delta==0?new int[]{0}:new int[]{delta,-delta}) {
                    int y=anchorY+direction;
                    if(y<minY||y>maxY) continue;
                    var pos=new BlockPos(x,y,z);
                    if(isSafe(level,event,pos,inside)) return Optional.of(pos);
                }
        }
        return Optional.empty();
    }

    private static boolean isSafe(ServerLevel level,EventState event,BlockPos position,boolean inside) {
        if(inside!=contains(event,level,position)||!level.getWorldBorder().isWithinBounds(position)) return false;
        var floor=level.getBlockState(position.below());
        return floor.isFaceSturdy(level,position.below(),net.minecraft.core.Direction.UP)
                &&level.getFluidState(position).isEmpty()&&level.getFluidState(position.above()).isEmpty()
                &&level.getBlockState(position).getCollisionShape(level,position).isEmpty()
                &&level.getBlockState(position.above()).getCollisionShape(level,position.above()).isEmpty()
                &&!floor.is(net.minecraft.world.level.block.Blocks.LAVA)&&!floor.is(net.minecraft.world.level.block.Blocks.FIRE)
                &&!floor.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)&&!floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
    }

    public static boolean reserve(MinecraftServer server, EventState event) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return reserveInScope(server, event); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> reserveInScope(server, store.events().bind(event)));
    }

    private static boolean reserveInScope(MinecraftServer server, EventState event) {
        var level = server.overworld();
        int chunks = SmpConfig.EVENT_REGION_CHUNKS.get();
        int r = chunks * 8;
        boolean manual = event.slot().startsWith("manual:");
        if(event.hasRequestedPosition())
            return reserveRequested(server,event,level,BlockPos.of(event.requestedPosition()),chunks);
        record Anchor(ServerPlayer player, BlockPos position) {}
        var anchors = new ArrayList<Anchor>();
        for (var player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel() != level || player.isSpectator() || !player.isAlive())
                continue;
            anchors.add(new Anchor(player, player.blockPosition()));
        }
        if (manual && anchors.isEmpty()) anchors.add(new Anchor(null, level.getSharedSpawnPos()));
        for (var anchor : anchors) {
            var candidates = new ArrayList<BlockPos>();
            int spacing = chunks + 1;
            for (int chunkX = -spacing; chunkX <= spacing; chunkX += spacing)
                for (int chunkZ = -spacing; chunkZ <= spacing; chunkZ += spacing)
                    candidates.add(anchor.position().offset(chunkX << 4, 0, chunkZ << 4));
            int rotation = Math.floorMod(event.id().hashCode(), candidates.size());
            Collections.rotate(candidates, rotation);
            for (var origin : candidates) {
            int minX = ((origin.getX() >> 4) - chunks / 2) << 4;
            int minZ = ((origin.getZ() >> 4) - chunks / 2) << 4;
            int maxX = minX + chunks * 16 - 1, maxZ = minZ + chunks * 16 - 1;
            int centerX=minX+r,centerZ=minZ+r;
            var center = new BlockPos(centerX, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,centerX,centerZ), centerZ);
            long distanceX =
                    Math.max(
                            0,
                            Math.abs((long) center.getX() - level.getSharedSpawnPos().getX()) - r);
            long distanceZ =
                    Math.max(
                            0,
                            Math.abs((long) center.getZ() - level.getSharedSpawnPos().getZ()) - r);
            if (!manual && distanceX * distanceX + distanceZ * distanceZ
                    < (long) SmpConfig.EVENT_DISTANCE.get() * SmpConfig.EVENT_DISTANCE.get())
                continue;
            if (SmpData.get(server).events().currentRows().stream()
                    .anyMatch(
                            other ->
                                    other != event
                                            && other.state().equals("ACTIVE")
                                            && other.dimension()
                                                    .equals(level.dimension().location().toString())
                                            && minX <= maxX(other) && maxX >= minX(other) && minZ <= maxZ(other) && maxZ >= minZ(other))) continue;
            boolean safe = true;
            for (int x = minX >> 4; x <= maxX >> 4 && safe; x++)
                for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                    var chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk == null || !manual && !chunk.getBlockEntities().isEmpty()) {
                        safe = false;
                        break;
                    }
                }
            if (!safe) continue;
            if (!enoughLand(level,minX,minZ,maxX,maxZ)) continue;
            locate(event, level, center, minX, minZ, maxX, maxZ);
            return true;
            }
        }
        return false;
    }

    private static boolean reserveRequested(MinecraftServer server,EventState event,ServerLevel level,BlockPos requested,
                                            int chunks) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return reserveRequestedInScope(server, event, level, requested, chunks); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> reserveRequestedInScope(server, store.events().bind(event), level, requested, chunks));
    }

    private static boolean reserveRequestedInScope(MinecraftServer server,EventState event,ServerLevel level,BlockPos requested,
                                            int chunks) {
        int size=chunks*16;
        int radius=size/2;
        int minX=requested.getX()-radius;
        int minZ=requested.getZ()-radius;
        int maxX=minX+size-1;
        int maxZ=minZ+size-1;
        BlockPos minimum=new BlockPos(minX,level.getMinBuildHeight(),minZ);
        BlockPos maximum=new BlockPos(maxX,level.getMaxBuildHeight()-1,maxZ);
        if(!level.getWorldBorder().isWithinBounds(minimum)||!level.getWorldBorder().isWithinBounds(maximum)) return false;
        if(SmpData.get(server).events().currentRows().stream().anyMatch(other->other!=event
                &&other.state().equals("ACTIVE")
                &&other.dimension().equals(level.dimension().location().toString())
                &&minX<=maxX(other)&&maxX>=minX(other)&&minZ<=maxZ(other)&&maxZ>=minZ(other))) return false;
        for(int chunkX=minX>>4;chunkX<=maxX>>4;chunkX++)
            for(int chunkZ=minZ>>4;chunkZ<=maxZ>>4;chunkZ++) level.getChunk(chunkX,chunkZ);
        if(!enoughLand(level,minX,minZ,maxX,maxZ)) return false;
        BlockPos center=new BlockPos(requested.getX(),level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,requested.getX(),requested.getZ()),requested.getZ());
        locate(event,level,center,minX,minZ,maxX,maxZ);
        return true;
    }

    private static boolean enoughLand(ServerLevel level,int minX,int minZ,int maxX,int maxZ) {
        int total=(maxX-minX+1)*(maxZ-minZ+1);
        int required=(int)Math.ceil(total*EventRules.MINIMUM_LAND_PERCENT.get()/100.0);
        int land=0,checked=0;
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
            checked++;
            int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
            if(supportedSurface(level,x,surface,z)) land++;
            if(land+total-checked<required) return false;
        }
        return land>=required;
    }

    private static boolean supportedSurface(ServerLevel level,int x,int surface,int z) {
        for(int depth=0;depth<4;depth++) {
            BlockPos pos=new BlockPos(x,surface-depth,z);
            var state=level.getBlockState(pos);
            if(!state.getFluidState().isEmpty()) return false;
            if(state.isFaceSturdy(level,pos,net.minecraft.core.Direction.UP)) return true;
        }
        return false;
    }

    private EventRegions() {}
}
