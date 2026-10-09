package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class EventAreaHooks {
    private static final String RUSH_BOUNDARY_EVENT="jem:resource_rush_boundary_event";
    private static final String RUSH_BOUNDARY_INSIDE="jem:resource_rush_boundary_inside";
    private static final String RUSH_BOUNDARY_X="jem:resource_rush_boundary_x";
    private static final String RUSH_BOUNDARY_Y="jem:resource_rush_boundary_y";
    private static final String RUSH_BOUNDARY_Z="jem:resource_rush_boundary_z";
    private static final String PROJECTILE_BOUNDARY_EVENT="jem:event_boundary_id";
    private static final String PROJECTILE_BOUNDARY_INSIDE="jem:event_boundary_inside";
    private static final String PROJECTILE_BOUNDARY_COOLDOWN="jem:event_boundary_cooldown";
    private static final double PROJECTILE_BOUNDARY_CLEARANCE=.08;
    private static final double ANIMAL_BOUNDARY_REJECTION=.25;
    static final double PROJECTILE_RESTITUTION_REDUCTION=.2;
    private static final Map<UUID,Set<UUID>> INSIDE=new HashMap<>();
    private static final Map<UUID,Set<UUID>> PROMPTED=new HashMap<>();
    private static final Map<UUID,List<EventNetwork.Boundary>> SENT=new HashMap<>();
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer player)) return;
        ReviveSupport.enforce(player);
        if(player.tickCount%10!=0) return;
        var inside=new HashSet<UUID>();
        var before=INSIDE.getOrDefault(player.getUUID(),Set.of());
        var prompted=new HashSet<UUID>();
        var promptedBefore=PROMPTED.getOrDefault(player.getUUID(),Set.of());
        var zones=new ArrayList<EventNetwork.Boundary>(StructureStaging.boundaries(player));
        for(var row:EventScheduler.active(player.server)) {
            if(!row.contains("radius")) continue;
            String type=row.getString("activity");UUID id=row.getUUID("id");
            boolean within=EventRegions.contains(row,player.serverLevel(),player.blockPosition());
            boolean near=EventRegions.near(row,player.serverLevel(),player.blockPosition());
            if(within) inside.add(id);
            if(near) prompted.add(id);
            if(type.equals("RESOURCE_RUSH") && within!=before.contains(id)) player.sendSystemMessage(Component.translatable(within?"jem.event.rush_enter":"jem.event.rush_leave"));
            boolean member=new EncounterContext(row).isParticipant(player.getUUID());
            if(!row.getBoolean("combatStarted") && near && !promptedBefore.contains(id) && !member && (type.equals("BLOOD_MOON") || type.equals("BOSS_RAID")))
                player.sendSystemMessage(Component.literal("▣ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("jem.smp."+type)).append(" ")
                        .append(Component.literal("[Открыть событие]").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/smp event-open " + id)))));
            if(type.equals("BLOOD_MOON") && member && player.isAlive() && !player.isSpectator()) {
                var entry=SmpRecords.members(row).getCompound(player.getStringUUID());
                if(within) {
                    if(entry.getBoolean("awaitingReturn")) { entry.remove("awaitingReturn");SmpData.get(player.server).changed(row); }
                    entry.putDouble("safeX",player.getX());entry.putDouble("safeY",player.getY());entry.putDouble("safeZ",player.getZ());
                }
            }
            if((!type.equals("BOSS_RAID")||row.hasUUID("targetBoss")) && zones.size()<32 && row.getString("dimension").equals(player.level().dimension().location().toString())) {
                double distance=Math.max(Math.max(EventRegions.minX(row)-player.getX(),player.getX()-EventRegions.maxX(row)),Math.max(EventRegions.minZ(row)-player.getZ(),player.getZ()-EventRegions.maxZ(row)));
                if(distance<=EventRules.BOUNDARY_DISTANCE.get()) {
                    CompoundTag party=row.hasUUID("party")?SmpData.get(player.server).find("parties",row.getUUID("party")):null;
                    boolean participating=party!=null&&party.getString("state").equals("ACTIVE")&&Parties.accepted(party,player.getUUID());
                    var encounter=new EncounterContext(row);
                    boolean completed=encounter.state()==EncounterContext.State.COMPLETED;
                    boolean active=type.equals("BLOOD_MOON")&&!completed;
                    boolean passable=type.equals("BLOOD_MOON")&&(row.getBoolean("combatStarted")||participating);
                    zones.add(new EventNetwork.Boundary(id,row.getString("dimension"),type,EventRegions.minX(row),player.serverLevel().getMinBuildHeight(),EventRegions.minZ(row),EventRegions.maxX(row),player.serverLevel().getMaxBuildHeight()-1,EventRegions.maxZ(row),type.equals("BLOOD_MOON")?EventRules.BLOOD_COLOR.get():type.equals("RESOURCE_RUSH")?EventRules.RUSH_COLOR.get():EventRules.RAID_COLOR.get(),active,passable));
                }
            }
        }
        if(!zones.equals(SENT.get(player.getUUID()))) { EventNetwork.zones(player,zones);SENT.put(player.getUUID(),List.copyOf(zones)); }
        INSIDE.put(player.getUUID(),inside);
        PROMPTED.put(player.getUUID(),prompted);
    }

    public static void prompt(ServerPlayer player, UUID id) {
        CompoundTag row = SmpData.get(player.server).find("events", id);
        SmpRecords.require(row != null && row.getString("state").equals("ACTIVE") && !row.getBoolean("combatStarted"), "unavailable");
        String type = row.getString("activity");
        SmpRecords.require((type.equals("BLOOD_MOON") || type.equals("BOSS_RAID")) && EventRegions.near(row, player.serverLevel(), player.blockPosition()), "travel_to_arena");
        EventNetwork.prompt(player, id, type, Component.translatable("jem.smp." + type).getString(), "");
    }

    @SubscribeEvent
    public static void animalBoundary(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.animal.Animal animal)
                || !(animal.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var row=EventScheduler.active(level.getServer(),"RESOURCE_RUSH");
        if(row==null || !row.getString("dimension").equals(level.dimension().location().toString())) return;
        var data=animal.getPersistentData();
        UUID eventId=row.getUUID("id");
        boolean inside=EventRegions.contains(row,level,animal.blockPosition());
        if(!data.hasUUID(RUSH_BOUNDARY_EVENT) || !data.getUUID(RUSH_BOUNDARY_EVENT).equals(eventId)) {
            rememberAnimalPosition(data,eventId,inside,animal);
            return;
        }
        if(inside!=data.getBoolean(RUSH_BOUNDARY_INSIDE)) {
            animal.teleportTo(data.getDouble(RUSH_BOUNDARY_X),data.getDouble(RUSH_BOUNDARY_Y),data.getDouble(RUSH_BOUNDARY_Z));
            var motion=animal.getDeltaMovement();
            animal.setDeltaMovement(-motion.x*ANIMAL_BOUNDARY_REJECTION,motion.y,-motion.z*ANIMAL_BOUNDARY_REJECTION);
            animal.getNavigation().stop();
            animal.fallDistance=0;
            return;
        }
        rememberAnimalPosition(data,eventId,inside,animal);
    }

    @SubscribeEvent
    public static void combatMobBoundary(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for(String type:List.of("BLOOD_MOON","BOSS_RAID")) {
            var row=EventScheduler.active(level.getServer(),type);
            if(row==null || !row.getBoolean("combatStarted") || !EventRegions.contains(row,level,mob.blockPosition())) continue;
            boolean belongs=type.equals("BLOOD_MOON")?BloodMoon.belongs(row,mob):RaidEvent.belongs(row,mob);
            if(!belongs) EventRegions.eject(mob,row);
            return;
        }
    }

    private static void rememberAnimalPosition(CompoundTag data,UUID eventId,boolean inside,net.minecraft.world.entity.animal.Animal animal) {
        data.putUUID(RUSH_BOUNDARY_EVENT,eventId);
        data.putBoolean(RUSH_BOUNDARY_INSIDE,inside);
        data.putDouble(RUSH_BOUNDARY_X,animal.getX());
        data.putDouble(RUSH_BOUNDARY_Y,animal.getY());
        data.putDouble(RUSH_BOUNDARY_Z,animal.getZ());
    }


    @SubscribeEvent public static void projectiles(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.START || !(event.level instanceof net.minecraft.server.level.ServerLevel level)) return;
        for(var row:EventScheduler.active(level.getServer())) {
            String type=row.getString("activity");
            if(!row.getString("state").equals("ACTIVE") || !row.getString("dimension").equals(level.dimension().location().toString()) || !type.equals("BLOOD_MOON")) continue;
            var bounds=new net.minecraft.world.phys.AABB(EventRegions.minX(row)-1,level.getMinBuildHeight(),EventRegions.minZ(row)-1,EventRegions.maxX(row)+2,level.getMaxBuildHeight(),EventRegions.maxZ(row)+2);
            for(var projectile:level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,bounds)) {
                boolean inside=inside(row,projectile.getX(),projectile.getZ());
                var data=projectile.getPersistentData();
                UUID eventId=row.getUUID("id");
                if(!data.hasUUID(PROJECTILE_BOUNDARY_EVENT) || !data.getUUID(PROJECTILE_BOUNDARY_EVENT).equals(eventId)) {
                    data.putUUID(PROJECTILE_BOUNDARY_EVENT,eventId);
                    data.putBoolean(PROJECTILE_BOUNDARY_INSIDE,inside);
                    continue;
                }
                boolean wasInside=data.getBoolean(PROJECTILE_BOUNDARY_INSIDE);
                if(wasInside==inside) continue;
                if(data.getLong(PROJECTILE_BOUNDARY_COOLDOWN)>level.getGameTime()) {
                    data.putBoolean(PROJECTILE_BOUNDARY_INSIDE,inside);
                    continue;
                }
                var motion=projectile.getDeltaMovement();
                var start=new net.minecraft.world.phys.Vec3(projectile.xo,projectile.yo,projectile.zo);
                var end=projectile.position();
                var boundary=new net.minecraft.world.phys.AABB(EventRegions.minX(row),level.getMinBuildHeight(),EventRegions.minZ(row),
                        EventRegions.maxX(row)+1.0,level.getMaxBuildHeight(),EventRegions.maxZ(row)+1.0);
                var hit=BoundaryCollision.intersection(boundary,start,end).orElse(start);
                var direction=end.subtract(start);
                var stopped=direction.lengthSqr()==0?start:hit.subtract(direction.normalize().scale(PROJECTILE_BOUNDARY_CLEARANCE));
                projectile.setPos(stopped.x,stopped.y,stopped.z);
                projectile.xo=stopped.x;
                projectile.yo=stopped.y;
                projectile.zo=stopped.z;
                if(projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow) projectile.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                else {
                    double restitution=EventRules.PROJECTILE_RESTITUTION.get()*PROJECTILE_RESTITUTION_REDUCTION;
                    projectile.setDeltaMovement(BoundaryCollision.reflect(boundary,hit,motion).scale(restitution));
                }
                projectile.hasImpulse=true;
                data.putLong(PROJECTILE_BOUNDARY_COOLDOWN,level.getGameTime()+2);
                data.putBoolean(PROJECTILE_BOUNDARY_INSIDE,inside(row,stopped.x,stopped.z));
            }
        }
    }
    private static boolean inside(CompoundTag row,double x,double z) {
        return x>=EventRegions.minX(row)&&x<EventRegions.maxX(row)+1.0&&z>=EventRegions.minZ(row)&&z<EventRegions.maxZ(row)+1.0;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void target(LivingChangeTargetEvent event) {
        var mob=event.getEntity();var tag=mob.getPersistentData();
        if(!tag.hasUUID(EventSession.SESSION) || mob.level().isClientSide) return;
        var target=event.getNewTarget();
        if(target==null) return;
        var row=SmpData.get(mob.getServer()).find("events",tag.getUUID(EventSession.SESSION));
        if(row!=null&&row.getString("activity").equals("BLOOD_MOON")&&!BloodMoon.canTarget(mob,target)) {
            event.setCanceled(true);
            return;
        }
        if(!(target instanceof ServerPlayer player) || row==null || !new EventSession(row).accepted(player.getUUID()) || !player.isAlive() || player.isSpectator() || !EventRegions.contains(row,player.serverLevel(),player.blockPosition())) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingAttackEvent event) {
        if(event.getEntity().level().isClientSide) return;
        var attacker=EventSession.owner(event.getSource().getEntity());
        if(attacker==null) attacker=EventSession.owner(event.getSource().getDirectEntity());
        var victim=event.getEntity();
        if(attacker==null) return;
        if(attacker instanceof Mob && victim instanceof Mob
                && attacker.getPersistentData().hasUUID(BloodMoon.EVENT_ID)
                && victim.getPersistentData().hasUUID(BloodMoon.EVENT_ID)
                && attacker.getPersistentData().getUUID(BloodMoon.EVENT_ID).equals(victim.getPersistentData().getUUID(BloodMoon.EVENT_ID))) {
            event.setCanceled(true);
            return;
        }
        var sourceSession = attacker instanceof ServerPlayer sourcePlayer ? EventSession.forPlayer(sourcePlayer) : null;
        var targetSession = victim instanceof ServerPlayer targetPlayer ? EventSession.forPlayer(targetPlayer) : null;
        UUID sourceId = sourceSession != null && sourceSession.getBoolean("combatStarted") ? sourceSession.getUUID("id")
                : attacker.getPersistentData().hasUUID(EventSession.SESSION) ? attacker.getPersistentData().getUUID(EventSession.SESSION) : null;
        UUID targetId = targetSession != null && targetSession.getBoolean("combatStarted") ? targetSession.getUUID("id")
                : victim.getPersistentData().hasUUID(EventSession.SESSION) ? victim.getPersistentData().getUUID(EventSession.SESSION) : null;
        if ((sourceId != null || targetId != null) && (!Objects.equals(sourceId, targetId)
                || attacker instanceof ServerPlayer && victim instanceof ServerPlayer)) {
            event.setCanceled(true);
            return;
        }
        var source=attacker.getPersistentData();var target=victim.getPersistentData();
        if(source.hasUUID(EventSession.SESSION)) {
            var row=SmpData.get(victim.getServer()).find("events",source.getUUID(EventSession.SESSION));
            if(!(victim instanceof ServerPlayer player) || row==null || !new EventSession(row).accepted(player.getUUID()) || !EventRegions.contains(row,player.serverLevel(),player.blockPosition())) event.setCanceled(true);
        }
        if(victim instanceof ServerPlayer player && attacker instanceof ServerPlayer other) {
            var row=EventSession.forPlayer(player);
            if(row!=null && new EventSession(row).accepted(other.getUUID())) event.setCanceled(true);
            for(var party:SmpData.get(player.server).all("parties")) {
                if(party.getBoolean("nativePending") && party.getString("activity").equals("BOSS")
                        && party.getString("state").equals("ACTIVE") && Parties.accepted(party,player.getUUID())
                        && Parties.accepted(party,other.getUUID())) {
                    event.setCanceled(true);
                    break;
                }
            }
        }
        if(target.hasUUID(EventSession.SESSION) && attacker instanceof ServerPlayer player) {
            var row=SmpData.get(player.server).find("events",target.getUUID(EventSession.SESSION));
            if(row==null || !new EventSession(row).accepted(player.getUUID()) || !EventRegions.contains(row,player.serverLevel(),player.blockPosition())) {
                event.setCanceled(true);
                return;
            }
            if(victim instanceof Mob mob && target.hasUUID(BloodMoon.EVENT_ID)) mob.setTarget(player);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void statistics(LivingDamageEvent event) {
        if(event.getEntity().level().isClientSide || event.getAmount()<=0) return;
        var owner=EventSession.owner(event.getSource().getEntity());
        if(owner==null) owner=EventSession.owner(event.getSource().getDirectEntity());
        if(!(owner instanceof ServerPlayer player) || !event.getEntity().getPersistentData().hasUUID(BloodMoon.EVENT_ID)) return;
        var row=SmpData.get(player.server).find("events",event.getEntity().getPersistentData().getUUID(BloodMoon.EVENT_ID));
        if(row==null || !row.getString("state").equals("ACTIVE") || !new EventSession(row).accepted(player.getUUID())) return;
        var member=SmpRecords.members(row).getCompound(player.getStringUUID());
        member.putBoolean("contributed",true);
        member.putDouble("damage",member.getDouble("damage")+Math.min(event.getAmount(),event.getEntity().getHealth()));
        SmpData.get(player.server).changed(row);
    }
    @SubscribeEvent public static void removed(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && event.getEntity() instanceof LivingEntity living)
            BloodMoon.removed(level.getServer(), living);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void drops(LivingDropsEvent event) {
        if(!event.getEntity().getPersistentData().hasUUID(BloodMoon.EVENT_ID)) return;
        UUID eventId=event.getEntity().getPersistentData().getUUID(BloodMoon.EVENT_ID);
        var row=SmpData.get(event.getEntity().getServer()).find("events",eventId);
        if(row==null) return;
        UUID attemptId=BloodMoonLoot.attempt(row);
        for(var drop:event.getDrops()) {
            BloodMoonLoot.mark(drop.getItem(),attemptId);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void experience(LivingExperienceDropEvent event) {
        var boss=event.getEntity();
        if(event.getDroppedExperience()<=0 || !boss.getPersistentData().hasUUID(BloodMoon.EVENT_ID)
                || !EventMobModifiers.isSetPieceBoss(boss) || boss.getServer()==null) return;
        var row=SmpData.get(boss.getServer()).find("events",boss.getPersistentData().getUUID(BloodMoon.EVENT_ID));
        if(row==null) return;
        int experience=event.getDroppedExperience();
        new EventSession(row).active(boss.getServer()).forEach(player ->
                com.siirio.jemworldbosstiers.api.ExperienceRewardApi.give(player,experience));
        event.setDroppedExperience(0);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { UUID id=e.getEntity().getUUID();INSIDE.remove(id);PROMPTED.remove(id);SENT.remove(id); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { INSIDE.clear();PROMPTED.clear();SENT.clear(); }
    private EventAreaHooks() {}
}
