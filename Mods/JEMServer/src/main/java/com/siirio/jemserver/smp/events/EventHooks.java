package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.*;

public final class EventHooks {
    private static final EventHooks INSTANCE = new EventHooks();
    private static final BloodMoonSpawnGuard BLOOD_MOON_SPAWN_GUARD = new BloodMoonSpawnGuard();
    private static boolean spawnGuardRegistered;
    private static boolean registered;

    public static void register() {
        if (registered) return;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(INSTANCE);
        registered = true;
    }

    public static void unregister() {
        unregisterSpawnGuard();
        if (!registered) return;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(INSTANCE);
        registered = false;
    }

    public static void registerSpawnGuard() {
        if(spawnGuardRegistered) return;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(BLOOD_MOON_SPAWN_GUARD);
        spawnGuardRegistered=true;
    }

    public static void unregisterSpawnGuard() {
        if(!spawnGuardRegistered) return;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(BLOOD_MOON_SPAWN_GUARD);
        spawnGuardRegistered=false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void resourceRushAnimalDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.animal.Animal animal)
                || !(animal.level() instanceof ServerLevel level)) return;
        var row=EventScheduler.active(level.getServer(),"RESOURCE_RUSH");
        if(row==null || !EventRegions.contains(row,level,animal.blockPosition())) return;
        var copies=new java.util.ArrayList<ItemEntity>();
        for(var drop:event.getDrops()) {
            if(drop.getItem().isEmpty()) continue;
            copies.add(new ItemEntity(level,drop.getX(),drop.getY(),drop.getZ(),drop.getItem().copy()));
        }
        event.getDrops().addAll(copies);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void placement(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BloodMoonSetPieces.placed(level,event);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent placements) placements.getReplacedBlockSnapshots().forEach(snapshot->ResourcePlacements.record(level,snapshot.getPos()));
        else ResourcePlacements.record(level,event.getPos());
        var row = EventScheduler.active(level.getServer(), "RESOURCE_RUSH");
        if (row == null) return;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiple)
            multiple.getReplacedBlockSnapshots()
                    .forEach(snapshot -> track(level, row, snapshot.getPos()));
        else track(level, row, event.getPos());
    }

    public static void machinePlacement(
            ServerLevel level,
            net.minecraft.core.BlockPos source,
            net.minecraft.core.BlockPos target) {
        ResourcePlacements.record(level,target);
        var event = EventScheduler.active(level.getServer(), "RESOURCE_RUSH");
        if (event != null) track(level, event, target);
    }

    private static void track(ServerLevel level, CompoundTag row, net.minecraft.core.BlockPos pos) {
        if (!EventRegions.contains(row, level, pos)) return;
        var placed = row.getCompound("placements");
        if (placed.size() >= SmpConfig.RUSH_TRACKED_BLOCKS.get()) {
            row.putBoolean("rushTrackingFull", true);
            SmpData.get(level.getServer()).changed(row);
            return;
        }
        placed.putBoolean(Long.toString(pos.asLong()), true);
        row.put("placements", placed);
        SmpData.get(level.getServer()).changed(row);
    }

    @SubscribeEvent
    public void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var row = EventScheduler.active(level.getServer(), "RESOURCE_RUSH");
        if (row == null) return;
        for (int reach = 0; reach <= 13; reach++)
            if (EventRegions.contains(
                    row, level, event.getPos().relative(event.getDirection(), reach))) {
                event.setCanceled(true);
                return;
            }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void entity(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if(!BloodMoon.adoptSummon(level,event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        RaidEvent.adoptSummon(level,event.getEntity());
        var tag = event.getEntity().getPersistentData();
        String key = tag.hasUUID(BloodMoon.EVENT_ID) ? BloodMoon.EVENT_ID : RaidEvent.EVENT_ID;
        if (!tag.hasUUID(key)) return;
        var row = SmpData.get(level.getServer()).find("events", tag.getUUID(key));
        if (row == null || !row.getString("state").equals("ACTIVE")
                || key.equals(BloodMoon.EVENT_ID) && !activeBloodMoonSpawn(level,event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        event.setCanceled(false);
        if(key.equals(BloodMoon.EVENT_ID)) level.getServer().execute(()->{
            if(event.getEntity().isAlive()&&!BloodMoon.tracked(row,event.getEntity())) event.getEntity().discard();
        });
    }

    static boolean activeBloodMoonSpawn(ServerLevel level,net.minecraft.world.entity.Entity entity) {
        var tag=entity.getPersistentData();
        if(!tag.hasUUID(BloodMoon.EVENT_ID)) return false;
        var row=SmpData.get(level.getServer()).find("events",tag.getUUID(BloodMoon.EVENT_ID));
        return row!=null && row.getString("state").equals("ACTIVE") && BloodMoonLoot.activeAttempt(row,entity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void defeated(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level)
            BloodMoon.defeated(level.getServer(), event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void contribution(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        net.minecraft.world.entity.Entity mob = event.getEntity();
        ServerPlayer player = null;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) player = attacker;
        else if (event.getEntity() instanceof ServerPlayer defender) {
            player = defender;
            mob = event.getSource().getEntity();
        }
        if (player == null
                || mob == null
                || !mob.getPersistentData().hasUUID(BloodMoon.EVENT_ID)
                || event.getAmount() <= 0) return;
        var row =
                SmpData.get(player.server)
                        .find("events", mob.getPersistentData().getUUID(BloodMoon.EVENT_ID));
        if (row == null
                || !row.getString("state").equals("ACTIVE")
                || !EventRegions.contains(row, player.serverLevel(), player.blockPosition()))
            return;
        var member = SmpRecords.members(row).getCompound(player.getStringUUID());
        member.putBoolean("contributed", true);
        SmpRecords.members(row).put(player.getStringUUID(), member);
        SmpData.get(player.server).changed(row);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void localCombat(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        var source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player
                && event.getEntity().getPersistentData().hasUUID(BloodMoon.EVENT_ID)) {
            var targetEvent =
                    SmpData.get(player.server)
                            .find(
                                    "events",
                                    event.getEntity()
                                            .getPersistentData()
                                            .getUUID(BloodMoon.EVENT_ID));
            if (targetEvent == null
                    || !targetEvent.getString("state").equals("ACTIVE")
                    || !EventRegions.contains(
                            targetEvent, player.serverLevel(), player.blockPosition()))
                event.setCanceled(true);
        }
        if (source == null
                || !source.getPersistentData().hasUUID(BloodMoon.EVENT_ID)
                || !(event.getEntity().level() instanceof ServerLevel level)) return;
        var row =
                SmpData.get(level.getServer())
                        .find("events", source.getPersistentData().getUUID(BloodMoon.EVENT_ID));
        if (row == null
                || !row.getString("state").equals("ACTIVE")
                || !EventRegions.contains(row, level, event.getEntity().blockPosition()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void explosion(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        net.minecraft.world.entity.Entity source = event.getExplosion().getIndirectSourceEntity();
        if (source == null) source = event.getExplosion().getDirectSourceEntity();
        if (source != null && source.getPersistentData().hasUUID(BloodMoon.EVENT_ID))
            event.getAffectedBlocks().clear();
    }
}
