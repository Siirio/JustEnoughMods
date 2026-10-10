package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.claims.Claims;
import com.siirio.jemserver.smp.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
    public static boolean contains(CompoundTag event, ServerLevel level, BlockPos pos) {
        if (!event.getString("dimension").equals(level.dimension().location().toString()))
            return false;
        return pos.getX() >= minX(event) && pos.getX() <= maxX(event) && pos.getZ() >= minZ(event) && pos.getZ() <= maxZ(event);
    }
    public static boolean contains(CompoundTag event, ServerLevel level, AABB box) {
        if (!event.getString("dimension").equals(level.dimension().location().toString())) return false;
        return box.minX >= minX(event) && box.maxX <= maxX(event) + 1D
                && box.minZ >= minZ(event) && box.maxZ <= maxZ(event) + 1D;
    }
    public static int minX(CompoundTag row) { return row.contains("minX") ? row.getInt("minX") : BlockPos.of(row.getLong("position")).getX()-row.getInt("radius"); }
    public static int maxX(CompoundTag row) { return row.contains("maxX") ? row.getInt("maxX") : BlockPos.of(row.getLong("position")).getX()+row.getInt("radius"); }
    public static int minZ(CompoundTag row) { return row.contains("minZ") ? row.getInt("minZ") : BlockPos.of(row.getLong("position")).getZ()-row.getInt("radius"); }
    public static int maxZ(CompoundTag row) { return row.contains("maxZ") ? row.getInt("maxZ") : BlockPos.of(row.getLong("position")).getZ()+row.getInt("radius"); }

    public static ServerLevel level(MinecraftServer server, CompoundTag event) {
        ResourceLocation dimension = ResourceLocation.tryParse(event.getString("dimension"));
        return dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }

    public static void locate(CompoundTag event, ServerLevel level, BlockPos center,
                              int minX, int minZ, int maxX, int maxZ) {
        locate(event, level.dimension().location().toString(), center, minX, minZ, maxX, maxZ);
    }

    public static void locate(CompoundTag event, String dimension, BlockPos center,
                              int minX, int minZ, int maxX, int maxZ) {
        event.putString("dimension", dimension);
        event.putLong("position", center.asLong());
        event.putInt("radius", Math.max(maxX - minX + 1, maxZ - minZ + 1) / 2);
        event.putInt("minX", minX);
        event.putInt("maxX", maxX);
        event.putInt("minZ", minZ);
        event.putInt("maxZ", maxZ);
    }

    public static boolean near(CompoundTag event, ServerLevel level, BlockPos pos) {
        if (!event.getString("dimension").equals(level.dimension().location().toString())) return false;
        int xDistance=Math.max(Math.max(minX(event)-pos.getX(),0),pos.getX()-maxX(event));
        int zDistance=Math.max(Math.max(minZ(event)-pos.getZ(),0),pos.getZ()-maxZ(event));
        return Math.max(xDistance,zDistance)<=EventRules.ENTRY_PROMPT_DISTANCE.get();
    }


    public static void eject(ServerPlayer player, CompoundTag event) {
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

    public static void eject(Mob mob,CompoundTag event) {
        if(!(mob.level() instanceof ServerLevel level)) return;
        var safe=safeOutside(level,event,mob.blockPosition());
        if(safe.isEmpty()) return;
        var pos=safe.get();
        mob.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        mob.setDeltaMovement(0,0,0);
        mob.getNavigation().stop();
        mob.fallDistance=0;
    }

    public static void ejectOutsiders(ServerLevel level,CompoundTag event) {
        var bounds=new net.minecraft.world.phys.AABB(minX(event),level.getMinBuildHeight(),minZ(event),maxX(event)+1,level.getMaxBuildHeight(),maxZ(event)+1);
        for(var mob:level.getEntitiesOfClass(Mob.class,bounds,candidate->!BloodMoon.belongs(event,candidate)&&!RaidEvent.belongs(event,candidate))) eject(mob,event);
    }

    public static boolean returnOrigin(ServerPlayer player, CompoundTag member) {
        ResourceLocation dimension=ResourceLocation.tryParse(member.getString("originDimension"));
        if(dimension==null) return false;
        var target=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
        if(target==null) return false;
        player.teleportTo(target,member.getDouble("originX"),member.getDouble("originY"),member.getDouble("originZ"),member.getFloat("originYaw"),member.getFloat("originPitch"));
        player.fallDistance=0;
        return true;
    }

    public static Optional<BlockPos> safeInside(ServerLevel level,CompoundTag event,BlockPos preferred) {
        var center=BlockPos.of(event.getLong("position"));
        return safe(level,event,preferred,true).or(()->safe(level,event,center,true));
    }

    public static Optional<BlockPos> safeOutside(ServerLevel level,CompoundTag event,BlockPos preferred) {
        int x=preferred.getX(),z=preferred.getZ();
        int left=Math.abs(x-minX(event)),right=Math.abs(maxX(event)-x),top=Math.abs(z-minZ(event)),bottom=Math.abs(maxZ(event)-z);
        int edge=Math.min(Math.min(left,right),Math.min(top,bottom));
        BlockPos anchor=edge==left?new BlockPos(minX(event)-2,preferred.getY(),z)
                :edge==right?new BlockPos(maxX(event)+2,preferred.getY(),z)
                :edge==top?new BlockPos(x,preferred.getY(),minZ(event)-2)
                :new BlockPos(x,preferred.getY(),maxZ(event)+2);
        return safe(level,event,anchor,false);
    }

    private static Optional<BlockPos> safe(ServerLevel level,CompoundTag event,BlockPos anchor,boolean inside) {
        for(int radius=0;radius<=8;radius++) for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
                if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius) continue;
                int x=anchor.getX()+dx,z=anchor.getZ()+dz;
                boolean constrained=inside&&event.contains("minY")&&event.contains("maxY");
                int minY=constrained?Math.max(level.getMinBuildHeight()+1,event.getInt("minY")):level.getMinBuildHeight()+1;
                int maxY=constrained?Math.min(level.getMaxBuildHeight()-2,event.getInt("maxY")):level.getMaxBuildHeight()-2;
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

    private static boolean isSafe(ServerLevel level,CompoundTag event,BlockPos position,boolean inside) {
        if(inside!=contains(event,level,position)||!level.getWorldBorder().isWithinBounds(position)) return false;
        var floor=level.getBlockState(position.below());
        return floor.isFaceSturdy(level,position.below(),net.minecraft.core.Direction.UP)
                &&level.getFluidState(position).isEmpty()&&level.getFluidState(position.above()).isEmpty()
                &&level.getBlockState(position).getCollisionShape(level,position).isEmpty()
                &&level.getBlockState(position.above()).getCollisionShape(level,position.above()).isEmpty()
                &&!floor.is(net.minecraft.world.level.block.Blocks.LAVA)&&!floor.is(net.minecraft.world.level.block.Blocks.FIRE)
                &&!floor.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)&&!floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
    }

    public static boolean reserve(MinecraftServer server, CompoundTag event) {
        var level = server.overworld();
        int chunks = SmpConfig.EVENT_REGION_CHUNKS.get();
        int r = chunks * 8;
        boolean manual = event.getString("slot").startsWith("manual:");
        int claimBuffer = SmpConfig.EVENT_CLAIM_BUFFER.get();
        var claims = Claims.territories(server);
        if(event.contains("requestedPosition"))
            return reserveRequested(server,event,level,BlockPos.of(event.getLong("requestedPosition")),chunks,claimBuffer,claims);
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
            int rotation = Math.floorMod(event.getUUID("id").hashCode(), candidates.size());
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
            if (SmpData.get(server).all("events").stream()
                    .anyMatch(
                            other ->
                                    other != event
                                            && other.getString("state").equals("ACTIVE")
                                            && other.getString("dimension")
                                                    .equals(level.dimension().location().toString())
                                            && minX <= maxX(other) && maxX >= minX(other) && minZ <= maxZ(other) && maxZ >= minZ(other))) continue;
            if (claims.stream()
                    .anyMatch(
                            claim ->
                                    claim.dimension().equals(level.dimension().location())
                                            && claim.minX() <= maxX + claimBuffer
                                            && claim.maxX() >= minX - claimBuffer
                                            && claim.minZ() <= maxZ + claimBuffer
                                            && claim.maxZ() >= minZ - claimBuffer)) continue;
            if (anchor.player() != null && !Claims.bossAreaAvailable(anchor.player(), new BlockPos(minX,center.getY(),minZ), new BlockPos(maxX,center.getY(),maxZ)))
                continue;
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

    private static boolean reserveRequested(MinecraftServer server,CompoundTag event,ServerLevel level,BlockPos requested,
                                            int chunks,int claimBuffer,java.util.List<Claims.Territory> claims) {
        int size=chunks*16;
        int radius=size/2;
        int minX=requested.getX()-radius;
        int minZ=requested.getZ()-radius;
        int maxX=minX+size-1;
        int maxZ=minZ+size-1;
        BlockPos minimum=new BlockPos(minX,level.getMinBuildHeight(),minZ);
        BlockPos maximum=new BlockPos(maxX,level.getMaxBuildHeight()-1,maxZ);
        if(!level.getWorldBorder().isWithinBounds(minimum)||!level.getWorldBorder().isWithinBounds(maximum)) return false;
        if(SmpData.get(server).all("events").stream().anyMatch(other->other!=event
                &&other.getString("state").equals("ACTIVE")
                &&other.getString("dimension").equals(level.dimension().location().toString())
                &&minX<=maxX(other)&&maxX>=minX(other)&&minZ<=maxZ(other)&&maxZ>=minZ(other))) return false;
        if(claims.stream().anyMatch(claim->claim.dimension().equals(level.dimension().location())
                &&claim.minX()<=maxX+claimBuffer&&claim.maxX()>=minX-claimBuffer
                &&claim.minZ()<=maxZ+claimBuffer&&claim.maxZ()>=minZ-claimBuffer)) return false;
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

    public static boolean claimAllowed(
            net.minecraft.server.level.ServerPlayer player, Claims.Area area) {
        return SmpData.get(player.server).all("events").stream()
                .filter(
                        e ->
                                e.getString("state").equals("ACTIVE")
                                        && e.getString("dimension")
                                                .equals(
                                                        player.level()
                                                                .dimension()
                                                                .location()
                                                                .toString()))
                .noneMatch(
                        e -> {
                            int buffer = SmpConfig.EVENT_CLAIM_BUFFER.get();
                            return area.minX() <= maxX(e) + buffer && area.maxX() >= minX(e) - buffer
                                    && area.minZ() <= maxZ(e) + buffer && area.maxZ() >= minZ(e) - buffer;
                        });
    }

    private EventRegions() {}
}
