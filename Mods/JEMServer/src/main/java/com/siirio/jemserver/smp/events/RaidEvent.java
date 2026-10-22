package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import com.siirio.jemworldbosstiers.api.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="jem_server")
public final class RaidEvent {
    public static final String EVENT_ID = "jem_smp_raid";
    private static final String FORGE_SPAWN_TYPE="forge:spawn_type";
    private static final String MOB_SUMMONED="MOB_SUMMONED";
    private static final String ORIGINAL_SCALE="jem:raid_original_scale";
    private static final String ORIGINAL_SCALE_PRESENT="jem:raid_original_scale_present";
    private static final String ALLIES="raidAllies";
    private static final int MAX_ALLIES=20;
    private static final int[] STAGE_COUNTS={7,7,6};

    public static void adoptSummon(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity entity) {
        if(!(entity instanceof Mob mob) || !(mob instanceof net.minecraft.world.entity.monster.Enemy)) return;
        var data=mob.getPersistentData();
        if(data.hasUUID(EVENT_ID) || !MOB_SUMMONED.equals(data.getString(FORGE_SPAWN_TYPE))) return;
        var event=EventScheduler.active(level.getServer(),"BOSS_RAID");
        if(event==null || !event.getBoolean("combatStarted") || !EventRegions.contains(event,level,mob.blockPosition())) return;
        if(event.getInt("raidAlliesSpawned")>=MAX_ALLIES) {mob.discard();return;}
        new EventSession(event).mark(mob);
        data.putUUID(EVENT_ID,event.getUUID("id"));
        if(event.hasUUID("bossEntity")) data.putUUID("JEMHostedBoss",event.getUUID("bossEntity"));
        ListTag allies=event.getList(ALLIES,Tag.TAG_STRING);
        allies.add(StringTag.valueOf(mob.getStringUUID()));
        event.put(ALLIES,allies);
        event.putInt("raidAlliesSpawned",event.getInt("raidAlliesSpawned")+1);
        SmpData.get(level.getServer()).changed(event);
    }

    public static boolean belongs(CompoundTag event,net.minecraft.world.entity.Entity entity) {
        var data=entity.getPersistentData();
        return data.hasUUID(EVENT_ID)&&data.getUUID(EVENT_ID).equals(event.getUUID("id"));
    }

    public static boolean prepare(MinecraftServer server, CompoundTag row) {
        if(!EventRuntime.available(server, row) || row.getBoolean("combatStarted")) return false;
        if(!row.hasUUID("targetBoss")&&!row.contains("raidArenaId")) {
            var arena=RaidArenaApi.availableArena(server,candidate->StructureStaging.arenaAvailable(server,candidate.id())).orElse(null);
            return arena==null||claimArena(server,row,arena.id(),arena.entityType(),
                    arena.dimension().toString(),arena.center(),arena.bounds());
        }
        ServerLevel level=EventRegions.level(server,row);
        Entity target=level==null||!row.hasUUID("targetBoss")?null:level.getEntity(row.getUUID("targetBoss"));
        LivingEntity boss=target instanceof LivingEntity living&&living.isAlive()?living:null;
        if(boss==null&&row.contains("raidArenaId")) {
            boss=RaidArenaApi.acquireBoss(server,row.getString("raidArenaId"),row.getUUID("id"),
                    new ResourceLocation(row.getString("bossType"))).orElse(null);
            // The native anchor/chunk may become available later; retain its reservation.
            if(boss==null) return true;
            // A profile may register several native entity types; preserve the acquired one.
            row.putString("bossType",BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString());
        }
        if(boss==null) return !row.hasUUID("targetBoss");
        if(!BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString().equals(row.getString("bossType"))
                ||!HostedEncounterApi.prepareRaid(boss)) return false;
        applyRaidScale(boss);
        boss.getPersistentData().putUUID(EVENT_ID,row.getUUID("id"));
        boolean changed=!row.hasUUID("targetBoss")||!row.getUUID("targetBoss").equals(boss.getUUID())
                ||row.getLong("position")!=boss.blockPosition().asLong();
        row.putUUID("targetBoss",boss.getUUID());
        row.putLong("position",boss.blockPosition().asLong());
        if(changed) SmpData.get(server).changed(row);
        return true;
    }

    public static boolean claimArena(MinecraftServer server, CompoundTag event, String arenaId,
                                     ResourceLocation bossType, String dimension, BlockPos center, BoundingBox bounds) {
        if (event.getBoolean("combatStarted")) return false;
        if(event.contains("raidArenaId")) return event.getString("raidArenaId").equals(arenaId)
                &&prepare(server,event);
        var arena = RaidArenaApi.reserve(server, arenaId, bossType, event.getUUID("id"),
                candidate->StructureStaging.arenaAvailable(server,candidate.id())).orElse(null);
        if (arena == null || !arena.dimension().toString().equals(dimension) || !RaidArenaApi.sameBounds(arena.bounds(), bounds)) return false;
        event.putString("raidArenaId", arena.id());
        event.putString("bossType", bossType.toString());
        StructureStaging.attachArena(event,UUID.fromString(arenaId),bossType,dimension,center,bounds);
        SmpData.get(server).changed(event);
        prepare(server,event);
        return true;
    }


    public static void start(ServerPlayer host, CompoundTag party, Collection<UUID> agreed) {
        SmpRecords.require(party.hasUUID("eventId") && !party.hasUUID("bossEntity"), "unavailable");
        var data = SmpData.get(host.server);
        var event = data.find("events", party.getUUID("eventId"));
        SmpRecords.require(event != null, "unavailable");
        requireActive(event);
        SmpRecords.require(
                event.hasUUID("party")
                        && event.getUUID("party").equals(party.getUUID("id"))
                        && !event.hasUUID("bossEntity"),
                "combat_locked");
        SmpRecords.require(
                EventRegions.near(event, host.serverLevel(), host.blockPosition()),
                "travel_to_arena");
        var level = EventRegions.level(host.server, event);
        SmpRecords.require(level != null, "unavailable");
        var pos = BlockPos.of(event.getLong("position"));
        SmpRecords.require(level.hasChunkAt(pos), "unavailable");
        for (UUID participant : agreed) if (event.contains("raidArenaId"))
            SmpRecords.require(!RaidArenaApi.participated(host.server, event.getString("raidArenaId"), participant), "raid_already_participated");
        SmpRecords.require(agreed.contains(host.getUUID()), "host_must_be_present");
        SmpRecords.require(prepare(host.server,event)&&event.hasUUID("targetBoss"),"unavailable");
        var entity=level.getEntity(event.getUUID("targetBoss"));
        SmpRecords.require(entity instanceof LivingEntity&&entity.isAlive(),"unavailable");
        LivingEntity boss=(LivingEntity)entity;
        boolean started = false;
        try {
            SmpRecords.require(HostedEncounterApi.prepareRaid(boss),"unavailable");
            pos=boss.blockPosition();
            event.putLong("position", pos.asLong());
            boss.getPersistentData().putBoolean("jem_solo", party.getBoolean("solo"));
            applyRaidScale(boss);
            boss.getPersistentData().putUUID(EVENT_ID, event.getUUID("id"));
            BoundingBox bounds=StructureStaging.prepareCombat(level,event,boss);
            SmpRecords.require(HostedEncounterApi.start(boss, agreed, true, bounds), "unavailable");
            party.putUUID("bossEntity", boss.getUUID());
            event.putUUID("bossEntity", boss.getUUID());
            event.putBoolean("solo", party.getBoolean("solo"));
            event.putBoolean("combatStarted", true);
            initializeReinforcements(event,boss);
            LivingEntity startedBoss=boss;
            level.players().forEach(player->EventNetwork.eventMob(player,startedBoss));
            data.changed(party);
            data.changed(event);
            if (event.contains("raidArenaId")) RaidArenaApi.recordParticipation(host.server, event.getString("raidArenaId"), agreed);
            started = true;
        } finally {
            if (!started) {
                HostedEncounterApi.cancel(boss);
                HostedEncounterApi.prepareRaid(boss);
                HostedEncounterApi.hold(boss);
            }
        }
    }

    private static void requireActive(CompoundTag event) {
        SmpRecords.require(
                event.getString("state").equals("ACTIVE")
                        && event.getString("activity").equals("BOSS_RAID")
                        && event.getLong("ends") > System.currentTimeMillis(),
                "unavailable");
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent tick) {
        if(tick.phase!=TickEvent.Phase.END||!com.siirio.jemserver.smp.SmpEnvironment.active(tick.getServer())||tick.getServer().getTickCount()%10!=0) return;
        for(CompoundTag event:SmpData.get(tick.getServer()).all("events")) {
            if (!EventRuntime.available(tick.getServer(), event)) continue;
            if(!event.getString("state").equals("ACTIVE")||!event.getString("activity").equals("BOSS_RAID")) continue;
            if(!event.getBoolean("combatStarted")) {prepare(tick.getServer(),event);continue;}
            ServerLevel level=EventRegions.level(tick.getServer(),event);
            if(level==null) continue;
            List<LivingEntity> bosses=livingBosses(level,event);
            if(bosses.isEmpty()) continue;
            CompoundTag initial=event.getCompound("raidBossInitialHealth");
            double initialHealth=initial.getAllKeys().stream().mapToDouble(initial::getDouble).sum();
            if(initialHealth<=0) continue;
            double ratio=bosses.stream().mapToDouble(LivingEntity::getHealth).sum()/initialHealth;
            int stage=event.getInt("raidReinforcementStage");
            while(stage<3&&(stage==0||stage==1&&ratio<=.5||stage==2&&ratio<=.25)) {
                spawnReinforcements(level,event,bosses.get(0),STAGE_COUNTS[stage]);
                stage++;
                event.putInt("raidReinforcementStage",stage);
                SmpData.get(level.getServer()).changed(event);
            }
        }
    }

    private static void initializeReinforcements(CompoundTag event,LivingEntity boss) {
        event.putInt("raidReinforcementStage",0);
        event.putInt("raidAlliesSpawned",0);
        event.put(ALLIES,new ListTag());
        CompoundTag initial=new CompoundTag();
        initial.putDouble(boss.getStringUUID(),boss.getMaxHealth());
        event.put("raidBossInitialHealth",initial);
    }

    private static List<LivingEntity> livingBosses(ServerLevel level,CompoundTag event) {
        List<LivingEntity> bosses=new ArrayList<>();
        if(event.hasUUID("bossEntity")) {
            Entity entity=level.getEntity(event.getUUID("bossEntity"));
            if(entity instanceof LivingEntity living&&living.isAlive()) bosses.add(living);
        }
        for(Tag value:event.getList("raidBosses",Tag.TAG_STRING)) {
            Entity entity=level.getEntity(UUID.fromString(value.getAsString()));
            if(entity instanceof LivingEntity living&&living.isAlive()&&!bosses.contains(living)) bosses.add(living);
        }
        return bosses;
    }

    private static void spawnReinforcements(ServerLevel level,CompoundTag event,LivingEntity boss,int requested) {
        List<EntityType<?>> pool=reinforcementPool(boss.getType());
        int remaining=Math.min(requested,MAX_ALLIES-event.getInt("raidAlliesSpawned"));
        if(pool.isEmpty()||remaining<=0) return;
        List<ServerPlayer> targets=new EventSession(event).active(level.getServer());
        for(int index=0;index<remaining;index++) {
            EntityType<?> type=pool.get(level.random.nextInt(pool.size()));
            Entity entity=type.create(level);
            if(!(entity instanceof Mob mob)) continue;
            double angle=Math.PI*2*index/Math.max(1,remaining);
            int distance=8+level.random.nextInt(7);
            int x=(int)Math.round(boss.getX()+Math.cos(angle)*distance);
            int z=(int)Math.round(boss.getZ()+Math.sin(angle)*distance);
            BlockPos position=EventRegions.safeInside(level,event,new BlockPos(x,boss.blockPosition().getY(),z)).orElse(null);
            if(position==null) continue;
            mob.moveTo(position.getX()+.5,position.getY(),position.getZ()+.5,0,0);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(position),MobSpawnType.EVENT,null,null);
            new EventSession(event).mark(mob);
            mob.getPersistentData().putUUID(EVENT_ID,event.getUUID("id"));
            mob.getPersistentData().putUUID("JEMHostedBoss",boss.getUUID());
            EventMobModifiers.applyRaidMob(mob,event.getInt("worldTier"),Math.max(1,targets.size()));
            if(!level.noCollision(mob)||!level.addFreshEntity(mob)) continue;
            if(!targets.isEmpty()) mob.setTarget(targets.get(level.random.nextInt(targets.size())));
            ListTag allies=event.getList(ALLIES,Tag.TAG_STRING);
            allies.add(StringTag.valueOf(mob.getStringUUID()));
            event.put(ALLIES,allies);
            event.putInt("raidAlliesSpawned",event.getInt("raidAlliesSpawned")+1);
        }
    }

    private static List<EntityType<?>> reinforcementPool(EntityType<?> bossType) {
        ResourceLocation id=BuiltInRegistries.ENTITY_TYPE.getKey(bossType);
        String path=id.equals(new ResourceLocation("minecraft","wither"))?"wither":id.equals(new ResourceLocation("luminous_beasts","mummy"))?"mummy":id.equals(new ResourceLocation("luminous_beasts","yeti"))?"yeti":"";
        if(path.isEmpty()) return List.of();
        TagKey<EntityType<?>> tag=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("jem_server","raid_allies/"+path));
        return BuiltInRegistries.ENTITY_TYPE.getTag(tag).stream().flatMap(set->set.stream()).map(Holder::value).toList();
    }

    private static void applyRaidScale(LivingEntity boss) {
        CompoundTag data=boss.getPersistentData();
        if(data.contains(ORIGINAL_SCALE_PRESENT)) return;
        float original=data.getFloat(EventMobScale.KEY);
        data.putBoolean(ORIGINAL_SCALE_PRESENT,data.contains(EventMobScale.KEY));
        data.putFloat(ORIGINAL_SCALE,original);
        float current=Math.max(1,original);
        float baseHeight=boss.getBbHeight()/current;
        EventMobScale.apply(boss,Math.min(current*1.35F,20/Math.max(.01F,baseHeight)));
    }

    private static void restoreRaidScale(LivingEntity boss) {
        CompoundTag data=boss.getPersistentData();
        if(!data.contains(ORIGINAL_SCALE_PRESENT)) return;
        if(data.getBoolean(ORIGINAL_SCALE_PRESENT)) EventMobScale.apply(boss,data.getFloat(ORIGINAL_SCALE));
        else EventMobScale.clear(boss);
        data.remove(ORIGINAL_SCALE);
        data.remove(ORIGINAL_SCALE_PRESENT);
    }

    private static void discardAllies(MinecraftServer server,CompoundTag event) {
        ServerLevel level=EventRegions.level(server,event);
        if(level!=null) for(Tag value:event.getList(ALLIES,Tag.TAG_STRING)) {
            Entity entity=level.getEntity(UUID.fromString(value.getAsString()));
            if(entity!=null) entity.discard();
        }
        event.remove(ALLIES);
    }

    public static void cleanup(MinecraftServer server, CompoundTag event) {
        discardAllies(server,event);
        UUID bossId=event.hasUUID("bossEntity")?event.getUUID("bossEntity"):event.hasUUID("targetBoss")?event.getUUID("targetBoss"):null;
        if(bossId!=null&&event.contains("raidArenaId")) {
            var level = EventRegions.level(server, event);
            if (level != null && level.getEntity(bossId) instanceof LivingEntity living) {
                restoreRaidScale(living);
                living.getPersistentData().remove(EVENT_ID);
                level.players().forEach(player -> EventNetwork.eventMob(player, living));
            }
            RaidArenaApi.completeRaid(server,event.getString("raidArenaId"),bossId,new ResourceLocation(event.getString("bossType")));
        } else if(bossId!=null) {
            var level=EventRegions.level(server,event);
            var boss=level==null?null:level.getEntity(bossId);
            if(boss instanceof LivingEntity living&&living.isAlive()) {
                if(event.hasUUID("targetBoss")) {
                    restoreRaidScale(living);
                    level.players().forEach(player->EventNetwork.eventMob(player,living));
                    HostedEncounterApi.cancel(living);
                    HostedEncounterApi.release(living);
                    if(HostedEncounterApi.preservesRaidBoss(living)) HostedEncounterApi.restorePreparedRaid(living);
                    living.getPersistentData().remove(EVENT_ID);
                    HostedEncounterApi.hold(living);
                } else living.discard();
            }
        } else if (event.contains("raidArenaId")) {
            RaidArenaApi.release(server, event.getString("raidArenaId"), event.getUUID("id"));
        }
    }

    public static void resetAttempt(MinecraftServer server, CompoundTag event) {
        discardAllies(server,event);
        if (!event.hasUUID("bossEntity")) {
            return;
        }
        UUID bossId = event.getUUID("bossEntity");
        if (event.contains("raidArenaId")) {
            RaidArenaApi.completeRaid(server,event.getString("raidArenaId"),bossId,new ResourceLocation(event.getString("bossType")));
            event.remove("targetBoss");
            RaidArenaApi.reserveAgain(server,event.getString("raidArenaId"),bossId,event.getUUID("id"));
            return;
        }
        var level = EventRegions.level(server, event);
        var boss = level == null ? null : level.getEntity(bossId);
        if(event.hasUUID("targetBoss")) {
            if(boss instanceof LivingEntity living&&living.isAlive()) {HostedEncounterApi.prepareRaid(living);applyRaidScale(living);HostedEncounterApi.hold(living);}
            return;
        }
        if (boss != null) {
            boss.discard();
        }
        if (event.contains("raidArenaId")) {
            RaidArenaApi.reserveAgain(server, event.getString("raidArenaId"), bossId, event.getUUID("id"));
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void loaded(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity boss)
                || !boss.getPersistentData().hasUUID(EVENT_ID) || WorldTierApi.profile(boss).isEmpty()) return;
        var row = SmpData.get(level.getServer()).find("events", boss.getPersistentData().getUUID(EVENT_ID));
        if (EventRuntime.available(level.getServer(), row) && row.getString("state").equals("ACTIVE") && row.getLong("ends") > System.currentTimeMillis()) return;
        if(HostedEncounterApi.isRaid(boss)&&!HostedEncounterApi.wasNativeBeforeRaid(boss)) {
            String arenaId=WorldTierApi.encounter(boss).map(encounter->encounter.arenaId()).orElse(null);
            if(arenaId!=null) RaidArenaApi.queueRaidCompletion(level.getServer(),arenaId,boss.getUUID(),BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()));
            HostedEncounterApi.hold(boss);
            return;
        }
        restoreRaidScale(boss);
        HostedEncounterApi.cancel(boss);
        if (HostedEncounterApi.preservesRaidBoss(boss)) HostedEncounterApi.restorePreparedRaid(boss);
        boss.getPersistentData().remove(EVENT_ID);
        HostedEncounterApi.hold(boss);
        WorldTierApi.encounter(boss).filter(encounter->encounter.arenaId()!=null).ifPresent(encounter->{
            var data=com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
            if(data.recordLiveBoss(encounter.arenaId(),boss)) data.clearRaidReplacement(encounter.arenaId());
        });
    }

    private RaidEvent() {}
}
