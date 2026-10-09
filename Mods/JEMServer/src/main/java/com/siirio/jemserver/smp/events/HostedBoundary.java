package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.Parties;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HostedBoundary {
    private static final String ENTERED="arenaEntered";
    private static final String INSIDE_X="arenaInsideX";
    private static final String INSIDE_Y="arenaInsideY";
    private static final String INSIDE_Z="arenaInsideZ";
    private static final String ENTRANCE_DIMENSION="arenaEntranceDimension";
    private static final String ENTRANCE_X="arenaEntranceX";
    private static final String ENTRANCE_Y="arenaEntranceY";
    private static final String ENTRANCE_Z="arenaEntranceZ";
    private static final double PROMPT_DISTANCE=1.5;
    private static final double FORFEIT_DISTANCE=4;
    private static final double ENTRY_DISTANCE_SQUARED=4;
    private static final long PROMPT_COOLDOWN_TICKS=60;
    private static final long FORFEIT_CONFIRMATION_TICKS=100;
    private static final double SAFE_POSITION_CLEARANCE=.75;
    private static final Map<UUID,Long> NEXT_PROMPT=new HashMap<>();
    private static final Map<UUID,Long> FORFEIT_UNTIL=new HashMap<>();

    public static void tick(MinecraftServer server) {
        for(CompoundTag party:SmpData.get(server).all("parties")) {
            if(!party.getString("state").equals("ACTIVE")||!party.getString("activity").equals("BOSS")) continue;
            if(party.getBoolean("bloodMoon")) continue;
            Boundary boundary=boundary(server,party);
            if(boundary==null) continue;
            for(ServerPlayer player:server.getPlayerList().getPlayers()) {
                if(player==null||!player.isAlive()||player.isSpectator()) continue;
                enforce(player,party,boundary);
            }
        }
    }

    public static boolean canEnter(ServerPlayer player,CompoundTag party,BoundingBox bounds) {
        return party.getString("state").equals("ACTIVE")&&Parties.accepted(party,player.getUUID());
    }

    public static void captureEntrances(MinecraftServer server,CompoundTag party,java.util.Collection<UUID> participants) {
        Boundary boundary=boundary(server,party);
        if(boundary==null) return;
        for(UUID id:participants) {
            ServerPlayer player=server.getPlayerList().getPlayer(id);
            if(player==null||player.serverLevel()!=boundary.level()||inside(player,boundary,player.position())) continue;
            CompoundTag member=SmpRecords.members(party).getCompound(id.toString());
            member.putString(ENTRANCE_DIMENSION,boundary.level().dimension().location().toString());
            member.putDouble(ENTRANCE_X,player.getX());
            member.putDouble(ENTRANCE_Y,player.getY());
            member.putDouble(ENTRANCE_Z,player.getZ());
        }
        SmpData.get(server).changed(party);
    }

    public static boolean canForfeit(ServerPlayer player,CompoundTag party) {
        Boundary boundary=boundary(player.server,party);
        if(boundary==null||player.serverLevel()!=boundary.level()) return false;
        CompoundTag member=SmpRecords.members(party).getCompound(player.getStringUUID());
        if(!member.getBoolean(ENTERED)||!boundary.bounds().isInside(player.blockPosition())) return false;
        long now=player.serverLevel().getGameTime();
        return distanceToEdge(boundary.bounds(),player.position())<=FORFEIT_DISTANCE
                ||now<=FORFEIT_UNTIL.getOrDefault(player.getUUID(),0L);
    }

    public static void clear(UUID player) {
        NEXT_PROMPT.remove(player);
        FORFEIT_UNTIL.remove(player);
    }

    public static void clear() {
        NEXT_PROMPT.clear();
        FORFEIT_UNTIL.clear();
    }

    private static void enforce(ServerPlayer player,CompoundTag party,Boundary boundary) {
        CompoundTag member=SmpRecords.members(party).getCompound(player.getStringUUID());
        Vec3 current=player.position();
        boolean sameDimension=player.serverLevel()==boundary.level();
        AABB insideBounds=BoundaryCollision.inside(boundary.bounds(),player.getBbWidth(),player.getBbHeight());
        AABB safeBounds=BoundaryCollision.inside(boundary.bounds(),player.getBbWidth(),player.getBbHeight(),SAFE_POSITION_CLEARANCE);
        boolean inside=sameDimension&&insideBounds.contains(current);
        if(inside) {
            if(!authorizeWalkIn(player,party,boundary.bounds(),new Vec3(player.xo,player.yo,player.zo),current)) {
                eject(player,party,boundary,current);
                return;
            }
            if(safeBounds.contains(current)) rememberInside(member,current);
            if(distanceToEdge(boundary.bounds(),current)<=PROMPT_DISTANCE) prompt(player,party);
            return;
        }
        if(!member.getBoolean(ENTERED)) return;
        prompt(player,party);
    }

    static ArenaTeleportSafety.Decision validateTeleport(ServerPlayer player,ServerLevel level,Vec3 requested) {
        for(CompoundTag party:SmpData.get(player.server).all("parties")) {
            if(!party.getString("state").equals("ACTIVE")||!party.getString("activity").equals("BOSS")||party.getBoolean("bloodMoon")) continue;
            Boundary boundary=boundary(player.server,party);
            if(boundary==null||boundary.level()!=level||!inside(player,boundary,requested)) continue;
            if(legalInside(player,party)) return ArenaTeleportSafety.Decision.allow(requested);
            BlockPos outside=outside(player,party,boundary,BlockPos.containing(requested));
            return ArenaTeleportSafety.Decision.redirect(outside==null?null:Vec3.atBottomCenterOf(outside));
        }
        return ArenaTeleportSafety.Decision.unmatched();
    }

    static boolean legalInside(ServerPlayer player,CompoundTag party) {
        if(!Parties.accepted(party,player.getUUID())) return false;
        CompoundTag member=SmpRecords.members(party).getCompound(player.getStringUUID());
        if(!member.getBoolean(ENTERED)||member.getBoolean("eliminated")) return false;
        if(party.hasUUID("eventId")) {
            CompoundTag event=SmpData.get(player.server).find("events",party.getUUID("eventId"));
            if(event!=null&&SmpRecords.members(event).getCompound(player.getStringUUID()).getBoolean("eliminated")) return false;
        }
        if(party.hasUUID("bossEntity")&&player.serverLevel().getEntity(party.getUUID("bossEntity")) instanceof net.minecraft.world.entity.LivingEntity boss
                &&com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(boss).stream()
                .anyMatch(status->status.playerId().equals(player.getUUID())&&status.state().equals("ELIMINATED"))) return false;
        return true;
    }

    static boolean authorizeWalkIn(ServerPlayer player,CompoundTag party,BoundingBox bounds,Vec3 previous,Vec3 current) {
        if(legalInside(player,party)) return true;
        if(!canEnter(player,party,bounds)||!crossedEntrance(BoundaryCollision.inside(bounds,player.getBbWidth(),player.getBbHeight()),previous,current)) return false;
        SmpRecords.members(party).getCompound(player.getStringUUID()).putBoolean(ENTERED,true);
        SmpData.get(player.server).changed(party);
        return true;
    }

    private static boolean crossedEntrance(AABB inside,Vec3 previous,Vec3 current) {
        return !inside.contains(previous)&&inside.contains(current)&&previous.distanceToSqr(current)<=ENTRY_DISTANCE_SQUARED;
    }

    private static boolean inside(ServerPlayer player,Boundary boundary,Vec3 position) {
        return BoundaryCollision.inside(boundary.bounds(),player.getBbWidth(),player.getBbHeight()).contains(position);
    }

    private static void eject(ServerPlayer player,CompoundTag party,Boundary boundary,Vec3 requested) {
        BlockPos outside=outside(player,party,boundary,BlockPos.containing(requested));
        if(outside==null) return;
        player.teleportTo(boundary.level(),outside.getX()+.5,outside.getY(),outside.getZ()+.5,player.getYRot(),player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance=0;
    }

    private static BlockPos outside(ServerPlayer player,CompoundTag party,Boundary boundary,BlockPos requested) {
        CompoundTag member=SmpRecords.members(party).getCompound(player.getStringUUID());
        ResourceLocation dimension=ResourceLocation.tryParse(member.getString(ENTRANCE_DIMENSION));
        if(dimension!=null&&dimension.equals(boundary.level().dimension().location())&&member.contains(ENTRANCE_X)
                &&member.contains(ENTRANCE_Y)&&member.contains(ENTRANCE_Z)) {
            BlockPos entrance=BlockPos.containing(member.getDouble(ENTRANCE_X),member.getDouble(ENTRANCE_Y),member.getDouble(ENTRANCE_Z));
            if(!inside(player,boundary,Vec3.atBottomCenterOf(entrance))&&StructureStaging.safeOutside(player,boundary.level(),entrance)) return entrance;
        }
        return StructureStaging.safeOutside(player,boundary.level(),boundary.bounds(),requested);
    }

    private static void rememberInside(CompoundTag member,Vec3 position) {
        member.putDouble(INSIDE_X,position.x);
        member.putDouble(INSIDE_Y,position.y);
        member.putDouble(INSIDE_Z,position.z);
    }


    private static void prompt(ServerPlayer player,CompoundTag party) {
        long now=player.serverLevel().getGameTime();
        if(now<NEXT_PROMPT.getOrDefault(player.getUUID(),0L)) return;
        boolean leader=party.getUUID("owner").equals(player.getUUID());
        String command=(leader?"/smp forfeit ":"/smp encounter-leave ")+party.getUUID("id");
        player.sendSystemMessage(Component.translatable(leader?"jem.event.boundary_forfeit_question":"jem.event.boundary_leave_question")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" "))
                .append(Component.translatable(leader?"jem.event.boundary_forfeit_confirm":"jem.event.boundary_leave_confirm").withStyle(style->style
                        .withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.translatable(leader?"jem.event.boundary_forfeit_warning":"jem.event.boundary_leave_warning"))))));
        NEXT_PROMPT.put(player.getUUID(),now+PROMPT_COOLDOWN_TICKS);
        FORFEIT_UNTIL.put(player.getUUID(),now+FORFEIT_CONFIRMATION_TICKS);
    }

    private static double distanceToEdge(BoundingBox bounds,Vec3 position) {
        return Math.min(Math.min(position.x-bounds.minX(),bounds.maxX()+1-position.x),
                Math.min(position.z-bounds.minZ(),bounds.maxZ()+1-position.z));
    }

    static Boundary boundary(MinecraftServer server,CompoundTag party) {
        ResourceLocation dimension=ResourceLocation.tryParse(party.getString("dimension"));
        ServerLevel level=dimension==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
        if(level==null) return null;
        int[] structure=party.getIntArray("structureBounds");
        if(structure.length==6) return new Boundary(level,new BoundingBox(structure[0],structure[1],structure[2],structure[3],structure[4],structure[5]));
        if(party.hasUUID("eventId")) {
            CompoundTag event=SmpData.get(server).find("events",party.getUUID("eventId"));
            if(event!=null&&event.contains("minX")&&event.contains("maxX")&&event.contains("minZ")&&event.contains("maxZ")) {
                int minY=event.contains("minY")?event.getInt("minY"):level.getMinBuildHeight();
                int maxY=event.contains("maxY")?event.getInt("maxY"):level.getMaxBuildHeight()-1;
                return new Boundary(level,new BoundingBox(event.getInt("minX"),minY,event.getInt("minZ"),event.getInt("maxX"),maxY,event.getInt("maxZ")));
            }
        }
        if(party.hasUUID("bossEntity")&&level.getEntity(party.getUUID("bossEntity")) instanceof net.minecraft.world.entity.LivingEntity boss)
            return new Boundary(level,BossStaging.bounds(boss));
        return null;
    }

    record Boundary(ServerLevel level,BoundingBox bounds) {}
    private HostedBoundary() {}
}
