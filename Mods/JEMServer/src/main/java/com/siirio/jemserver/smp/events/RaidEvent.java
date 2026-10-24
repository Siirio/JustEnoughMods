package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import com.siirio.jemworldbosstiers.api.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="jem_server")
public final class RaidEvent {
    public static final String EVENT_ID = "jem_smp_raid";
    private static final String FORGE_SPAWN_TYPE="forge:spawn_type";
    private static final String MOB_SUMMONED="MOB_SUMMONED";
    private static final String ORIGINAL_SCALE="jem:raid_original_scale";
    private static final String ORIGINAL_SCALE_PRESENT="jem:raid_original_scale_present";
    private static final String ORIGINAL_PEHKUI="jem:raid_original_pehkui";

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
        if(HostedEncounterApi.damageOwner(mob) instanceof ServerPlayer) return;
        var event=EventScheduler.active(level.getServer(),"BOSS_RAID");
        if(event==null || !event.combatStarted() || !event.hasBossEntity() || !EventRegions.contains(event,level,mob.getBoundingBox())) return;
        if(data.hasUUID("JEMHostedBoss") && !data.getUUID("JEMHostedBoss").equals(event.bossEntity())) return;
        new EventSession(event).mark(mob);
        data.putUUID(EVENT_ID,event.id());
        if(event.hasBossEntity()) data.putUUID("JEMHostedBoss",event.bossEntity());

    }

    public static boolean belongs(EventState event,net.minecraft.world.entity.Entity entity) {
        var data=entity.getPersistentData();
        return data.hasUUID(EVENT_ID)&&data.getUUID(EVENT_ID).equals(event.id());
    }

    /** Native summons survive terminal/reload cleanup. Only JEM attempt markers are retired. */
    public static boolean clearNativeSummon(Entity entity) {
        var tag=entity.getPersistentData();
        if(!tag.hasUUID(EVENT_ID)||!MOB_SUMMONED.equals(tag.getString(FORGE_SPAWN_TYPE))) return false;
        UUID event=tag.getUUID(EVENT_ID);
        if(tag.hasUUID(EventSession.SESSION)&&event.equals(tag.getUUID(EventSession.SESSION))) {
            tag.remove(EventSession.SESSION); tag.remove(EventSession.SPAWNED); tag.remove(EventSession.TYPE);
        }
        tag.remove(EVENT_ID);
        return true;
    }

    private static void clearNativeSummons(MinecraftServer server,EventState event) {
        var level=EventRegions.level(server,event);
        var bounds=event.structureBounds();
        if(level==null||bounds==null) return;
        var volume=new net.minecraft.world.phys.AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX()+1D,bounds.maxY()+1D,bounds.maxZ()+1D);
        for(var mob:level.getEntitiesOfClass(Mob.class,volume,candidate->belongs(event,candidate))) clearNativeSummon(mob);
    }

    public static boolean prepare(MinecraftServer server, EventState row) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return prepareInScope(server, row); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> prepareInScope(server, store.events().bind(row)));
    }

    private static boolean prepareInScope(MinecraftServer server, EventState row) {
        if(!EventRuntime.available(server, row) || row.combatStarted() || !row.pendingOutcome().isEmpty()) return false;
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
        if (event.combatStarted() || !event.pendingOutcome().isEmpty()) return false;
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
        var previousEvent = event.copy();
        try {
            var snapshot = new EventParticipants(host.getUUID(), java.util.List.copyOf(agreed), party.solo());
            new EncounterContext(event).initialize(host, snapshot);
            data.flush(host.server);
            SmpRecords.require(HostedEncounterApi.prepareRaid(boss),"unavailable");
            pos=boss.blockPosition();
            event.position( pos.asLong());
            boss.getPersistentData().putBoolean("jem_solo", party.solo());
            boss.getPersistentData().putUUID(EVENT_ID, event.id());
            BoundingBox bounds=StructureStaging.prepareCombat(level,event,boss);
            SmpRecords.require(StructureStaging.ejectUnrelated(level, bounds, boss, agreed), "arena_occupied");
            applyRaidScale(boss, bounds);
            SmpRecords.require(HostedEncounterApi.start(boss, agreed, true, bounds, party.id(),
                    attempt -> HostedParties.commitHostedStart(host.server, party, boss, attempt)), "unavailable");
            party.bossEntity(boss.getUUID());
            party.hostedAttempt(HostedEncounterApi.attemptId(boss));
            event.bossEntity( boss.getUUID());
            event.solo( party.solo());
            event.combatStarted( true);
            LivingEntity startedBoss=boss;
            level.players().forEach(player->EventNetwork.eventMob(player,startedBoss));


            started = true;
        } finally {
            if (!started) {
                boolean participationAcknowledged = true;
                var failedAttempt = party.hostedAttempt();
                if (failedAttempt != null && event.hasRaidArenaId()) {
                    var world = com.siirio.jemworldbosstiers.progression.WorldTierData.get(host.server);
                    world.rollbackRaidParticipation(event.raidArenaId(), failedAttempt, agreed);
                    participationAcknowledged = world.flush(host.server);
                }
                data.events().replace(previousEvent);
                HostedEncounterApi.cancel(boss);
                restoreRaidScale(boss);
                HostedEncounterApi.prepareRaid(boss);
                HostedEncounterApi.hold(boss);
                data.flush(host.server);
                SmpRecords.require(participationAcknowledged, "storage_unavailable");
            }
        }
    }

    private static void requireActive(EventState event) {
        SmpRecords.require(
                event.state().equals("ACTIVE")
                        && event.pendingOutcome().isEmpty()
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
        }
    }

    private static void applyRaidScale(LivingEntity boss, BoundingBox bounds) {
        CompoundTag data=boss.getPersistentData();
        if(data.contains(ORIGINAL_SCALE_PRESENT)) return;
        float original=data.getFloat(EventMobScale.KEY);
        data.putBoolean(ORIGINAL_SCALE_PRESENT,data.contains(EventMobScale.KEY));
        data.putFloat(ORIGINAL_SCALE,original);
        var nativeScale=virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(boss);
        data.put(ORIGINAL_PEHKUI,nativeScale.writeNbt(new CompoundTag()));
        float current=nativeScale.getBaseScale();
        if(!Float.isFinite(current)||current<=0) return;
        var originalBox=boss.getBoundingBox();
        var interior=new net.minecraft.world.phys.AABB(bounds.minX(),bounds.minY()+SolidBoundary.WALL_THICKNESS,bounds.minZ(),
                bounds.maxX()+1D,bounds.maxY()+1D-SolidBoundary.WALL_THICKNESS,bounds.maxZ()+1D);
        double factor=safeScaleFactor(originalBox,boss.position(),interior,
                box -> boss.level().noCollision(boss,box));
        float scaled=(float)Math.min(EventMobScale.MAX_SCALE,current*factor);
        // Tiny native bosses retain their native baseline if the presentation transport cannot encode it.
        if(factor<=1 || scaled<=current || !EventMobScale.supported(scaled)) return;
        EventMobScale.apply(boss,scaled);
        boss.refreshDimensions();
        if(!fits(boss.getBoundingBox(),interior)||!boss.level().noCollision(boss,boss.getBoundingBox())) {
            restoreRaidScale(boss);
        }
    }

    static double safeScaleFactor(net.minecraft.world.phys.AABB box, net.minecraft.world.phys.Vec3 origin,
                                  net.minecraft.world.phys.AABB interior, java.util.function.Predicate<net.minecraft.world.phys.AABB> clear) {
        if(!fits(box,interior)||!clear.test(box)) return 1D;
        double low=1D,high=1.35D;
        for(int step=0;step<16;step++) {
            double factor=(low+high)/2D;
            var scaled=scaledBox(box,origin,factor);
            if(fits(scaled,interior)&&clear.test(scaled)) low=factor;
            else high=factor;
        }
        return low;
    }

    private static net.minecraft.world.phys.AABB scaledBox(net.minecraft.world.phys.AABB box,net.minecraft.world.phys.Vec3 origin,double factor) {
        return new net.minecraft.world.phys.AABB(origin.x+(box.minX-origin.x)*factor,origin.y+(box.minY-origin.y)*factor,origin.z+(box.minZ-origin.z)*factor,
                origin.x+(box.maxX-origin.x)*factor,origin.y+(box.maxY-origin.y)*factor,origin.z+(box.maxZ-origin.z)*factor);
    }

    private static boolean fits(net.minecraft.world.phys.AABB box,net.minecraft.world.phys.AABB interior) {
        return box.minX>=interior.minX&&box.minY>=interior.minY&&box.minZ>=interior.minZ
                &&box.maxX<=interior.maxX&&box.maxY<=interior.maxY&&box.maxZ<=interior.maxZ;
    }

    private static void restoreRaidScale(LivingEntity boss) {
        CompoundTag data=boss.getPersistentData();
        if(!data.contains(ORIGINAL_SCALE_PRESENT)) return;
        if(data.contains(ORIGINAL_PEHKUI,net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(boss).readNbt(data.getCompound(ORIGINAL_PEHKUI));
            if(data.getBoolean(ORIGINAL_SCALE_PRESENT)) data.putFloat(EventMobScale.KEY,data.getFloat(ORIGINAL_SCALE));
            else data.remove(EventMobScale.KEY);
            boss.refreshDimensions();
        } else if(data.getBoolean(ORIGINAL_SCALE_PRESENT)) EventMobScale.apply(boss,data.getFloat(ORIGINAL_SCALE));
        else EventMobScale.clear(boss);
        data.remove(ORIGINAL_SCALE);
        data.remove(ORIGINAL_SCALE_PRESENT);
        data.remove(ORIGINAL_PEHKUI);
    }

    public static void cleanup(MinecraftServer server, EventState event) {
        clearNativeSummons(server,event);
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
        closeBeforeReset(() -> closeHostedAttempt(server, event), () -> resetClosedAttempt(server, event));
    }

    /** A reset cannot retire or replace the boss while its canonical combat owner remains active. */
    static void closeBeforeReset(Runnable closeCanonical, Runnable reset) {
        closeCanonical.run();
        reset.run();
    }

    static void closeHostedAttempt(MinecraftServer server, EventState event) {
        if (!event.activity().equals("BOSS_RAID") || !event.hasBossEntity()) return;
        SmpRecords.require(event.hasAttemptId(), "encounter_recovery_pending");
        var attempt = event.attemptId();
        var world = com.siirio.jemworldbosstiers.progression.WorldTierData.get(server);
        var level = EventRegions.level(server, event);
        var entity = level == null ? null : level.getEntity(event.bossEntity());
        if (entity instanceof LivingEntity boss) {
            UUID current = HostedEncounterApi.attemptId(boss);
            SmpRecords.require(current == null || current.equals(attempt), "combat_locked");
            if (current != null) SmpRecords.require(HostedEncounterApi.cancel(boss), "encounter_recovery_pending");
        }
        SmpRecords.require(!world.hostedAttempts().containsKey(attempt)
                && !world.hostedSettlements().containsKey(attempt), "encounter_recovery_pending");
    }

    private static void resetClosedAttempt(MinecraftServer server, EventState event) {
        clearNativeSummons(server,event);
        if (!event.hasBossEntity()) {
            return;
        }
        UUID bossId = event.bossEntity();
        var scaleLevel=EventRegions.level(server,event);
        if(scaleLevel!=null&&scaleLevel.getEntity(bossId) instanceof LivingEntity living) restoreRaidScale(living);
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
        restoreRaidScale(boss);
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
