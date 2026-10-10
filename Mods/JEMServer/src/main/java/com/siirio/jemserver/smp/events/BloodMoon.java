package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
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

    public static boolean startManual(MinecraftServer server, CompoundTag row) {
        return true;
    }

    private static final int MAX_ACTIVE_SUMMONS=3;

    public static boolean adoptSummon(ServerLevel level,Entity entity) {
        if(!(entity instanceof Mob mob) || !(mob instanceof Enemy)) return true;
        var data=mob.getPersistentData();
        if(data.hasUUID(EVENT_ID) || !MOB_SUMMONED.equals(data.getString(FORGE_SPAWN_TYPE))) return true;
        for(var row:SmpData.get(level.getServer()).all("events")) {
            if(!row.getString("state").equals("ACTIVE") || !row.getString("activity").equals("BLOOD_MOON")
                    || !row.getBoolean("combatStarted") || !EventRegions.contains(row,level,mob.blockPosition())) continue;
            Entity owner=EventSession.owner(mob);
            if(owner instanceof net.minecraft.server.level.ServerPlayer player&&!new EventSession(row).accepted(player.getUUID())) return true;
            long active=row.getList("mobs",Tag.TAG_STRING).stream().map(Tag::getAsString).map(UUID::fromString).map(level::getEntity)
                    .filter(candidate->candidate instanceof Mob living&&living.isAlive()&&living.getPersistentData().getBoolean(SUMMONED)).count();
            if(active>=MAX_ACTIVE_SUMMONS) return false;
            new EventSession(row).mark(mob);
            data.putUUID(EVENT_ID,row.getUUID("id"));
            data.putUUID(BloodMoonLoot.ATTEMPT_ID,BloodMoonLoot.attempt(row));
            data.putBoolean(SUMMONED,true);
            data.putInt(WAVE,row.getInt("wave"));
            EventMobModifiers.applyBloodMoonSummon(mob);
            var mobs=row.getList("mobs",Tag.TAG_STRING);
            if(!mobs.contains(StringTag.valueOf(mob.getStringUUID()))) mobs.add(StringTag.valueOf(mob.getStringUUID()));
            row.put("mobs",mobs);
            SmpData.get(level.getServer()).changed(row);
            return true;
        }
        return true;
    }

    public static void tick(MinecraftServer server, CompoundTag row) {
        var level=EventRegions.level(server,row);
        if(level==null) return;
        var encounter=new EncounterContext(row);
        if(!encounter.tickStartup(server)) { SmpData.get(server).changed(row); return; }
        if(!row.getBoolean("combatStarted")) {
            if(!encounter.allParticipantsInside(server)) return;
            row.putBoolean("combatStarted",true);
            row.putLong("nextWave",System.currentTimeMillis());
            EventRegions.ejectOutsiders(level,row);
            SmpData.get(server).changed(row);
        }
        if(BloodMoonVoting.active(row)) { BloodMoonVoting.tick(server,row); return; }
        var active=new EventSession(row).active(server);
        var mobs=row.getList("mobs",Tag.TAG_STRING);
        if(active.isEmpty()) {
            for(Tag tag:mobs) {
                var entity=level.getEntity(UUID.fromString(tag.getAsString()));
                if(entity instanceof Mob mob) clearInvalidTarget(mob,List.of());
            }
            return;
        }
        var targets=new HashMap<UUID,Integer>();
        active.forEach(player -> targets.put(player.getUUID(),0));
        var living=new EnumMap<BloodMoonWaves.Category,Integer>(BloodMoonWaves.Category.class);
        boolean dirty=false;
        for(int index=mobs.size()-1;index>=0;index--) {
            var entity=level.getEntity(UUID.fromString(mobs.getString(index)));
            if(!(entity instanceof Mob mob) || !mob.isAlive()) {
                mobs.remove(index);
                row.putInt("waveKilled",Math.min(row.getInt("waveTotal"),row.getInt("waveKilled")+1));
                dirty=true;
                continue;
            }
            clearInvalidTarget(mob,active);
            var name=mob.getPersistentData().getString(CATEGORY);
            if(!name.isEmpty()) living.merge(BloodMoonWaves.Category.valueOf(name),1,Integer::sum);
            if(mob.getTarget() instanceof net.minecraft.server.level.ServerPlayer player && active.contains(player))
                targets.merge(player.getUUID(),1,Integer::sum);
            else {
                var target=target(active,targets,mob);
                mob.setTarget(target);
                targets.merge(target.getUUID(),1,Integer::sum);
            }
        }
        int wave=row.getInt("wave");
        dirty|=reconcileWaveThree(row,mobs,wave);
        int recovered=BloodMoonSetPieces.recoverMissingSpawn(level,row,active,wave,row.getInt("worldTier"));
        if(recovered>0) {
            row.putInt("waveTotal",row.getInt("waveTotal")+recovered);
            row.putInt("waveSpawned",row.getInt("waveSpawned")+recovered);
            dirty=true;
        }
        if(wave>0&&remaining(row)==0&&mobs.isEmpty()) {
            BloodMoonSetPieces.finishWave(level,row,wave);
            if(row.getInt("rewardedWave")<wave) {
                for(var player:active) {
                    var member=SmpRecords.members(row).getCompound(player.getStringUUID());
                    member.putInt("waves",member.getInt("waves")+1);
                }
                BloodMoonRewards.stage(level,row,active,wave);
                row.putInt("rewardedWave",wave);
            }
            if(wave==BloodMoonWaves.WAVES) { complete(server,row); return; }
            if(row.getInt("continuedWave")<wave) {
                BloodMoonVoting.open(server,row,wave);
                return;
            }
        }
        if(wave==0 || row.getInt("continuedWave")==wave) {
            if(System.currentTimeMillis()<row.getLong("nextWave")) return;
            wave++;
            int tier=WorldTierApi.currentTier(server);
            row.putInt("wave",wave);row.putInt("worldTier",tier);
            recordWavePerformance(level,row);
            EventMobModifiers.resetWave(row);
            int setPieces=BloodMoonSetPieces.beginWave(level,row,active,wave,tier);
            int ordinaryTotal=BloodMoonWaves.ordinaryTotal(tier,wave,active.size(),setPieces);
            row.putInt("waveTotal",ordinaryTotal);
            row.putInt("waveSpawned",0);row.putInt("waveKilled",0);
            row.putInt("batchSpawned",0);
            row.putInt("batchNumber",1);
            row.putInt("batchTarget",BloodMoonWaves.batchTarget(wave,active.size(),ordinaryTotal));
            row.remove(SPAWN_FAILURES);
            if(wave==3) row.putBoolean(SINGLE_BATCH_WAVE_THREE,true);
            else row.remove(SINGLE_BATCH_WAVE_THREE);
            var remaining=new CompoundTag();
            var composition=ordinaryTotal==0?Map.<BloodMoonWaves.Category,Integer>of():BloodMoonWaves.composition(wave,ordinaryTotal);
            int fallback=0;
            for(var entry:composition.entrySet()) {
                if(entry.getValue()>0 && roster(entry.getKey(),wave,tier).isEmpty()) fallback+=entry.getValue();
                else remaining.putInt(entry.getKey().name(),entry.getValue());
            }
            remaining.putInt(BloodMoonWaves.Category.HORDE.name(),remaining.getInt(BloodMoonWaves.Category.HORDE.name())+fallback);
            row.put("remaining",remaining);
            row.putInt("waveTotal",ordinaryTotal+setPieces);
            row.putInt("waveSpawned",setPieces);
            dirty=true;
        }
        var remaining=row.getCompound("remaining");
        if(mobs.isEmpty()&&row.getInt("batchSpawned")>=row.getInt("batchTarget")&&remaining(row)>0) {
            row.putInt("batchSpawned",0);
            row.putInt("batchNumber",row.getInt("batchNumber")+1);
            row.putInt("batchTarget",BloodMoonWaves.batchTarget(wave,active.size(),remaining(row)));
        }
        int budget=Math.min(EventRules.SPAWN_BATCH.get(),Math.max(0,row.getInt("batchTarget")-row.getInt("batchSpawned")));
        budget=(int)Math.ceil(budget*BloodMoonSetPieces.ordinarySpawnPressure(level,row));
        for(int i=0;i<budget;i++) {
            var available=new ArrayList<BloodMoonWaves.Category>();
            for(var kind:BloodMoonWaves.Category.values()) {
                if(remaining.getInt(kind.name())<=0) continue;
                available.add(kind);
            }
            if(available.isEmpty()) break;
            var kind=available.get(level.random.nextInt(available.size()));
            int failures=row.getCompound(SPAWN_FAILURES).getInt(kind.name());
            Mob mob=spawn(level,row,kind,wave,row.getInt("worldTier"),active,failures>=FALLBACK_AFTER_FAILURES);
            if(mob==null) {
                failedSpawn(row,remaining,kind);
                continue;
            }
            var target=target(active,targets,mob);
            mob.setTarget(target);
            var mobId=StringTag.valueOf(mob.getStringUUID());
            mobs.add(mobId);
            row.put("mobs",mobs);
            if(!mob.isAddedToWorld() && !level.addFreshEntity(mob)) {
                mobs.remove(mobId);
                failedSpawn(row,remaining,kind);
                continue;
            }
            row.getCompound(SPAWN_FAILURES).remove(kind.name());
            BloodMoonSetPieces.prepareSpawn(row,mob,mob.getPersistentData().getBoolean("jem:event_elite"));
            targets.merge(target.getUUID(),1,Integer::sum);
            living.merge(kind,1,Integer::sum);
            remaining.putInt(kind.name(),remaining.getInt(kind.name())-1);
            row.putInt("waveSpawned",row.getInt("waveSpawned")+1);
            row.putInt("batchSpawned",row.getInt("batchSpawned")+1);
            dirty=true;
        }
        if(dirty) {
            row.put("mobs",mobs);
            SmpData.get(server).changed(row);
        }
    }

    private static boolean reconcileWaveThree(CompoundTag row,ListTag mobs,int wave) {
        if(wave!=3||row.getBoolean(SINGLE_BATCH_WAVE_THREE)) return false;
        row.putBoolean(SINGLE_BATCH_WAVE_THREE,true);
        row.put("remaining",new CompoundTag());
        int total=row.getInt("waveKilled")+mobs.size();
        row.putInt("waveTotal",total);
        row.putInt("waveSpawned",total);
        row.putInt("batchSpawned",mobs.size());
        row.putInt("batchTarget",mobs.size());
        return true;
    }

    private static void recordWavePerformance(ServerLevel level,CompoundTag row) {
        double damage=0;
        for(String id:SmpRecords.members(row).getAllKeys()) damage+=SmpRecords.members(row).getCompound(id).getDouble("damage");
        long now=level.getGameTime();
        long started=row.getLong("waveStartedTick");
        if(started>0&&now>started) {
            double observed=(damage-row.getDouble("waveDamageStart"))/((now-started)/20D);
            if(Double.isFinite(observed)&&observed>0) row.putDouble("observedPartyDps",observed);
        }
        row.putDouble("waveDamageStart",damage);
        row.putLong("waveStartedTick",now);
    }

    private static int remaining(CompoundTag row) {
        CompoundTag remaining=row.getCompound("remaining");
        int total=0;
        for(var category:BloodMoonWaves.Category.values()) total+=Math.max(0,remaining.getInt(category.name()));
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

    private static Mob spawn(ServerLevel level,CompoundTag row,BloodMoonWaves.Category kind,int wave,int tier,List<net.minecraft.server.level.ServerPlayer> active,boolean fallback) {
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

    private static Mob createMob(ServerLevel level,CompoundTag row,List<EntityType<?>> roster,BloodMoonWaves.Category kind,int wave,int tier,int x,int z) {
        var column=new BlockPos(x,0,z);
        if(!level.hasChunkAt(column)) return null;
        var pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column);
        if(!level.getFluidState(pos).isEmpty() || !level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)) return null;
        var entity=roster.get(level.random.nextInt(roster.size())).create(level);
        if(!(entity instanceof Mob mob) || WorldTierApi.profile(mob).isPresent()) return null;
        mob.moveTo(x+.5,pos.getY(),z+.5,0,0);
        if(!level.noCollision(mob)) return null;
        new EventSession(row).mark(mob);
        mob.getPersistentData().putUUID(EVENT_ID,row.getUUID("id"));
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

    private static void failedSpawn(CompoundTag row,CompoundTag remaining,BloodMoonWaves.Category kind) {
        var failures=row.getCompound(SPAWN_FAILURES);
        int attempts=failures.getInt(kind.name())+1;
        if(attempts<SKIP_AFTER_FAILURES) {
            failures.putInt(kind.name(),attempts);
            row.put(SPAWN_FAILURES,failures);
            return;
        }
        failures.remove(kind.name());
        row.put(SPAWN_FAILURES,failures);
        remaining.putInt(kind.name(),Math.max(0,remaining.getInt(kind.name())-1));
        row.putInt("waveTotal",Math.max(row.getInt("waveKilled")+row.getList("mobs",Tag.TAG_STRING).size(),row.getInt("waveTotal")-1));
        LOGGER.warn("Skipping unspawnable Blood Moon slot event={} wave={} category={}",row.getUUID("id"),row.getInt("wave"),kind);
    }

    private static List<EntityType<?>> weightedRoster(ServerLevel level,CompoundTag row,BloodMoonWaves.Category kind,int wave,int tier) {
        String fallback=rosterPath(kind,wave,tier);
        String variant=switch(kind) {
            case HORDE -> level.random.nextDouble()<HORDE_FILLER_CHANCE ? "horde/filler" : "horde/wave_"+wave;
            case ARACHNID -> wave==1 || level.random.nextDouble()<ARACHNID_SPIDER_CHANCE ? "arachnid/spiders" : "arachnid/others";
            case RANGED -> level.random.nextDouble()<VANILLA_RANGED_CHANCE ? "ranged/low_weight" : "ranged/primary";
            default -> fallback;
        };
        var selected=new ArrayList<>(types(variant));
        if(selected.isEmpty()) selected.addAll(types(fallback));
        if(wave==2&&row.getInt("batchNumber")>=2&&kind==BloodMoonWaves.Category.HUNTER
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

    private static void complete(MinecraftServer server,CompoundTag row) {
        BloodMoonRewards.commit(server,row,true);
        finishSuccess(server,row);
    }

    public static void cashOut(MinecraftServer server, CompoundTag row) {
        BloodMoonRewards.commit(server,row,false);
        finishSuccess(server,row);
    }

    private static void finishSuccess(MinecraftServer server, CompoundTag row) {
        for(var player:new EventSession(row).active(server)) if(SmpRecords.members(row).getCompound(player.getStringUUID()).getBoolean("contributed")) {
            Profiles.count(server,player.getUUID(),"bloodMoonClears");
            Profiles.countEvent(server,player.getUUID(),"BLOOD_MOON");
        }
        CombatNetwork.completedBloodMoon(server,row);
        EventScheduler.finish(server,row,"COMPLETED");
    }

    public static void fail(MinecraftServer server, CompoundTag row) {
        if(row.getString("state").equals("ACTIVE")) EventScheduler.failAttempt(server,row);
    }

    public static void defeated(MinecraftServer server,LivingEntity entity) {
        if(!entity.getPersistentData().hasUUID(EVENT_ID)) return;
        var row=SmpData.get(server).find("events",entity.getPersistentData().getUUID(EVENT_ID));
        if(row==null || !row.getString("state").equals("ACTIVE")) return;
        var mobs=row.getList("mobs",Tag.TAG_STRING);
        if(mobs.remove(StringTag.valueOf(entity.getStringUUID()))) {
            if(!entity.getPersistentData().getBoolean(SUMMONED)) row.putInt("waveKilled",row.getInt("waveKilled")+1);
            var source=entity.getLastDamageSource();
            var owner=source==null?null:EventSession.owner(source.getEntity());
            if(owner==null && source!=null) owner=EventSession.owner(source.getDirectEntity());
            if(owner instanceof net.minecraft.server.level.ServerPlayer player && new EventSession(row).accepted(player.getUUID())) {
                var member=SmpRecords.members(row).getCompound(player.getStringUUID());
                member.putInt("kills",member.getInt("kills")+1);
            }
        }
        SmpData.get(server).changed(row);
    }
    public static void removed(MinecraftServer server, LivingEntity entity) {
        var reason = entity.getRemovalReason();
        if (reason == null || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK
                || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) return;
        if (reason == Entity.RemovalReason.KILLED) { defeated(server, entity); return; }
        var data = entity.getPersistentData();
        if (!data.hasUUID(EVENT_ID)) return;
        var row = SmpData.get(server).find("events", data.getUUID(EVENT_ID));
        if (row == null || !row.getString("state").equals("ACTIVE")) return;
        var mobs = row.getList("mobs", Tag.TAG_STRING);
        if (!mobs.remove(StringTag.valueOf(entity.getStringUUID()))) return;
        if (data.getBoolean(SUMMONED)) {
            SmpData.get(server).changed(row);
            return;
        }
        String category = data.getString(CATEGORY);
        if(category.isEmpty()) {
            row.putInt("waveKilled",row.getInt("waveKilled")+1);
            SmpData.get(server).changed(row);
            return;
        }
        var remaining = row.getCompound("remaining");
        remaining.putInt(category, remaining.getInt(category) + 1);
        row.put("remaining", remaining);
        row.putInt("waveSpawned", Math.max(0, row.getInt("waveSpawned") - 1));
        SmpData.get(server).changed(row);
    }

    public static void cleanup(MinecraftServer server,CompoundTag row) {
        BloodMoonSetPieces.cleanup(server,row);
        var level=EventRegions.level(server,row);
        if(level!=null) {
            for(Tag tag:row.getList("mobs",Tag.TAG_STRING)) {
                var entity=level.getEntity(UUID.fromString(tag.getAsString()));
                discard(entity);
            }
            var bounds=new net.minecraft.world.phys.AABB(EventRegions.minX(row),level.getMinBuildHeight(),EventRegions.minZ(row),
                    EventRegions.maxX(row)+1,level.getMaxBuildHeight(),EventRegions.maxZ(row)+1);
            for(var entity:level.getEntities((Entity)null,bounds,candidate->candidate.getPersistentData().hasUUID(EVENT_ID)
                    && candidate.getPersistentData().getUUID(EVENT_ID).equals(row.getUUID("id")))) discard(entity);
        }
        row.remove("mobs");
    }
    public static boolean belongs(CompoundTag row,Entity entity) {
        var data=entity.getPersistentData();
        return data.hasUUID(EVENT_ID)&&data.getUUID(EVENT_ID).equals(row.getUUID("id"));
    }
    public static boolean tracked(CompoundTag row,Entity entity) {
        return row.getList("mobs",Tag.TAG_STRING).contains(StringTag.valueOf(entity.getStringUUID()));
    }
    public static boolean canTarget(Entity eventMob,Entity target) {
        var data=eventMob.getPersistentData();
        if(!data.hasUUID(EVENT_ID)||eventMob.getServer()==null) return true;
        var row=SmpData.get(eventMob.getServer()).find("events",data.getUUID(EVENT_ID));
        if(row==null||!row.getString("state").equals("ACTIVE")||!row.getString("activity").equals("BLOOD_MOON")) return true;
        return target instanceof net.minecraft.server.level.ServerPlayer player
                &&new EventSession(row).accepted(player.getUUID())
                &&player.isAlive()&&!player.isSpectator()
                &&EventRegions.contains(row,player.serverLevel(),player.blockPosition());
    }
    private static void clearInvalidTarget(Mob mob,List<net.minecraft.server.level.ServerPlayer> active) {
        boolean validTarget=mob.getTarget()!=null&&active.contains(mob.getTarget());
        if(mob.getTarget()!=null&&!validTarget) mob.setTarget(null);
        if(!(mob instanceof Warden warden)) return;
        var angry=warden.getEntityAngryAt().orElse(null);
        if(validTarget||angry!=null&&canTarget(warden,angry)) return;
        if(angry!=null) warden.clearAnger(angry);
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
