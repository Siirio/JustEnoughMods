package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class BloodMoonSolidBoundary {
    private static final double WALL_THICKNESS=.25D;
    private static volatile List<EventNetwork.Boundary> clientZones=List.of();

    public static void clientZones(List<EventNetwork.Boundary> zones) {
        clientZones=zones.stream().filter(zone->zone.type().equals("BLOOD_MOON")&&zone.active()).toList();
    }

    public static Vec3 collide(Entity entity,Vec3 movement) {
        if(entity.isSpectator()||movement.lengthSqr()==0) return movement;
        if(entity.level().isClientSide) {
            if(!Boolean.TRUE.equals(net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    ()->()->localMovementEntity(entity)))) return movement;
            return collide(entity,movement,clientZones.stream().filter(zone->!zone.passable())
                    .filter(zone->zone.dimension().equals(entity.level().dimension().location().toString())).toList());
        }
        if(!(entity.level() instanceof ServerLevel level)) return movement;
        Vec3 constrained=movement;
        for(var row:SmpData.get(level.getServer()).all("events")) {
            if(!row.getString("state").equals("ACTIVE")||!row.getString("dimension").equals(level.dimension().location().toString())) continue;
            String type=row.getString("activity");
            if(type.equals("BLOOD_MOON")) {
                if(!row.getBoolean("combatStarted")||!bloodMoonEntity(entity,row)||!EventRegions.contains(row,level,entity.getBoundingBox())) continue;
            } else if(type.equals("RESOURCE_RUSH")) {
                if(!(entity instanceof Mob)||level.players().stream().noneMatch(player->EventRegions.contains(row,level,player.blockPosition()))) continue;
            } else continue;
            EventNetwork.Boundary boundary=boundary(row,level);
            constrained=type.equals("RESOURCE_RUSH")?collideResourceRush(entity.getBoundingBox(),constrained,boundary)
                    :new Wall(boundary).collide(entity.getBoundingBox(),constrained);
            if(constrained.lengthSqr()==0) break;
        }
        return constrained;
    }

    public static BossTeleportSafety.Decision validateTeleport(ServerPlayer player,ServerLevel level,Vec3 requested) {
        for(var row:SmpData.get(player.server).all("events")) {
            if(!row.getString("state").equals("ACTIVE")||!row.getString("activity").equals("BLOOD_MOON")
                    ||!row.getBoolean("combatStarted")||!new EventSession(row).accepted(player.getUUID())) continue;
            if(validDestination(player,level,row,requested)) return BossTeleportSafety.Decision.allow(requested);
            return BossTeleportSafety.Decision.redirect(safeDestination(player,row));
        }
        return BossTeleportSafety.Decision.unmatched();
    }

    private static boolean localMovementEntity(Entity entity) {
        net.minecraft.client.player.LocalPlayer player=net.minecraft.client.Minecraft.getInstance().player;
        return player!=null&&(player==entity||player.getRootVehicle()==entity);
    }

    private static Vec3 collide(Entity entity,Vec3 movement,List<EventNetwork.Boundary> boundaries) {
        Vec3 constrained=movement;
        for(EventNetwork.Boundary boundary:boundaries) {
            constrained=new Wall(boundary).collide(entity.getBoundingBox(),constrained);
            if(constrained.lengthSqr()==0) break;
        }
        return constrained;
    }

    private static Vec3 collideResourceRush(AABB box,Vec3 movement,EventNetwork.Boundary boundary) {
        List<VoxelShape> shapes=new Wall(boundary).shapes();
        double x=Shapes.collide(Direction.Axis.X,box,shapes,movement.x);
        if(x!=0) box=box.move(x,0,0);
        double z=Shapes.collide(Direction.Axis.Z,box,shapes,movement.z);
        return new Vec3(x,movement.y,z);
    }

    private static boolean bloodMoonEntity(Entity entity,net.minecraft.nbt.CompoundTag row) {
        EventSession session=new EventSession(row);
        boolean playerPassenger=hasPlayer(entity);
        if(playerPassenger) return hasParticipant(entity,session);
        return entity instanceof Mob;
    }

    private static boolean hasPlayer(Entity entity) {
        if(entity instanceof ServerPlayer) return true;
        for(Entity passenger:entity.getPassengers()) if(hasPlayer(passenger)) return true;
        return false;
    }

    private static boolean hasParticipant(Entity entity,EventSession session) {
        if(entity instanceof ServerPlayer player&&session.accepted(player.getUUID())) return true;
        for(Entity passenger:entity.getPassengers()) if(hasParticipant(passenger,session)) return true;
        return false;
    }

    private static Vec3 safeDestination(ServerPlayer player,net.minecraft.nbt.CompoundTag row) {
        ServerLevel level=EventRegions.level(player.server,row);
        if(level==null) return null;
        Vec3 current=player.position();
        if(player.serverLevel()==level&&validDestination(player,level,row,current)) return current;
        var member=SmpRecords.members(row).getCompound(player.getStringUUID());
        if(member.contains("safeX")) {
            Vec3 saved=new Vec3(member.getDouble("safeX"),member.getDouble("safeY"),member.getDouble("safeZ"));
            if(validDestination(player,level,row,saved)) return saved;
        }
        BlockPos respawn=new EncounterContext(row).respawnPoint(level);
        Vec3 saved=respawn==null?null:Vec3.atBottomCenterOf(respawn);
        return saved!=null&&validDestination(player,level,row,saved)?saved:null;
    }

    private static boolean validDestination(ServerPlayer player,ServerLevel level,net.minecraft.nbt.CompoundTag row,Vec3 destination) {
        if(level!=EventRegions.level(player.server,row)) return false;
        BlockPos position=BlockPos.containing(destination);
        if(!level.hasChunkAt(position)||!level.getWorldBorder().isWithinBounds(position)) return false;
        AABB box=player.getBoundingBox().move(destination.x-player.getX(),destination.y-player.getY(),destination.z-player.getZ());
        EventNetwork.Boundary boundary=boundary(row,level);
        return EventRegions.contains(row,level,box)&&!BossSolidBoundary.intersectsWall(box,boundary)&&level.noCollision(player,box);
    }

    private static EventNetwork.Boundary boundary(net.minecraft.nbt.CompoundTag row,ServerLevel level) {
        return new EventNetwork.Boundary(row.getUUID("id"),row.getString("dimension"),"BLOOD_MOON",
                EventRegions.minX(row),level.getMinBuildHeight(),EventRegions.minZ(row),EventRegions.maxX(row),
                level.getMaxBuildHeight()-1,EventRegions.maxZ(row),EventRules.BLOOD_COLOR.get(),true,false);
    }

    private record Wall(EventNetwork.Boundary boundary) {
        private List<VoxelShape> shapes() {
            double minX=boundary.minX();
            double minZ=boundary.minZ();
            double maxX=boundary.maxX()+1D;
            double maxZ=boundary.maxZ()+1D;
            double minY=boundary.minY();
            double maxY=boundary.maxY()+1D;
            List<VoxelShape> result=new ArrayList<>(4);
            result.add(Shapes.create(new AABB(minX-WALL_THICKNESS,minY,minZ-WALL_THICKNESS,minX,maxY,maxZ+WALL_THICKNESS)));
            result.add(Shapes.create(new AABB(maxX,minY,minZ-WALL_THICKNESS,maxX+WALL_THICKNESS,maxY,maxZ+WALL_THICKNESS)));
            result.add(Shapes.create(new AABB(minX,minY,minZ-WALL_THICKNESS,maxX,maxY,minZ)));
            result.add(Shapes.create(new AABB(minX,minY,maxZ,maxX,maxY,maxZ+WALL_THICKNESS)));
            return result;
        }

        private Vec3 collide(AABB box,Vec3 movement) {
            List<VoxelShape> shapes=shapes();
            AABB swept=box.expandTowards(movement).inflate(WALL_THICKNESS);
            if(shapes.stream().noneMatch(shape->shape.bounds().intersects(swept))) return movement;
            double y=Shapes.collide(Direction.Axis.Y,box,shapes,movement.y);
            if(y!=0) box=box.move(0,y,0);
            boolean zFirst=Math.abs(movement.x)<Math.abs(movement.z);
            double first=Shapes.collide(zFirst?Direction.Axis.Z:Direction.Axis.X,box,shapes,zFirst?movement.z:movement.x);
            if(first!=0) box=zFirst?box.move(0,0,first):box.move(first,0,0);
            double second=Shapes.collide(zFirst?Direction.Axis.X:Direction.Axis.Z,box,shapes,zFirst?movement.x:movement.z);
            return zFirst?new Vec3(second,y,first):new Vec3(first,y,second);
        }
    }

    private BloodMoonSolidBoundary() {}
}
