package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="jem_server")
public final class EventAreaHooks {
    static final double PROJECTILE_RESTITUTION_REDUCTION=.2;
    private static final Map<UUID,Set<UUID>> INSIDE=new HashMap<>();
    private static final Map<UUID,Set<UUID>> PROMPTED=new HashMap<>();
    private static final Map<UUID,List<EventBoundary>> SENT=new HashMap<>();
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer player)) return;
        ReviveSupport.enforce(player);
        if(player.tickCount%10!=0) return;
        var inside=new HashSet<UUID>();
        var before=INSIDE.getOrDefault(player.getUUID(),Set.of());
        var prompted=new HashSet<UUID>();
        var promptedBefore=PROMPTED.getOrDefault(player.getUUID(),Set.of());
        var zones=new ArrayList<EventBoundary>();
        for(var row:EventScheduler.active(player.server)) {
            if(!row.contains("radius")) continue;
            String type=row.getString("activity");UUID id=row.getUUID("id");
            boolean within=EventRegions.contains(row,player.serverLevel(),player.blockPosition());
            boolean safelyWithin=EventRegions.contains(row,player.serverLevel(),player.getBoundingBox())
                    &&player.serverLevel().noCollision(player,player.getBoundingBox());
            boolean near=EventRegions.near(row,player.serverLevel(),player.blockPosition());
            if(within) inside.add(id);
            if(near) prompted.add(id);
            if(type.equals("RESOURCE_RUSH") && within!=before.contains(id)) player.sendSystemMessage(Component.translatable(within?"jem.event.rush_enter":"jem.event.rush_leave"));
            boolean member=new EncounterContext(row).isParticipant(player.getUUID());
            if(!row.getBoolean("combatStarted") && near && !promptedBefore.contains(id) && !member && (type.equals("BLOOD_MOON") || type.equals("BOSS_RAID")))
                player.sendSystemMessage(Component.literal("▣ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("jem.smp."+type)).append(" ")
                        .append(Component.translatable("jem.smp.open_event").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/smp event-open " + id)))));
            if(type.equals("BLOOD_MOON") && member && player.isAlive() && !player.isSpectator()) {
                var entry=SmpRecords.members(row).getCompound(player.getStringUUID());
                if(safelyWithin) {
                    if(entry.getBoolean("awaitingReturn")) { entry.remove("awaitingReturn");SmpData.get(player.server).changed(row); }
                    entry.putDouble("safeX",player.getX());entry.putDouble("safeY",player.getY());entry.putDouble("safeZ",player.getZ());
                }
            }
            if(!type.equals("BOSS_RAID") && zones.size()<32 && row.getString("dimension").equals(player.level().dimension().location().toString())) {
                double distance=Math.max(Math.max(EventRegions.minX(row)-player.getX(),player.getX()-EventRegions.maxX(row)),Math.max(EventRegions.minZ(row)-player.getZ(),player.getZ()-EventRegions.maxZ(row)));
                if(distance<=EventRules.BOUNDARY_DISTANCE.get()) {
                    var encounter=new EncounterContext(row);
                    boolean completed=encounter.state()==EncounterContext.State.COMPLETED;
                    boolean active=type.equals("BLOOD_MOON")&&!completed;
                    boolean locked=type.equals("BLOOD_MOON")&&BloodMoonSolidBoundary.locked(player,row);
                    boolean passable=type.equals("BLOOD_MOON")&&!locked;
                    zones.add(new EventBoundary(id,row.getString("dimension"),type,EventRegions.minX(row),player.serverLevel().getMinBuildHeight(),EventRegions.minZ(row),EventRegions.maxX(row),player.serverLevel().getMaxBuildHeight()-1,EventRegions.maxZ(row),type.equals("BLOOD_MOON")?EventRules.BLOOD_COLOR.get():type.equals("RESOURCE_RUSH")?EventRules.RUSH_COLOR.get():EventRules.RAID_COLOR.get(),active,passable));
                }
            }
        }
        if(!zones.equals(SENT.get(player.getUUID()))) { EventNetwork.zones(player,zones);SENT.put(player.getUUID(),List.copyOf(zones)); }
        INSIDE.put(player.getUUID(),inside);
        PROMPTED.put(player.getUUID(),prompted);
    }

    public static void prompt(ServerPlayer player, UUID id) {
        CompoundTag row = SmpData.get(player.server).find("events", id);
        SmpRecords.require(EventRuntime.available(player.server, row), "unavailable");
        SmpRecords.require(row != null && row.getString("state").equals("ACTIVE") && !row.getBoolean("combatStarted"), "unavailable");
        String type = row.getString("activity");
        SmpRecords.require((type.equals("BLOOD_MOON") || type.equals("BOSS_RAID")) && EventRegions.near(row, player.serverLevel(), player.blockPosition()), "travel_to_arena");
        EventNetwork.prompt(player, id, EventEntryKind.valueOf(type), Component.translatable("jem.smp." + type).getString(), "");
    }

    @SubscribeEvent
    public static void combatMobBoundary(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for(String type:List.of("BLOOD_MOON")) {
            var row=EventScheduler.active(level.getServer(),type);
            if(row==null || !row.getBoolean("combatStarted") || !EventRegions.contains(row,level,mob.blockPosition())) continue;
            boolean belongs=type.equals("BLOOD_MOON")?BloodMoon.belongs(row,mob):RaidEvent.belongs(row,mob);
            if(!belongs && !EventSession.admitted(row,mob)) EventRegions.eject(mob,row);
            else if(mob.getNavigation().getTargetPos()!=null && !SolidBoundary.permitsTeleport(mob,level,Vec3.atBottomCenterOf(mob.getNavigation().getTargetPos())))
                mob.getNavigation().stop();
            return;
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void teleport(EntityTeleportEvent event) {
        if(!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        BossTeleportSafety.Decision decision=BossTeleportSafety.validate(event.getEntity(),level,event.getTarget());
        if(!decision.matched()||event.getTarget().equals(decision.destination())) return;
        if(decision.destination()==null) event.setCanceled(true);
        else {
            event.setTargetX(decision.destination().x);
            event.setTargetY(decision.destination().y);
            event.setTargetZ(decision.destination().z);
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void target(LivingChangeTargetEvent event) {
        var mob=event.getEntity();var tag=mob.getPersistentData();
        if(!tag.hasUUID(EventSession.SESSION) || mob.level().isClientSide) return;
        var target=event.getNewTarget();
        if(target==null) return;
        var row=SmpData.get(mob.getServer()).find("events",tag.getUUID(EventSession.SESSION));
        if (row != null ? !EventRuntime.available(mob.getServer(), row) : !EventRuntime.enabled(mob.getServer())) return;
        if(row!=null&&row.getString("activity").equals("BLOOD_MOON")&&!BloodMoon.canTarget(mob,target)) {
            event.setCanceled(true);
            return;
        }
        if(row==null || !EventSession.canInteract(mob,target)) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingAttackEvent event) {
        if(event.getEntity().level().isClientSide) return;
        Entity sourceEntity=event.getSource().getEntity();
        if(sourceEntity==null) sourceEntity=event.getSource().getDirectEntity();
        var attacker=EventSession.owner(sourceEntity);
        var victim=event.getEntity();
        if(attacker==null) return;
        LivingEntity actor=sourceEntity instanceof LivingEntity living ? living
                : attacker instanceof LivingEntity living ? living : null;
        if (unavailableEventEntity(attacker) || unavailableEventEntity(victim)) return;
        if(attacker instanceof Mob && victim instanceof Mob
                && attacker.getPersistentData().hasUUID(BloodMoon.EVENT_ID)
                && victim.getPersistentData().hasUUID(BloodMoon.EVENT_ID)
                && attacker.getPersistentData().getUUID(BloodMoon.EVENT_ID).equals(victim.getPersistentData().getUUID(BloodMoon.EVENT_ID))) {
            event.setCanceled(true);
            return;
        }
        if (actor != null && !EventSession.canInteract(actor,victim)) {
            event.setCanceled(true);
            return;
        }
        var target=victim.getPersistentData();
        if(EventSession.combatForEntity(victim)!=null && attacker instanceof ServerPlayer sourcePlayer
                && EventSession.owner(victim) instanceof ServerPlayer targetPlayer && sourcePlayer!=targetPlayer) {
            event.setCanceled(true);
            return;
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
        if(victim instanceof Mob mob && target.hasUUID(BloodMoon.EVENT_ID)
                && attacker instanceof ServerPlayer && actor!=null) mob.setTarget(actor);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void statistics(LivingDamageEvent event) {
        if (!EventRuntime.enabled(event.getEntity().getServer())) return;
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
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && EventRuntime.enabled(level.getServer()) && event.getEntity() instanceof LivingEntity living)
            BloodMoon.removed(level.getServer(), living);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void drops(LivingDropsEvent event) {
        if (!EventRuntime.enabled(event.getEntity().getServer())) return;
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
        if (!EventRuntime.enabled(event.getEntity().getServer())) return;
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
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { invalidate(event.getEntity().getUUID()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { invalidate(event.getEntity().getUUID()); }
    @SubscribeEvent public static void dimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) { invalidate(event.getEntity().getUUID()); }
    private static boolean unavailableEventEntity(Entity entity) {
        if (entity.getServer() == null) return false;
        var tag = entity.getPersistentData();
        String key = tag.hasUUID(EventSession.SESSION) ? EventSession.SESSION : RaidEvent.EVENT_ID;
        if (!tag.hasUUID(key)) return false;
        var row = SmpData.get(entity.getServer()).find("events", tag.getUUID(key));
        return row != null ? !EventRuntime.available(entity.getServer(), row) : !EventRuntime.enabled(entity.getServer());
    }

    private static void invalidate(UUID id) {
        INSIDE.remove(id);
        PROMPTED.remove(id);
        SENT.remove(id);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { INSIDE.clear();PROMPTED.clear();SENT.clear(); }

    public static void releaseBloodMoon(MinecraftServer server,UUID eventId,boolean visible) {
        for(ServerPlayer player:server.getPlayerList().getPlayers()) {
            List<EventBoundary> current=SENT.get(player.getUUID());
            if(current==null) continue;
            var updated=new ArrayList<EventBoundary>();
            for(EventBoundary boundary:current) {
                if(!boundary.id().equals(eventId)) updated.add(boundary);
                else if(visible) updated.add(new EventBoundary(boundary.id(),boundary.dimension(),boundary.type(),boundary.minX(),boundary.minY(),boundary.minZ(),boundary.maxX(),boundary.maxY(),boundary.maxZ(),boundary.color(),boundary.active(),true));
            }
            if(updated.equals(current)) continue;
            EventNetwork.zones(player,updated);
            SENT.put(player.getUUID(),List.copyOf(updated));
        }
    }
    private EventAreaHooks() {}
}
