package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.*;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.*;

public final class BloodMoon {
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(BloodMoon.class);
    public static final String EVENT_ID = "jem_smp_blood_moon";
    private static final String CATEGORY="jem:blood_category";
    private static final String WAVE="jem:blood_wave";
    private static final String SUMMONED="jem:blood_summoned";
    private static final String FORGE_SPAWN_TYPE="forge:spawn_type";
    private static final String MOB_SUMMONED="MOB_SUMMONED";
    private static final String SINGLE_BATCH_WAVE_THREE="singleBatchWaveThree";
    private static final double HORDE_FILLER_CHANCE=.15;
    private static final double VANILLA_RANGED_CHANCE=.25;
    private static final double ARACHNID_SPIDER_CHANCE=.55;
    private static final float WITCH_SCALE_MULTIPLIER=1.20F;
    private static final int ARENA_SPAWN_ATTEMPT_MULTIPLIER=4;
    private static final String SPAWN_FAILURES="spawnFailures";
    private static final int FALLBACK_AFTER_FAILURES=8;
    private static final int SKIP_AFTER_FAILURES=24;
    private static final String WEATHER="bloodMoonWeather";

    public static boolean startManual(MinecraftServer server, EventState row) {
        return true;
    }

    private static final int MAX_ACTIVE_SUMMONS=3;

    public static boolean adoptSummon(ServerLevel level,Entity entity) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return adoptSummonInScope(level, entity); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> adoptSummonInScope(level, entity));
    }

    private static boolean adoptSummonInScope(ServerLevel level,Entity entity) {
        if(!(entity instanceof Mob mob) || !(mob instanceof Enemy)) return true;
        var data=mob.getPersistentData();
        if(data.hasUUID(EVENT_ID) || !MOB_SUMMONED.equals(data.getString(FORGE_SPAWN_TYPE))) return true;
        for(var row:SmpData.get(level.getServer()).events().currentRows()) {
            if(!row.state().equals("ACTIVE") || !row.activity().equals("BLOOD_MOON")
                    || !row.combatStarted() || !EventRegions.contains(row,level,mob.blockPosition())) continue;
            Entity owner=EventSession.owner(mob);
            // Player summons retain their native owner and never become hostile wave entries.
            if(owner instanceof net.minecraft.server.level.ServerPlayer) return true;
            long active=row.mobs().stream().map(level::getEntity)
                    .filter(candidate->candidate instanceof Mob living&&living.isAlive()&&living.getPersistentData().getBoolean(SUMMONED)).count();
            if(active>=MAX_ACTIVE_SUMMONS) return false;
            new EventSession(row).mark(mob);
            data.putUUID(EVENT_ID,row.id());
            data.putUUID(BloodMoonLoot.ATTEMPT_ID,BloodMoonLoot.attempt(row));
            data.putBoolean(SUMMONED,true);
            data.putInt(WAVE,row.wave());
            EventMobModifiers.applyBloodMoonSummon(mob);
            var mobs=new ArrayList<>(row.mobs());
            if(!mobs.contains(mob.getUUID())) mobs.add(mob.getUUID());
            row.mobs(mobs);

            return true;
        }
        return true;
    }

    public static void tick(MinecraftServer server, EventState row) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { tickInScope(server, row); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> tickInScope(server, store.events().bind(row)));
    }

    private static void tickInScope(MinecraftServer server, EventState row) {
        var level=EventRegions.level(server,row);
        if(level==null) return;
        protectWeather(level,row);
        var encounter=new EncounterContext(row);
        if(!encounter.tickStartup(server)) { return; }
        if(!row.combatStarted()) {
            if(!encounter.allParticipantsInside(server)) return;
            row.combatStarted(true);
            row.nextWave(System.currentTimeMillis());
            EventRegions.ejectOutsiders(level,row);

        }
        if(BloodMoonVoting.active(row)) { BloodMoonVoting.tick(server,row); return; }
        var active=new EventSession(row).active(server);
        var mobs=new ArrayList<>(row.mobs());
        if(active.isEmpty()) {
            for(UUID tag:mobs) {
                var entity=level.getEntity(tag);
                if(entity instanceof Mob mob) clearInvalidTarget(mob);
            }
            return;
        }
        var targets=new HashMap<UUID,Integer>();
        active.forEach(player -> targets.put(player.getUUID(),0));
        var living=new EnumMap<BloodMoonWaves.Category,Integer>(BloodMoonWaves.Category.class);
        boolean dirty=false;
        for(int index=mobs.size()-1;index>=0;index--) {
            var entity=level.getEntity(mobs.get(index));
            if(!(entity instanceof Mob mob) || !mob.isAlive()) {
                mobs.remove(index);
                row.waveKilled(Math.min(row.waveTotal(),row.waveKilled()+1));
                dirty=true;
                continue;
            }
            clearInvalidTarget(mob);
            var name=mob.getPersistentData().getString(CATEGORY);
            if(!name.isEmpty()) living.merge(BloodMoonWaves.Category.valueOf(name),1,Integer::sum);
            if(mob.getTarget()!=null && canTarget(mob,mob.getTarget())
                    && EventSession.owner(mob.getTarget()) instanceof net.minecraft.server.level.ServerPlayer player
                    && active.contains(player)) targets.merge(player.getUUID(),1,Integer::sum);
            else {
                var target=target(active,targets,mob);
                mob.setTarget(target);
                targets.merge(target.getUUID(),1,Integer::sum);
            }
        }
        int wave=row.wave();
        dirty|=reconcileWaveThree(row,mobs,wave);
        row.mobs(mobs);
        int recovered=BloodMoonSetPieces.recoverMissingSpawn(level,row,active,wave,row.worldTier());
        mobs = new ArrayList<>(row.mobs());
        if(recovered>0) {
            row.waveTotal(row.waveTotal()+recovered);
            row.waveSpawned(row.waveSpawned()+recovered);
            dirty=true;
        }
        if(wave>0&&remaining(row)==0&&mobs.isEmpty()) {
            BloodMoonSetPieces.finishWave(level,row,wave);
            if(row.rewardedWave()<wave) {
                for(var player:active) {
                    var member=row.memberOrEmpty(player.getUUID());
                    member.waves(member.waves()+1);
                }
                BloodMoonRewards.stage(level,row,active,wave);
                row.rewardedWave(wave);
            }
            if(wave==BloodMoonWaves.WAVES) { complete(server,row); return; }
            if(row.continuedWave()<wave) {
                BloodMoonVoting.open(server,row,wave);
                return;
            }
        }
        if(wave==0 || row.continuedWave()==wave) {
            if(System.currentTimeMillis()<row.nextWave()) return;
            wave++;
            int tier=WorldTierApi.currentTier(server);
            row.wave(wave);row.worldTier(tier);
            recordWavePerformance(level,row);
            EventMobModifiers.resetWave(row);
            int setPieces=BloodMoonSetPieces.beginWave(level,row,active,wave,tier);
            mobs = new ArrayList<>(row.mobs());
            int ordinaryTotal=BloodMoonWaves.ordinaryTotal(tier,wave,active.size(),setPieces);
            row.waveTotal(ordinaryTotal);
            row.waveSpawned(0);row.waveKilled(0);
            row.batchSpawned(0);
            row.batchNumber(1);
            row.batchTarget(BloodMoonWaves.batchTarget(wave,active.size(),ordinaryTotal));
            row.clearSpawnFailures();
            if(wave==3) row.singleBatchWaveThree(true);
            else row.clearSingleBatchWaveThree();
            var remaining=new LinkedHashMap<String,Integer>();
            var composition=ordinaryTotal==0?Map.<BloodMoonWaves.Category,Integer>of():BloodMoonWaves.composition(wave,ordinaryTotal);
            int fallback=0;
            for(var entry:composition.entrySet()) {
                if(entry.getValue()>0 && roster(entry.getKey(),wave,tier).isEmpty()) fallback+=entry.getValue();
                else remaining.put(entry.getKey().name(),entry.getValue());
            }
            remaining.put(BloodMoonWaves.Category.HORDE.name(),remaining.getOrDefault(BloodMoonWaves.Category.HORDE.name(),0)+fallback);
            row.remaining(remaining);
            row.waveTotal(ordinaryTotal+setPieces);
            row.waveSpawned(setPieces);
            dirty=true;
        }
        var remaining=new LinkedHashMap<>(row.remaining());
        if(mobs.isEmpty()&&row.batchSpawned()>=row.batchTarget()&&remaining(row)>0) {
            row.batchSpawned(0);
            row.batchNumber(row.batchNumber()+1);
            row.batchTarget(BloodMoonWaves.batchTarget(wave,active.size(),remaining(row)));
        }
        int budget=Math.min(EventRules.SPAWN_BATCH.get(),Math.max(0,row.batchTarget()-row.batchSpawned()));
        budget=(int)Math.ceil(budget*BloodMoonSetPieces.ordinarySpawnPressure(level,row));
        for(int i=0;i<budget;i++) {
            var available=new ArrayList<BloodMoonWaves.Category>();
            for(var kind:BloodMoonWaves.Category.values()) {
                if(remaining.getOrDefault(kind.name(),0)<=0) continue;
                available.add(kind);
            }
            if(available.isEmpty()) break;
            var kind=available.get(level.random.nextInt(available.size()));
            int failures=row.spawnFailures().getOrDefault(kind.name(),0);
            Mob mob=spawn(level,row,kind,wave,row.worldTier(),active,failures>=FALLBACK_AFTER_FAILURES);
            if(mob==null) {
                failedSpawn(row,remaining,kind);
                continue;
            }
            var target=target(active,targets,mob);
            mob.setTarget(target);
            var mobId=mob.getUUID();
            mobs.add(mobId);
            row.mobs(mobs);
            if(!mob.isAddedToWorld() && !level.addFreshEntity(mob)) {
                mobs.remove(mobId);
                row.mobs(mobs);
                failedSpawn(row,remaining,kind);
                continue;
            }
            var failuresAfterSpawn = new LinkedHashMap<>(row.spawnFailures()); failuresAfterSpawn.remove(kind.name()); row.spawnFailures(failuresAfterSpawn);
            BloodMoonSetPieces.prepareSpawn(row,mob,mob.getPersistentData().getBoolean("jem:event_elite"));
            targets.merge(target.getUUID(),1,Integer::sum);
            living.merge(kind,1,Integer::sum);
            remaining.put(kind.name(),remaining.getOrDefault(kind.name(),0)-1);
            row.waveSpawned(row.waveSpawned()+1);
            row.batchSpawned(row.batchSpawned()+1);
            dirty=true;
        }
        row.remaining(remaining);
        row.mobs(mobs);
        if(dirty) {
            row.mobs(mobs);

        }
    }

    private static void protectWeather(ServerLevel level,EventState row) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { protectWeatherInScope(level, row); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> protectWeatherInScope(level, store.events().bind(row)));
    }

    private static void protectWeatherInScope(ServerLevel level,EventState row) {
        var server=level.getServer();
        var weather=server.getWorldData().overworldData();
        if(!row.hasBloodMoonWeather()) {
            var snapshot=new EventWeather();
            snapshot.clear(weather.getClearWeatherTime());
            snapshot.rain(weather.getRainTime());
            snapshot.thunder(weather.getThunderTime());
            snapshot.raining(weather.isRaining());
            snapshot.thundering(weather.isThundering());
            row.bloodMoonWeather(snapshot);

        }
        boolean changed=weather.isRaining()||weather.isThundering()||level.getRainLevel(1)>0||level.getThunderLevel(1)>0;
        level.setWeatherParameters(Integer.MAX_VALUE,0,false,false);
        level.setRainLevel(0);
        level.setThunderLevel(0);
        if(changed) {
            var players=server.getPlayerList();
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING,0),level.dimension());
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0),level.dimension());
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,0),level.dimension());
        }
    }

    private static void restoreWeather(MinecraftServer server,EventState row) {
        if(row.state().equals("ACTIVE")||!row.hasBloodMoonWeather()) return;
        var snapshot=row.bloodMoonWeather();
        var weather=server.getWorldData().overworldData();
        weather.setClearWeatherTime(snapshot.clear());
        weather.setRainTime(snapshot.rain());
        weather.setThunderTime(snapshot.thunder());
        weather.setRaining(snapshot.raining());
        weather.setThundering(snapshot.thundering());
        row.clearBloodMoonWeather();
    }

    private static boolean reconcileWaveThree(EventState row,List<UUID> mobs,int wave) {
        if(wave!=3||row.singleBatchWaveThree()) return false;
        row.singleBatchWaveThree(true);
        row.remaining(Map.of());
        int total=row.waveKilled()+mobs.size();
        row.waveTotal(total);
        row.waveSpawned(total);
        row.batchSpawned(mobs.size());
        row.batchTarget(mobs.size());
        return true;
    }

    private static void recordWavePerformance(ServerLevel level,EventState row) {
        double damage=0;
        for(var member:row.members().values()) damage+=member.damage();
        long now=level.getGameTime();
        long started=row.waveStartedTick();
        if(started>0&&now>started) {
            double observed=(damage-row.waveDamageStart())/((now-started)/20D);
            if(Double.isFinite(observed)&&observed>0) row.observedPartyDps(observed);
        }
        row.waveDamageStart(damage);
        row.waveStartedTick(now);
    }

    private static int remaining(EventState row) {
        var remaining=row.remaining();
        int total=0;
        for(var category:BloodMoonWaves.Category.values()) total+=Math.max(0,remaining.getOrDefault(category.name(),0));
        return total;
    }

    private static net.minecraft.server.level.ServerPlayer target(
            List<net.minecraft.server.level.ServerPlayer> active,
            Map<UUID,Integer> targets,
            Mob mob) {
        return active.stream()
                .min(Comparator.comparingInt((net.minecraft.server.level.ServerPlayer player) -> targets.getOrDefault(player.getUUID(),0))
                        .thenComparingDouble(mob::distanceToSqr)
                        .thenComparing(player -> player.getUUID().toString()))
                .orElseThrow();
    }

    private static Mob spawn(ServerLevel level,EventState row,BloodMoonWaves.Category kind,int wave,int tier,List<net.minecraft.server.level.ServerPlayer> active,boolean fallback) {
        var roster=fallback?List.<EntityType<?>>of(fallback(kind)):weightedRoster(level,row,kind,wave,tier);
        if(roster.isEmpty()) return null;
        int minX=EventRegions.minX(row),minZ=EventRegions.minZ(row),maxX=EventRegions.maxX(row),maxZ=EventRegions.maxZ(row);
        for(int attempt=0;attempt<EventRules.SPAWN_ATTEMPTS.get();attempt++) {
            var anchor=active.get(level.random.nextInt(active.size()));
            int minimum=EventRules.BLOOD_SPAWN_MIN_DISTANCE.get();
            int maximum=Math.max(minimum,EventRules.BLOOD_SPAWN_MAX_DISTANCE.get());
            double angle=level.random.nextDouble()*Math.PI*2;
            int distance=minimum+level.random.nextInt(maximum-minimum+1);
            int x=(int)Math.floor(anchor.getX()+Math.cos(angle)*distance);
            int z=(int)Math.floor(anchor.getZ()+Math.sin(angle)*distance);
            if(x<=minX || x>=maxX || z<=minZ || z>=maxZ) continue;
            var mob=createMob(level,row,roster,kind,wave,tier,x,z);
            if(mob!=null) return mob;
        }
        int width=maxX-minX-1;
        int depth=maxZ-minZ-1;
        if(width<=0||depth<=0) return null;
        int attempts=Math.max(EventRules.SPAWN_ATTEMPTS.get(),EventRules.SPAWN_ATTEMPTS.get()*ARENA_SPAWN_ATTEMPT_MULTIPLIER);
        for(int attempt=0;attempt<attempts;attempt++) {
            int x=minX+1+level.random.nextInt(width);
            int z=minZ+1+level.random.nextInt(depth);
            var mob=createMob(level,row,roster,kind,wave,tier,x,z);
            if(mob!=null) return mob;
        }
        return null;
    }

    private static Mob createMob(ServerLevel level,EventState row,List<EntityType<?>> roster,BloodMoonWaves.Category kind,int wave,int tier,int x,int z) {
        var column=new BlockPos(x,0,z);
        if(!level.hasChunkAt(column)) return null;
        var pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column);
        if(!level.getFluidState(pos).isEmpty() || !level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)) return null;
        var entity=roster.get(level.random.nextInt(roster.size())).create(level);
        if(!(entity instanceof Mob mob) || WorldTierApi.profile(mob).isPresent()) return null;
        mob.moveTo(x+.5,pos.getY(),z+.5,0,0);
        if(!level.noCollision(mob)) return null;
        new EventSession(row).mark(mob);
        mob.getPersistentData().putUUID(EVENT_ID,row.id());
        mob.getPersistentData().putUUID(BloodMoonLoot.ATTEMPT_ID,BloodMoonLoot.attempt(row));
        mob.getPersistentData().putString(CATEGORY,kind.name());
        mob.getPersistentData().putInt(WAVE,wave);
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.EVENT,null,null);
        mob.setPersistenceRequired();
        EventMobModifiers.applyBloodMoon(mob,row,tier,wave);
        if(mob instanceof Witch) {
            float scale=Math.max(1,mob.getPersistentData().getFloat(EventMobScale.KEY));
            EventMobScale.apply(mob,scale*WITCH_SCALE_MULTIPLIER);
        }
        if(!level.noCollision(mob)) return null;
        mob.setHealth(mob.getMaxHealth());
        return mob;
    }

    private static EntityType<?> fallback(BloodMoonWaves.Category kind) {
        return switch(kind) {
            case HORDE -> EntityType.ZOMBIE;
            case HUNTER -> EntityType.PILLAGER;
            case RANGED -> EntityType.SKELETON;
            case FLYING -> EntityType.PHANTOM;
            case SUPPORT -> EntityType.WITCH;
            case TELEPORTER -> EntityType.ENDERMAN;
            case CREEPER -> EntityType.CREEPER;
            case ARACHNID -> EntityType.SPIDER;
        };
    }

    private static void failedSpawn(EventState row,Map<String,Integer> remaining,BloodMoonWaves.Category kind) {
        var failures=new LinkedHashMap<>(row.spawnFailures());
        int attempts=failures.getOrDefault(kind.name(),0)+1;
        if(attempts<SKIP_AFTER_FAILURES) {
            failures.put(kind.name(),attempts);
            row.spawnFailures(failures);
            return;
        }
        failures.remove(kind.name());
        row.spawnFailures(failures);
        remaining.put(kind.name(),Math.max(0,remaining.getOrDefault(kind.name(),0)-1));
        row.waveTotal(Math.max(row.waveKilled()+new ArrayList<>(row.mobs()).size(),row.waveTotal()-1));
        LOGGER.warn("Skipping unspawnable Blood Moon slot event={} wave={} category={}",row.id(),row.wave(),kind);
    }

    private static List<EntityType<?>> weightedRoster(ServerLevel level,EventState row,BloodMoonWaves.Category kind,int wave,int tier) {
        String fallback=rosterPath(kind,wave,tier);
        String variant=switch(kind) {
            case HORDE -> level.random.nextDouble()<HORDE_FILLER_CHANCE ? "horde/filler" : "horde/wave_"+wave;
            case ARACHNID -> wave==1 || level.random.nextDouble()<ARACHNID_SPIDER_CHANCE ? "arachnid/spiders" : "arachnid/others";
            case RANGED -> level.random.nextDouble()<VANILLA_RANGED_CHANCE ? "ranged/low_weight" : "ranged/primary";
            default -> fallback;
        };
        var selected=new ArrayList<>(types(variant));
        if(selected.isEmpty()) selected.addAll(types(fallback));
        if(wave==2&&row.batchNumber()>=2&&kind==BloodMoonWaves.Category.HUNTER
                &&net.mcreator.skarriermobs.procedures.CarnagerEntitySpawningConditionProcedure.execute(level)) {
            EntityType<?> carnager=BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("skarrier_mobs","carnager"));
            if(carnager!=EntityType.PIG) selected.add(carnager);
        }
        return selected;
    }

    private static List<EntityType<?>> roster(BloodMoonWaves.Category kind,int wave,int tier) {
        return types(rosterPath(kind,wave,tier));
    }

    private static String rosterPath(BloodMoonWaves.Category kind,int wave,int tier) {
        return switch(kind) {
            case HORDE -> "horde/wave_"+wave;
            case HUNTER -> "hunter/wave_"+wave;
            case FLYING -> "flying/wave_"+wave;
            default -> kind.name().toLowerCase(Locale.ROOT);
        };
    }

    private static List<EntityType<?>> types(String path) {
        var key=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("jem_server","blood_moon/"+path));
        return BuiltInRegistries.ENTITY_TYPE.getTag(key).stream().flatMap(set->set.stream()).map(net.minecraft.core.Holder::value).toList();
    }

    private static void complete(MinecraftServer server,EventState row) {
        BloodMoonRewards.commit(server,row,true);
        finishSuccess(server,row);
    }

    public static void cashOut(MinecraftServer server, EventState row) {
        BloodMoonRewards.commit(server,row,false);
        finishSuccess(server,row);
    }

    private static void finishSuccess(MinecraftServer server, EventState row) {
        for(var player:new EventSession(row).active(server)) if(row.memberOrEmpty(player.getUUID()).contributed()) {
            Profiles.count(server,player.getUUID(),ProfileCounter.BLOOD_MOON_CLEARS);
            Profiles.countEvent(server,player.getUUID(),"BLOOD_MOON");
        }
        CombatNetwork.completedBloodMoon(server,row);
        EventScheduler.finish(server,row,"COMPLETED");
    }

    public static void fail(MinecraftServer server, EventState row) {
        if(row.state().equals("ACTIVE")) EventScheduler.failAttempt(server,row);
    }

    public static void defeated(MinecraftServer server,LivingEntity entity) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { defeatedInScope(server, entity); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> defeatedInScope(server, entity));
    }

    private static void defeatedInScope(MinecraftServer server,LivingEntity entity) {
        if(!entity.getPersistentData().hasUUID(EVENT_ID)) return;
        var row=SmpData.get(server).events().current(entity.getPersistentData().getUUID(EVENT_ID));
        if(row==null || !row.state().equals("ACTIVE")) return;
        var mobs=new ArrayList<>(row.mobs());
        if(mobs.remove(entity.getUUID())) {
            row.mobs(mobs);
            if(!entity.getPersistentData().getBoolean(SUMMONED)) row.waveKilled(row.waveKilled()+1);
            var source=entity.getLastDamageSource();
            var owner=source==null?null:EventSession.owner(source.getEntity());
            if(owner==null && source!=null) owner=EventSession.owner(source.getDirectEntity());
            if(owner instanceof net.minecraft.server.level.ServerPlayer player && new EventSession(row).accepted(player.getUUID())) {
                var member=row.memberOrEmpty(player.getUUID());
                member.kills(member.kills()+1);
            }
        }

    }
    public static void removed(MinecraftServer server, LivingEntity entity) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { removedInScope(server, entity); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> removedInScope(server, entity));
    }

    private static void removedInScope(MinecraftServer server, LivingEntity entity) {
        var reason = entity.getRemovalReason();
        if (reason == null || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK
                || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) return;
        if (reason == Entity.RemovalReason.KILLED) { defeated(server, entity); return; }
        var data = entity.getPersistentData();
        if (!data.hasUUID(EVENT_ID)) return;
        var row = SmpData.get(server).events().current(data.getUUID(EVENT_ID));
        if (row == null || !row.state().equals("ACTIVE")) return;
        var mobs = new ArrayList<>(row.mobs());
        if (!mobs.remove(entity.getUUID())) return;
        row.mobs(mobs);
        if (data.getBoolean(SUMMONED)) {

            return;
        }
        String category = data.getString(CATEGORY);
        if(category.isEmpty()) {
            row.waveKilled(row.waveKilled()+1);

            return;
        }
        var remaining = new LinkedHashMap<>(row.remaining());
        remaining.put(category, remaining.getOrDefault(category,0) + 1);
        row.remaining(remaining);
        row.waveSpawned( Math.max(0, row.waveSpawned() - 1));

    }

    public static void cleanup(MinecraftServer server,EventState row) {
        restoreWeather(server,row);
        BloodMoonSetPieces.cleanup(server,row);
        var level=EventRegions.level(server,row);
        if(level!=null) {
            for(UUID tag:row.mobs()) {
                var entity=level.getEntity(tag);
                discard(entity);
            }
            var bounds=new net.minecraft.world.phys.AABB(EventRegions.minX(row),level.getMinBuildHeight(),EventRegions.minZ(row),
                    EventRegions.maxX(row)+1,level.getMaxBuildHeight(),EventRegions.maxZ(row)+1);
            for(var entity:level.getEntities((Entity)null,bounds,candidate->candidate.getPersistentData().hasUUID(EVENT_ID)
                    && candidate.getPersistentData().getUUID(EVENT_ID).equals(row.id()))) discard(entity);
        }
        row.clearMobs();
    }
    public static boolean belongs(EventState row,Entity entity) {
        var data=entity.getPersistentData();
        return data.hasUUID(EVENT_ID)&&data.getUUID(EVENT_ID).equals(row.id());
    }
    public static boolean tracked(EventState row,Entity entity) {
        return new ArrayList<>(row.mobs()).contains(entity.getUUID());
    }
    public static boolean canTarget(Entity eventMob,Entity target) {
        if (!EventRuntime.enabled(eventMob.getServer())) return true;
        var data=eventMob.getPersistentData();
        if(!data.hasUUID(EVENT_ID)||eventMob.getServer()==null) return true;
        var row=SmpData.get(eventMob.getServer()).events().snapshot(data.getUUID(EVENT_ID));
        if(row==null||!row.state().equals("ACTIVE")||!row.activity().equals("BLOOD_MOON")) return true;
        return row.combatStarted() && eventMob instanceof LivingEntity actor
                && target instanceof LivingEntity recipient
                && EventSession.owner(recipient) instanceof net.minecraft.server.level.ServerPlayer
                && EventSession.canInteract(actor,recipient);
    }
    private static void clearInvalidTarget(Mob mob) {
        boolean validTarget=mob.getTarget()!=null&&canTarget(mob,mob.getTarget());
        if(mob.getTarget()!=null&&!validTarget) mob.setTarget(null);
        if(!(mob instanceof Warden warden)) return;
        var angry=warden.getEntityAngryAt().orElse(null);
        boolean validAnger=angry!=null&&canTarget(warden,angry);
        if(angry!=null&&!validAnger) warden.clearAnger(angry);
        if(validTarget||validAnger) return;
        warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        warden.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        warden.getNavigation().stop();
    }
    private static void discard(Entity entity) {
        if(entity==null) return;
        entity.getPersistentData().remove(EVENT_ID);
        entity.discard();
    }
    private BloodMoon() {}
}
