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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class EventAreaHooks {
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
                        .append(Component.literal("[Открыть событие]").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/smp event-open " + id)))));
            if(type.equals("BLOOD_MOON") && member && player.isAlive() && !player.isSpectator()) {
                var entry=SmpRecords.members(row).getCompound(player.getStringUUID());
                if(safelyWithin) {
                    if(entry.getBoolean("awaitingReturn")) { entry.remove("awaitingReturn");SmpData.get(player.server).changed(row); }
                    entry.putDouble("safeX",player.getX());entry.putDouble("safeY",player.getY());entry.putDouble("safeZ",player.getZ());
                }
            }
            if((!type.equals("BOSS_RAID")||row.hasUUID("targetBoss")) && zones.size()<32 && row.getString("dimension").equals(player.level().dimension().location().toString())) {
                double distance=Math.max(Math.max(EventRegions.minX(row)-player.getX(),player.getX()-EventRegions.maxX(row)),Math.max(EventRegions.minZ(row)-player.getZ(),player.getZ()-EventRegions.maxZ(row)));
                if(distance<=EventRules.BOUNDARY_DISTANCE.get()) {
                    var encounter=new EncounterContext(row);
                    boolean completed=encounter.state()==EncounterContext.State.COMPLETED;
                    boolean active=type.equals("BLOOD_MOON")&&!completed;
                    boolean locked=type.equals("BLOOD_MOON")&&row.getBoolean("combatStarted")
                            &&new EventSession(row).accepted(player.getUUID())&&safelyWithin;
                    boolean passable=type.equals("BLOOD_MOON")&&!locked;
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

    public static void releaseBloodMoon(MinecraftServer server,UUID eventId,boolean visible) {
        for(ServerPlayer player:server.getPlayerList().getPlayers()) {
            List<EventNetwork.Boundary> current=SENT.get(player.getUUID());
            if(current==null) continue;
            var updated=new ArrayList<EventNetwork.Boundary>();
            for(EventNetwork.Boundary boundary:current) {
                if(!boundary.id().equals(eventId)) updated.add(boundary);
                else if(visible) updated.add(new EventNetwork.Boundary(boundary.id(),boundary.dimension(),boundary.type(),boundary.minX(),boundary.minY(),boundary.minZ(),boundary.maxX(),boundary.maxY(),boundary.maxZ(),boundary.color(),boundary.active(),true));
            }
            if(updated.equals(current)) continue;
            EventNetwork.zones(player,updated);
            SENT.put(player.getUUID(),List.copyOf(updated));
        }
    }
    private EventAreaHooks() {}
}
