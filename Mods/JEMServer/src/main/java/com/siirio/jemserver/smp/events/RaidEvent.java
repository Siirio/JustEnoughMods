package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import com.siirio.jemworldbosstiers.api.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
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
import java.util.Map;
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
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { adoptSummonInScope(level, entity); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> adoptSummonInScope(level, entity));
    }

    private static void adoptSummonInScope(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity entity) {
        if(!(entity instanceof Mob mob) || !(mob instanceof net.minecraft.world.entity.monster.Enemy)) return;
        var data=mob.getPersistentData();
        if(data.hasUUID(EVENT_ID) || !MOB_SUMMONED.equals(data.getString(FORGE_SPAWN_TYPE))) return;
        var event=EventScheduler.active(level.getServer(),"BOSS_RAID");
        if(event==null || !event.combatStarted() || !EventRegions.contains(event,level,mob.blockPosition())) return;
        if(event.raidAlliesSpawned()>=MAX_ALLIES) {mob.discard();return;}
        new EventSession(event).mark(mob);
        data.putUUID(EVENT_ID,event.id());
        if(event.hasBossEntity()) data.putUUID("JEMHostedBoss",event.bossEntity());
        var allies = new ArrayList<>(event.raidAllies());
        allies.add(mob.getUUID());
        event.raidAllies(allies);
        event.raidAlliesSpawned(event.raidAlliesSpawned()+1);

    }

    public static boolean belongs(EventState event,net.minecraft.world.entity.Entity entity) {
        var data=entity.getPersistentData();
        return data.hasUUID(EVENT_ID)&&data.getUUID(EVENT_ID).equals(event.id());
    }

    public static boolean prepare(MinecraftServer server, EventState row) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return prepareInScope(server, row); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> prepareInScope(server, store.events().bind(row)));
    }

    private static boolean prepareInScope(MinecraftServer server, EventState row) {
        if(!EventRuntime.available(server, row) || row.combatStarted()) return false;
        if(!row.hasTargetBoss()&&!row.hasRaidArenaId()) {
            var arena=RaidArenaApi.availableArena(server,candidate->StructureStaging.arenaAvailable(server,candidate.id())).orElse(null);
            return arena==null||claimArena(server,row,arena.id(),arena.entityType(),
                    arena.dimension().toString(),arena.center(),arena.bounds());
        }
        ServerLevel level=EventRegions.level(server,row);
        Entity target=level==null||!row.hasTargetBoss()?null:level.getEntity(row.targetBoss());
        LivingEntity boss=target instanceof LivingEntity living&&living.isAlive()?living:null;
        if(boss==null&&row.hasRaidArenaId()) {
            boss=RaidArenaApi.acquireBoss(server,row.raidArenaId(),row.id(),
                    new ResourceLocation(row.bossType())).orElse(null);
            // The native anchor/chunk may become available later; retain its reservation.
            if(boss==null) return true;
            // A profile may register several native entity types; preserve the acquired one.
            row.bossType(BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString());
        }
        if(boss==null) return !row.hasTargetBoss();
        if(!BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString().equals(row.bossType())
                ||!HostedEncounterApi.prepareRaid(boss)) return false;
        boss.getPersistentData().putUUID(EVENT_ID,row.id());
        boolean changed=!row.hasTargetBoss()||!row.targetBoss().equals(boss.getUUID())
                ||row.position()!=boss.blockPosition().asLong();
        row.targetBoss(boss.getUUID());
        row.position(boss.blockPosition().asLong());
        
        return true;
    }

    public static boolean claimArena(MinecraftServer server, EventState event, String arenaId,
                                     ResourceLocation bossType, String dimension, BlockPos center, BoundingBox bounds) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return claimArenaInScope(server, event, arenaId, bossType, dimension, center, bounds); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> claimArenaInScope(server, store.events().bind(event), arenaId, bossType, dimension, center, bounds));
    }

    private static boolean claimArenaInScope(MinecraftServer server, EventState event, String arenaId,
                                     ResourceLocation bossType, String dimension, BlockPos center, BoundingBox bounds) {
        if (!com.siirio.jemserver.smp.SmpEnvironment.active(server)) return false;
        if (event.combatStarted()) return false;
        if(event.hasRaidArenaId()) return event.raidArenaId().equals(arenaId)
                &&prepare(server,event);
        var arena = RaidArenaApi.reserve(server, arenaId, bossType, event.id(),
                candidate->StructureStaging.arenaAvailable(server,candidate.id())).orElse(null);
        if (arena == null || !arena.dimension().toString().equals(dimension) || !RaidArenaApi.sameBounds(arena.bounds(), bounds)) return false;
        event.raidArenaId( arena.id());
        event.bossType( bossType.toString());
        StructureStaging.attachArena(event,UUID.fromString(arenaId),bossType,dimension,center,bounds);

        prepare(server,event);
        return true;
    }


    public static void start(ServerPlayer host, PartyState party, Collection<UUID> agreed) {
        var serverForMutation = host.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { startInScope(host, party, agreed); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> startInScope(host, store.parties().bind(party), agreed));
    }

    private static void startInScope(ServerPlayer host, PartyState party, Collection<UUID> agreed) {
        SmpRecords.require(com.siirio.jemserver.smp.SmpEnvironment.active(host), "unavailable");
        SmpRecords.require(party.hasEventId() && !party.hasBossEntity(), "unavailable");
        var data = SmpData.get(host.server);
        var event = data.events().current(party.eventId());
        SmpRecords.require(event != null, "unavailable");
        requireActive(event);
        SmpRecords.require(
                event.hasParty()
                        && event.party().equals(party.id())
                        && !event.hasBossEntity(),
                "combat_locked");
        SmpRecords.require(
                EventRegions.near(event, host.serverLevel(), host.blockPosition()),
                "travel_to_arena");
        var level = EventRegions.level(host.server, event);
        SmpRecords.require(level != null, "unavailable");
        var pos = BlockPos.of(event.position());
        SmpRecords.require(level.hasChunkAt(pos), "unavailable");
        for (UUID participant : agreed) if (event.hasRaidArenaId())
            SmpRecords.require(!RaidArenaApi.participated(host.server, event.raidArenaId(), participant), "raid_already_participated");
        SmpRecords.require(agreed.contains(host.getUUID()), "host_must_be_present");
        SmpRecords.require(prepare(host.server,event)&&event.hasTargetBoss(),"unavailable");
        var entity=level.getEntity(event.targetBoss());
        SmpRecords.require(entity instanceof LivingEntity&&entity.isAlive(),"unavailable");
        LivingEntity boss=(LivingEntity)entity;
        boolean started = false;
        try {
            SmpRecords.require(HostedEncounterApi.prepareRaid(boss),"unavailable");
            pos=boss.blockPosition();
            event.position( pos.asLong());
            boss.getPersistentData().putBoolean("jem_solo", party.solo());
            applyRaidScale(boss);
            boss.getPersistentData().putUUID(EVENT_ID, event.id());
            BoundingBox bounds=StructureStaging.prepareCombat(level,event,boss);
            SmpRecords.require(HostedEncounterApi.start(boss, agreed, true, bounds), "unavailable");
            party.bossEntity(boss.getUUID());
            event.bossEntity( boss.getUUID());
            event.solo( party.solo());
            event.combatStarted( true);
            initializeReinforcements(event,boss);
            LivingEntity startedBoss=boss;
            level.players().forEach(player->EventNetwork.eventMob(player,startedBoss));


            if (event.hasRaidArenaId()) RaidArenaApi.recordParticipation(host.server, event.raidArenaId(), agreed);
            started = true;
        } finally {
            if (!started) {
                HostedEncounterApi.cancel(boss);
                restoreRaidScale(boss);
                HostedEncounterApi.prepareRaid(boss);
                HostedEncounterApi.hold(boss);
            }
        }
    }

    private static void requireActive(EventState event) {
        SmpRecords.require(
                event.state().equals("ACTIVE")
                        && event.activity().equals("BOSS_RAID")
                        && event.ends() > System.currentTimeMillis(),
                "unavailable");
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent tick) {
        var serverForMutation = tick.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { tickInScope(tick); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> tickInScope(tick));
    }

    private static void tickInScope(TickEvent.ServerTickEvent tick) {
        if(tick.phase!=TickEvent.Phase.END||!com.siirio.jemserver.smp.SmpEnvironment.active(tick.getServer())||tick.getServer().getTickCount()%10!=0) return;
        for(EventState event:SmpData.get(tick.getServer()).events().currentRows()) {
            if (!EventRuntime.available(tick.getServer(), event)) continue;
            if(!event.state().equals("ACTIVE")||!event.activity().equals("BOSS_RAID")) continue;
            if(!event.combatStarted()) {prepare(tick.getServer(),event);continue;}
            ServerLevel level=EventRegions.level(tick.getServer(),event);
            if(level==null) continue;
            List<LivingEntity> bosses=livingBosses(level,event);
            if(bosses.isEmpty()) continue;
            double initialHealth = event.raidBossInitialHealth().values().stream().mapToDouble(Double::doubleValue).sum();
            if(initialHealth<=0) continue;
            double ratio=bosses.stream().mapToDouble(LivingEntity::getHealth).sum()/initialHealth;
            int stage=event.raidReinforcementStage();
            while(stage<3&&(stage==0||stage==1&&ratio<=.5||stage==2&&ratio<=.25)) {
                spawnReinforcements(level,event,bosses.get(0),STAGE_COUNTS[stage]);
                stage++;
                event.raidReinforcementStage(stage);

            }
        }
    }

    private static void initializeReinforcements(EventState event,LivingEntity boss) {
        event.raidReinforcementStage(0);
        event.raidAlliesSpawned(0);
        event.raidAllies(List.of());
        event.raidBossInitialHealth(Map.of(boss.getUUID(), (double) boss.getMaxHealth()));
    }

    private static List<LivingEntity> livingBosses(ServerLevel level,EventState event) {
        List<LivingEntity> bosses=new ArrayList<>();
        if(event.hasBossEntity()) {
            Entity entity=level.getEntity(event.bossEntity());
            if(entity instanceof LivingEntity living&&living.isAlive()) bosses.add(living);
        }
        for(UUID value : event.raidBosses()) {
            Entity entity=level.getEntity(value);
            if(entity instanceof LivingEntity living&&living.isAlive()&&!bosses.contains(living)) bosses.add(living);
        }
        return bosses;
    }

    private static void spawnReinforcements(ServerLevel level,EventState event,LivingEntity boss,int requested) {
        List<EntityType<?>> pool=reinforcementPool(boss.getType());
        int remaining=Math.min(requested,MAX_ALLIES-event.raidAlliesSpawned());
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
            mob.getPersistentData().putUUID(EVENT_ID,event.id());
            mob.getPersistentData().putUUID("JEMHostedBoss",boss.getUUID());
            EventMobModifiers.applyRaidMob(mob,event.worldTier(),Math.max(1,targets.size()));
            if(!level.noCollision(mob)||!level.addFreshEntity(mob)) continue;
            if(!targets.isEmpty()) mob.setTarget(targets.get(level.random.nextInt(targets.size())));
            var allies = new ArrayList<>(event.raidAllies());
            allies.add(mob.getUUID());
            event.raidAllies(allies);
            event.raidAlliesSpawned(event.raidAlliesSpawned()+1);
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

    private static void discardAllies(MinecraftServer server,EventState event) {
        ServerLevel level=EventRegions.level(server,event);
        if(level!=null) for(UUID value : event.raidAllies()) {
            Entity entity=level.getEntity(value);
            if(entity!=null) entity.discard();
        }
        event.clearRaidAllies();
    }

    public static void cleanup(MinecraftServer server, EventState event) {
        discardAllies(server,event);
        UUID bossId=event.hasBossEntity()?event.bossEntity():event.hasTargetBoss()?event.targetBoss():null;
        if(bossId!=null&&event.hasRaidArenaId()) {
            var level = EventRegions.level(server, event);
            if (level != null && level.getEntity(bossId) instanceof LivingEntity living) {
                restoreRaidScale(living);
                living.getPersistentData().remove(EVENT_ID);
                level.players().forEach(player -> EventNetwork.eventMob(player, living));
            }
            RaidArenaApi.completeRaid(server,event.raidArenaId(),bossId,new ResourceLocation(event.bossType()));
        } else if(bossId!=null) {
            var level=EventRegions.level(server,event);
            var boss=level==null?null:level.getEntity(bossId);
            if(boss instanceof LivingEntity living&&living.isAlive()) {
                if(event.hasTargetBoss()) {
                    restoreRaidScale(living);
                    level.players().forEach(player->EventNetwork.eventMob(player,living));
                    HostedEncounterApi.cancel(living);
                    HostedEncounterApi.release(living);
                    if(HostedEncounterApi.preservesRaidBoss(living)) HostedEncounterApi.restorePreparedRaid(living);
                    living.getPersistentData().remove(EVENT_ID);
                    HostedEncounterApi.hold(living);
                } else living.discard();
            }
        } else if (event.hasRaidArenaId()) {
            RaidArenaApi.release(server, event.raidArenaId(), event.id());
        }
    }

    public static void resetAttempt(MinecraftServer server, EventState event) {
        if (!com.siirio.jemserver.smp.SmpEnvironment.active(server)) return;
        discardAllies(server,event);
        if (!event.hasBossEntity()) {
            return;
        }
        UUID bossId = event.bossEntity();
        if (event.hasRaidArenaId()) {
            RaidArenaApi.completeRaid(server,event.raidArenaId(),bossId,new ResourceLocation(event.bossType()));
            event.clearTargetBoss();
            RaidArenaApi.reserveAgain(server,event.raidArenaId(),bossId,event.id());
            return;
        }
        var level = EventRegions.level(server, event);
        var boss = level == null ? null : level.getEntity(bossId);
        if(event.hasTargetBoss()) {
            if(boss instanceof LivingEntity living&&living.isAlive()) {HostedEncounterApi.prepareRaid(living);restoreRaidScale(living);HostedEncounterApi.hold(living);}
            return;
        }
        if (boss != null) {
            boss.discard();
        }
        if (event.hasRaidArenaId()) {
            RaidArenaApi.reserveAgain(server, event.raidArenaId(), bossId, event.id());
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void loaded(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        var serverForMutation = event.getEntity().getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { loadedInScope(event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> loadedInScope(event));
    }

    private static void loadedInScope(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity boss)
                || !boss.getPersistentData().hasUUID(EVENT_ID) || WorldTierApi.profile(boss).isEmpty()) return;
        if (!com.siirio.jemserver.smp.SmpEnvironment.active(level.getServer())) {
            restoreRaidScale(boss);
            com.siirio.jemworldbosstiers.encounter.HostedEncounters.restoreNative(boss);
            boss.getPersistentData().remove(EVENT_ID);
            return;
        }
        var row = SmpData.get(level.getServer()).events().current(boss.getPersistentData().getUUID(EVENT_ID));
        if (EventRuntime.available(level.getServer(), row) && row.state().equals("ACTIVE") && row.ends() > System.currentTimeMillis()) return;
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
