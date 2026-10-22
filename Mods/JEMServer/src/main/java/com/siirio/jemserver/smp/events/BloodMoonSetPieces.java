package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.SmpData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BloodMoonSetPieces {
    private static final String SET_PIECE="jem:setpiece";
    private static final String SET_PIECE_BOSS="jem:setpiece_boss";
    private static final String DORMANT="jem:setpiece_dormant";
    private static final String TWIN="jem:twin_creeper";
    private static final String TWIN_DETONATIONS="jem:twin_detonations";
    private static final String EXPOSED_UNTIL="jem:setpiece_exposed_until";
    private static final String ATTACK="setPieceAttack";
    private static final int NORMAL_SPAWN_TELEGRAPH_TICKS=8;
    private static final int LARGE_SPAWN_TELEGRAPH_TICKS=20;
    private static final int FLOOR_BLOCKS_PER_TICK=12;
    private static final int COMBO_ATTACK_COUNT=3;
    private static final int COMBO_TELEGRAPH_TICKS=32;
    private static final int COMBO_IMPACT_TICKS=12;
    private static final int ATTACK_GAP_TICKS=60;
    private static final int COMBO_GAP_TICKS=ATTACK_GAP_TICKS;
    private static final int COMBO_MIN_INTERVAL_TICKS=400;
    private static final int COMBO_INTERVAL_VARIANCE_TICKS=201;
    private static final double ATTACK_TELEGRAPH_SLOWDOWN=1.60;
    private static final double ATTACK_COOLDOWN_SCALE=.32;
    private static final int FINAL_WARNING_TICKS=20;
    private static final int ATTACK_TELEGRAPH_COLOR=0x26D9D0;
    private static final double FLOOR_ORDER_JITTER=.55;
    private static final double FLOOR_CENTER_RADIUS=.25;
    private static final double FLOOR_MIDDLE_RADIUS=.65;
    private static final float TWIN_CREEPER_HEIGHT=15;
    private static final float LARGE_CREEPER_SCALE=TWIN_CREEPER_HEIGHT/EntityType.CREEPER.getHeight();
    private static final float MAX_BOSS_HEIGHT=20;
    private static final float TWIN_EXPLOSION_POWER=3;
    private static final TagKey<EntityType<?>> CAMPAIGN_BOSSES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("jem_server","blood_moon/campaign_bosses"));
    private static final UUID HEALTH_SCALE=UUID.nameUUIDFromBytes("jem:setpiece:health".getBytes(StandardCharsets.UTF_8));
    private static final Set<Scenario> WAVE_THREE_SCENARIOS=EnumSet.of(Scenario.TWIN_CREEPERS,Scenario.BREACH_BROTHERS,Scenario.RED_KNIGHTS,Scenario.MAGNETIC_ASSEMBLY,Scenario.RED_HUNT,Scenario.TREMOR);
    private static final Set<Scenario> WAVE_FIVE_SCENARIOS=EnumSet.of(Scenario.ANCIENT_WARDEN,Scenario.CRITICAL_MASS,Scenario.IRON_HEART,Scenario.CARNIVOROUS_GARDEN,Scenario.LAST_SENTINEL,Scenario.BONE_CATHEDRAL);

    public static List<String> waveThreeSelections() {
        return selections(WAVE_THREE_SCENARIOS);
    }

    public static List<String> waveFiveSelections() {
        return selections(WAVE_FIVE_SCENARIOS);
    }

    public static boolean select(EventState row,String waveThree,String waveFive) {
        Scenario third=scenario(waveThree.toUpperCase(Locale.ROOT));
        Scenario fifth=scenario(waveFive.toUpperCase(Locale.ROOT));
        if(!WAVE_THREE_SCENARIOS.contains(third)||!WAVE_FIVE_SCENARIOS.contains(fifth)) return false;
        row.wave3SetPiece(third.name());
        row.wave5SetPiece(fifth.name());
        return true;
    }

    public static int beginWave(ServerLevel level,EventState row,List<ServerPlayer> players,int wave,int tier) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return beginWaveInScope(level, row, players, wave, tier); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> beginWaveInScope(level, store.events().bind(row), players, wave, tier));
    }

    private static int beginWaveInScope(ServerLevel level,EventState row,List<ServerPlayer> players,int wave,int tier) {
        Set<Scenario> pool=wave==3?WAVE_THREE_SCENARIOS:wave==BloodMoonWaves.WAVES?WAVE_FIVE_SCENARIOS:Set.of();
        if(pool.isEmpty()) return 0;
        row.editSetPiece().mobs(List.of());
        row.editSetPiece().bosses(List.of());
        row.editSetPiece().clearAttack();
        row.editSetPiece().clearImpact();
        row.editSetPiece().clearAttackStart();
        row.editSetPiece().clearRecoveryUntil();
        row.editSetPiece().clearAttackPhase();
        row.editSetPiece().clearPhaseUntil();
        row.editSetPiece().clearLastAttack();
        row.editSetPiece().clearPreviousAttack();
        row.editSetPiece().clearFinalUsed();
        row.editSetPiece().clearCongregation();
        row.editSetPiece().clearFloorQueue();
        row.editSetPiece().clearFloorIndex();
        resetCombo(row);
        BossDebuffController.reset(row);
        Scenario selected=scenario(wave==3?row.wave3SetPiece():row.wave5SetPiece());
        if(selected==null||!pool.contains(selected)||!compatible(selected)) {
            var compatible=pool.stream().filter(BloodMoonSetPieces::compatible).toList();
            if(compatible.isEmpty()) return 0;
            selected=compatible.get(level.random.nextInt(compatible.size()));
            if(wave==3) row.wave3SetPiece(selected.name()); else row.wave5SetPiece(selected.name());
        }
        buildArena(level,row,selected,wave);
        int count=spawnScenario(level,row,players,selected,tier,wave);
        if(count>0) markSpawned(row,wave);
        double initial=livingBosses(level,row).stream().mapToDouble(LivingEntity::getMaxHealth).sum();
        row.editSetPiece().initialHealth(Math.max(1,initial));
        initializeCooldowns(level,row,selected);

        return count;
    }

    public static int recoverMissingSpawn(ServerLevel level,EventState row,List<ServerPlayer> players,int wave,int tier) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return recoverMissingSpawnInScope(level, row, players, wave, tier); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> recoverMissingSpawnInScope(level, store.events().bind(row), players, wave, tier));
    }

    private static int recoverMissingSpawnInScope(ServerLevel level,EventState row,List<ServerPlayer> players,int wave,int tier) {
        if(wave!=3&&wave!=BloodMoonWaves.WAVES) return 0;
        if(wave==3?row.wave3SetPieceSpawned():row.wave5SetPieceSpawned()) return 0;
        if(!livingSetPieces(level,row).isEmpty()) {
            markSpawned(row,wave);

            return 0;
        }
        Scenario selected=current(row);
        if(selected==null) return 0;
        int count=spawnScenario(level,row,players,selected,tier,wave);
        if(count<=0) return 0;
        markSpawned(row,wave);
        row.editSetPiece().initialHealth(Math.max(1,livingBosses(level,row).stream().mapToDouble(LivingEntity::getMaxHealth).sum()));
        initializeCooldowns(level,row,selected);

        return count;
    }

    public static void prepareSpawn(EventState row,Mob mob,boolean large) {
        int duration=large?LARGE_SPAWN_TELEGRAPH_TICKS:NORMAL_SPAWN_TELEGRAPH_TICKS;
        var position=mob.blockPosition();
        new EventSession(row).active(mob.getServer()).forEach(player->EventNetwork.beam(player,position,duration,mob.getBbWidth()));
    }

    public static double ordinarySpawnPressure(ServerLevel level,EventState row) {
        return 1;
    }

    public static void finishWave(ServerLevel level,EventState row,int wave) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishWaveInScope(level, row, wave); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishWaveInScope(level, store.events().bind(row), wave));
    }

    private static void finishWaveInScope(ServerLevel level,EventState row,int wave) {
        if((wave==3||wave==BloodMoonWaves.WAVES)&&row.setPiece().arenaWave()==wave) {
            row.editSetPiece().restoring(true);
            resumeRestoration(level,row);

        }
    }

    public static boolean survivingTwin(Creeper creeper) {
        if (!EventRuntime.enabled(creeper.getServer())) return false;
        var data=creeper.getPersistentData();
        if(!data.getBoolean(TWIN)) return false;
        int detonations=data.getInt(TWIN_DETONATIONS);
        if(detonations>=8) return false;
        data.putInt(TWIN_DETONATIONS,detonations+1);
        return true;
    }

    public static float twinExplosionPower() {
        return TWIN_EXPLOSION_POWER;
    }

    public static double twinExplosionY(Creeper creeper) {
        return creeper.getY()+creeper.getBbHeight()*.35;
    }

    public static void placed(ServerLevel level,BlockEvent.EntityPlaceEvent event) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { placedInScope(level, event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> placedInScope(level, event));
    }

    private static void placedInScope(ServerLevel level,BlockEvent.EntityPlaceEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        EventState row=EventSession.forPlayer(player);
        if(row==null||!EventRegions.contains(row,level,event.getPos())||!"RED_KNIGHTS".equals(row.wave3SetPiece())) return;
        var cover=new java.util.LinkedHashMap<>(row.bloodMoonCover());
        if(event instanceof BlockEvent.EntityMultiPlaceEvent multiple)
            multiple.getReplacedBlockSnapshots().forEach(snapshot->cover.put(snapshot.getPos().asLong(),true));
        else cover.put(event.getPos().asLong(),true);
        row.bloodMoonCover(cover);

    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        var serverForMutation = event.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { tickInScope(event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> tickInScope(event));
    }

    private static void tickInScope(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        MinecraftServer server=event.getServer();
        for(var row:SmpData.get(server).events().currentRows()) {
            ServerLevel level=EventRegions.level(server,row);
            if(level==null) continue;
            if(row.setPiece().restoring()&&(server.getTickCount()%20)==0) {
                resumeRestoration(level,row);

            }
            if(!EventRuntime.enabled(server)||!row.state().equals("ACTIVE")||!row.activity().equals("BLOOD_MOON")) continue;
            Scenario selected=current(row);
            if(selected==null) continue;
            tickMechanics(level,row,selected);
        }
    }

    @SubscribeEvent
    public static void exposedDamage(LivingHurtEvent event) {
        if (!EventRuntime.enabled(event.getEntity().getServer())) return;
        if(event.getEntity().level().isClientSide||event.getEntity().getPersistentData().getLong(EXPOSED_UNTIL)<event.getEntity().level().getGameTime()) return;
        event.setAmount(event.getAmount()*1.5F);
    }

    public static void cleanup(MinecraftServer server,EventState row) {
        ServerLevel level=EventRegions.level(server,row);
        if(level!=null) {
            livingBosses(level,row).forEach(com.siirio.jemworldbosstiers.api.BossEffectApi::clear);
            row.editSetPiece().restoring(true);
            resumeRestoration(level,row);
        }
        if(!pendingRestoration(row)) {
            row.clearWave3SetPiece();
            row.clearWave5SetPiece();
            row.editSetPiece().clearRestoring();
        }
        row.editSetPiece().clearMobs();
        row.editSetPiece().clearBosses();
        row.editSetPiece().clearAttack();
        row.editSetPiece().clearImpact();
        row.editSetPiece().clearCooldowns();
        BossDebuffController.reset(row);
        row.editSetPiece().clearInitialHealth();
        row.editSetPiece().clearLastAttack();
        row.editSetPiece().clearAttackStart();
        row.editSetPiece().clearRecoveryUntil();
        row.editSetPiece().clearAttackPhase();
        row.editSetPiece().clearPhaseUntil();
        row.clearBloodMoonCover();
        row.editSetPiece().clearFloorQueue();
        row.editSetPiece().clearFloorIndex();
        resetCombo(row);
        row.editSetPiece().clearOriginX();
        row.editSetPiece().clearOriginZ();
        row.editSetPiece().clearSecondX();
        row.editSetPiece().clearSecondZ();
        row.editSetPiece().clearTargetX();
        row.editSetPiece().clearTargetZ();
        row.editSetPiece().clearFinalUsed();
        row.editSetPiece().clearCongregation();
    }

    private static int spawnScenario(ServerLevel level,EventState row,List<ServerPlayer> players,Scenario scenario,int tier,int wave) {
        List<Spawn> spawns=spawns(scenario);
        int count=0;
        for(int index=0;index<spawns.size();index++) {
            Spawn spawn=spawns.get(index);
            BlockPos position=spawnPosition(level,row,scenario,index,spawns.size());
            Mob mob=create(level,spawn.id(),position,scenario);
            if(mob==null) continue;
            new EventSession(row).mark(mob);
            mob.getPersistentData().putUUID(BloodMoon.EVENT_ID,row.id());
            mob.getPersistentData().putUUID(BloodMoonLoot.ATTEMPT_ID,BloodMoonLoot.attempt(row));
            mob.getPersistentData().putString(SET_PIECE,scenario.name());
            mob.getPersistentData().putBoolean(SET_PIECE_BOSS,spawn.boss());
            mob.getPersistentData().putBoolean(DORMANT,spawn.dormant());
            if(!(mob instanceof Warden&&scenario==Scenario.ANCIENT_WARDEN)) mob.finalizeSpawn(level,level.getCurrentDifficultyAt(position),MobSpawnType.EVENT,null,null);
            if(spawn.dormant()) {mob.setNoAi(true);mob.setInvulnerable(true);}
            if(scenario==Scenario.TWIN_CREEPERS) mob.getPersistentData().putBoolean(TWIN,true);
            float scale=spawn.scale();
            if(spawn.boss()) scale=Math.min(scale,MAX_BOSS_HEIGHT/Math.max(.01F,mob.getBbHeight()));
            EventMobScale.apply(mob,scale);
            EventMobModifiers.applyBloodMoonSetPiece(mob,tier,wave,spawn.boss());
            EventMobModifiers.applyMovementProfile(mob,scale,spawn.boss());
            scaleHealth(mob,healthFactor(row,wave,mob,spawn,spawns));
            mob.setPersistenceRequired();
            mob.setHealth(mob.getMaxHealth());
            row.mobs(append(row.mobs(),mob.getUUID()));
            if(!level.addFreshEntity(mob)) {
                row.mobs(without(row.mobs(),mob.getUUID()));
                continue;
            }
            if(spawn.boss()) {
                com.siirio.jemworldbosstiers.api.BossEffectApi.begin(mob);
                BossDebuffController.initializeBoss(level,row,mob,players);
                SpawnPresentation.begin(level,mob,presentation(scenario),presentationDuration(scenario));
            } else {
                ServerPlayer target=players.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                if(target!=null) mob.setTarget(target);
            }
            row.editSetPiece().mobs(append(row.setPiece().mobs(),mob.getUUID()));
            if(spawn.boss()) row.editSetPiece().bosses(append(row.setPiece().bosses(),mob.getUUID()));
            count++;
        }
        return count;
    }

    private static Mob create(ServerLevel level,String id,BlockPos position,Scenario scenario) {
        EntityType<?> type=EntityType.byString(id).orElse(null);
        Entity entity=type==null?null:type.create(level);
        if(!(entity instanceof Mob mob)) return null;
        mob.moveTo(position.getX()+.5,position.getY(),position.getZ()+.5,0,0);
        return mob;
    }

    private static void markSpawned(EventState row,int wave) {
        if(wave==3) row.wave3SetPieceSpawned(true); else row.wave5SetPieceSpawned(true);
    }

    private static <T> List<T> append(List<T> values,T value) {
        var result=new ArrayList<>(values);
        result.add(value);
        return result;
    }

    private static <T> List<T> without(List<T> values,T value) {
        var result=new ArrayList<>(values);
        result.remove(value);
        return result;
    }

    private static void scaleHealth(Mob mob,double factor) {
        var health=mob.getAttribute(Attributes.MAX_HEALTH);
        if(health==null) return;
        health.removeModifier(HEALTH_SCALE);
        health.addPermanentModifier(new AttributeModifier(HEALTH_SCALE,"JEM set-piece health",factor-1,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static double healthFactor(EventState row,int wave,Mob mob,Spawn spawn,List<Spawn> spawns) {
        double dps=row.observedPartyDps();
        int bosses=(int)spawns.stream().filter(Spawn::boss).count();
        int allies=spawns.size()-bosses;
        if(dps<=0) return spawn.boss()?(bosses==1?2.5:1.8):.75;
        double duration=wave==BloodMoonWaves.WAVES?120:85;
        double share=spawn.boss()?.8/Math.max(1,bosses):.2/Math.max(1,allies);
        double reference=Math.max(1,mob.getMaxHealth());
        double factor=dps*duration*share/reference;
        return spawn.boss()?Mth.clamp(factor,wave==BloodMoonWaves.WAVES?2:1.5,wave==BloodMoonWaves.WAVES?8:6):Mth.clamp(factor,.5,1.5);
    }

    private static BlockPos spawnPosition(ServerLevel level,EventState row,Scenario scenario,int index,int total) {
        int centerX=(EventRegions.minX(row)+EventRegions.maxX(row))/2;
        int centerZ=(EventRegions.minZ(row)+EventRegions.maxZ(row))/2;
        int halfX=Math.max(8,(EventRegions.maxX(row)-EventRegions.minX(row))/2-8);
        int halfZ=Math.max(8,(EventRegions.maxZ(row)-EventRegions.minZ(row))/2-8);
        if(scenario.single()&&index==0) return surface(level,centerX,centerZ);
        double angle=Math.PI*2*index/Math.max(1,total);
        int x=centerX+(int)Math.round(Math.cos(angle)*Math.min(halfX,18));
        int z=centerZ+(int)Math.round(Math.sin(angle)*Math.min(halfZ,18));
        if(scenario==Scenario.TWIN_CREEPERS) {
            x=centerX+(index==0?halfX:-halfX);
            z=centerZ+(index==0?-halfZ:halfZ);
        }
        return surface(level,x,z);
    }

    private static BlockPos surface(ServerLevel level,int x,int z) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));
    }

    private static void power(ServerLevel level,Creeper creeper) {
        LightningBolt lightning=EntityType.LIGHTNING_BOLT.create(level);
        if(lightning==null) return;
        lightning.moveTo(creeper.position());
        lightning.setVisualOnly(true);
        level.addFreshEntity(lightning);
        creeper.thunderHit(level,lightning);
    }

    private static void tickMechanics(ServerLevel level,EventState row,Scenario scenario) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { tickMechanicsInScope(level, row, scenario); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> tickMechanicsInScope(level, store.events().bind(row), scenario));
    }

    private static void tickMechanicsInScope(ServerLevel level,EventState row,Scenario scenario) {
        List<Mob> bosses=livingBosses(level,row);
        advanceArena(level,row,scenario,bosses);
        advanceIntroductions(level,row,bosses);
        BossDebuffController.tick(level,row,bosses);
        List<ServerPlayer> participants=new EventSession(row).active(level.getServer());
        bosses.forEach(boss->com.siirio.jemworldbosstiers.api.BossEffectApi.tick(boss,participants));
        if(scenario==Scenario.BONE_CATHEDRAL) activateCongregation(level,row);
        long now=level.getGameTime();
        if(row.setPiece().hasAttackBlocksUntil()&&row.setPiece().attackBlocksUntil()<=now) {
            restoreAttackBlocks(level,row);

        }
        if(bosses.isEmpty()) return;
        if(bosses.stream().anyMatch(BloodMoonSetPieces::introducing)) return;
        if(!row.setPiece().attack().isEmpty()) {
            Attack attack=Attack.valueOf(row.setPiece().attack());
            long impact=row.setPiece().impact();
            long windupEnd=row.setPiece().phaseUntil();
            String phase=row.setPiece().attackPhase();
            if(phase.equals("WINDUP")&&now>=windupEnd) {
                row.editSetPiece().attackPhase("TELEGRAPH");
                row.editSetPiece().phaseUntil(impact-finalTelegraphLead(row,attack));
            } else if(phase.equals("TELEGRAPH")&&now>=windupEnd) row.editSetPiece().attackPhase("LOCKED_PATTERN");
            if(phase.equals("WINDUP")) windup(level,attack,bosses);
            else if((now&1)==0) telegraph(level,row,attack,impact-now,bosses);
            if(now>=impact) execute(level,row,scenario,attack,bosses);
            return;
        }
        if(now<row.setPiece().recoveryUntil()) return;
        double ratio=healthRatio(level,row);
        if(row.setPiece().comboActive()) {
            if(startNextCombo(level,row,scenario,bosses)) return;
            finishCombo(level,row);
        } else if(now>=row.setPiece().comboNext()&&beginCombo(level,row,scenario,bosses,ratio)) {
            startNextCombo(level,row,scenario,bosses);
            return;
        }
        List<AttackSpec> available=attacks(scenario).stream()
                .filter(spec->requirements(row,spec.attack(),bosses,ratio))
                .filter(spec->row.setPiece().cooldowns().getOrDefault(spec.attack().name(),0L)<=now)
                .toList();
        if(available.isEmpty()) return;
        AttackSpec chosen=weighted(level,available,row.setPiece().lastAttack(),row.setPiece().previousAttack());
        start(level,row,scenario,chosen.attack(),bosses,false);
    }

    private static void start(ServerLevel level,EventState row,Scenario scenario,Attack attack,List<Mob> bosses,boolean combo) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { startInScope(level, row, scenario, attack, bosses, combo); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> startInScope(level, store.events().bind(row), scenario, attack, bosses, combo));
    }

    private static void startInScope(ServerLevel level,EventState row,Scenario scenario,Attack attack,List<Mob> bosses,boolean combo) {
        long telegraph=combo?COMBO_TELEGRAPH_TICKS:telegraphTicks(attack);
        row.editSetPiece().attack(attack.name());
        row.editSetPiece().attackStart(level.getGameTime());
        row.editSetPiece().impact(level.getGameTime()+telegraph);
        row.editSetPiece().attackPhase("WINDUP");
        row.editSetPiece().phaseUntil(level.getGameTime()+(combo?Math.max(4,telegraph/4):Math.max(10,telegraph/4)));
        stageGeometry(level,row,attack,bosses);
        bosses.forEach(mob->mob.getNavigation().stop());
        if(attack==Attack.CRIMSON_DIVIDE) new EventSession(row).active(level.getServer()).forEach(player->player.sendSystemMessage(Component.translatable("jem.event.blood_moon.crimson_divide")));
        if(attack==Attack.TWIN_CATACLYSM) bosses.forEach(mob->mob.setInvulnerable(true));
        if(attack==Attack.COLLISION_COURSE) stageCollision(level,row,bosses);
        if(attack==Attack.CROSS_BLAST||attack==Attack.HUNTER_LATTICE) chooseTwinPattern(level,row);
        if(attack==Attack.ANCIENT_ROAR) row.editSetPiece().castParity(row.setPiece().castParity()+1);
        if(attack==Attack.ANCIENT_ROAR&&bosses.get(0) instanceof Warden warden) warden.setPose(Pose.ROARING);
        if(attack==Attack.RADIATION_SWEEP) row.editSetPiece().reverse(level.random.nextBoolean());
        if(attack==Attack.END_COLLAPSE) chooseSafeQuarter(level,row);
        AttackGeometry geometry=geometry(row,attack,bosses,0);
        new EventSession(row).active(level.getServer()).forEach(player->EventNetwork.telegraph(player,row.id(),geometry,(int)telegraph,ATTACK_TELEGRAPH_COLOR));
        play(level,bosses.get(0).blockPosition(),warningSound(attack),SoundEvents.WARDEN_SONIC_CHARGE,1.4F,.85F);

    }

    private static void execute(ServerLevel level,EventState row,Scenario scenario,Attack attack,List<Mob> bosses) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { executeInScope(level, row, scenario, attack, bosses); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> executeInScope(level, store.events().bind(row), scenario, attack, bosses));
    }

    private static void executeInScope(ServerLevel level,EventState row,Scenario scenario,Attack attack,List<Mob> bosses) {
        Mob source=bosses.get(0);
        AttackGeometry geometry=geometry(row,attack,bosses,1);
        boolean combo=row.setPiece().comboActive();
        int attackDuration=combo?COMBO_IMPACT_TICKS:attackVfxTicks(attack);
        new EventSession(row).active(level.getServer()).forEach(player->EventNetwork.attackVfx(player,row.id(),geometry,attackDuration,ATTACK_TELEGRAPH_COLOR));
        physicalImpact(level,row,source,attack,geometry);
        for(ServerPlayer player:new EventSession(row).active(level.getServer())) {
            if(!geometry.contains(player.getX(),player.getZ())) continue;
            if(attack==Attack.CRIMSON_DIVIDE&&cover(level,row,source,player)) continue;
            if(attack==Attack.CRIMSON_DIVIDE) {
                player.stopUsingItem();
                player.getCooldowns().addCooldown(Items.SHIELD,100);
            }
            float damage=EventMobModifiers.signatureDamage(row.worldTier(),row.wave(),damage(attack));
            if(damage>0) EventMobModifiers.signature(source,()->player.hurt(level.damageSources().mobAttack(source),damage));
            applyMotion(geometry,attack,player);
            if(attack==Attack.SCULK_BURIAL) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,4));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,80,0));
            }
        }
        if(attack==Attack.DISASSEMBLE) bosses.forEach(mob->mob.getPersistentData().putLong(EXPOSED_UNTIL,level.getGameTime()+60));
        if(attack==Attack.FALLING_FORGE) bosses.forEach(mob->mob.heal(mob.getMaxHealth()*.1F));
        if(attack==Attack.COLLISION_COURSE) releaseCollision(bosses);
        livingSetPieces(level,row).stream().filter(mob->!mob.getPersistentData().getBoolean(DORMANT)).forEach(mob->mob.setInvulnerable(false));
        if(attack==Attack.TWIN_CATACLYSM) twinImpact(level,bosses);
        if(attack==Attack.ANCIENT_ROAR&&source instanceof Warden warden) warden.setPose(Pose.STANDING);
        if(!row.setPiece().attackTerrain().isEmpty()) row.editSetPiece().attackBlocksUntil(level.getGameTime()+40);
        double ratio=healthRatio(level,row);
        AttackSpec spec=attacks(scenario).stream().filter(value->value.attack()==attack).findFirst().orElse(new AttackSpec(attack,600,390,1));
        var cooldowns=new java.util.LinkedHashMap<>(row.setPiece().cooldowns());
        cooldowns.put(attack.name(),level.getGameTime()+attackCooldown(ratio<.5?spec.aggressive():spec.normal()));
        row.editSetPiece().cooldowns(cooldowns);
        row.editSetPiece().previousAttack(row.setPiece().lastAttack());
        row.editSetPiece().lastAttack(attack.name());
        row.editSetPiece().recoveryUntil(level.getGameTime()+(combo?COMBO_GAP_TICKS:recoveryTicks(attack)));
        row.editSetPiece().attackPhase("RECOVERY");
        if(attack==Attack.FINAL_COUNTDOWN||attack==Attack.LAST_MASS) row.editSetPiece().finalUsed(true);
        row.editSetPiece().clearAttack();
        row.editSetPiece().clearImpact();
        row.editSetPiece().clearPhaseUntil();

    }

    private static void telegraph(ServerLevel level,EventState row,Attack attack,long remaining,List<Mob> bosses) {
        long total=currentTelegraphTicks(row,attack);
        double progress=1-Mth.clamp(remaining/(double)total,0,1);
        AttackGeometry geometry=geometry(row,attack,bosses,progress);
        bosses.forEach(mob->face(mob,geometry.targetX(),geometry.targetZ(),12));
        if(attack==Attack.TENTACLE_CORRIDOR||attack==Attack.SCULK_BURIAL) rupture(level,row,geometry);
        if(remaining==FINAL_WARNING_TICKS||remaining==10||remaining==5) play(level,BlockPos.of(row.position()),warningSound(attack),SoundEvents.NOTE_BLOCK_HAT.value(),1.1F,remaining==5?1.65F:1.25F);
    }

    private static void windup(ServerLevel level,Attack attack,List<Mob> bosses) {
        for(Mob boss:bosses) {
            if((level.getGameTime()&3)==0) level.sendParticles(warningParticle(attack),
                    boss.getX(),boss.getEyeY(),boss.getZ(),3,boss.getBbWidth()*.2,.15,boss.getBbWidth()*.2,.01);
        }
    }

    private static ParticleOptions warningParticle(Attack attack) {
        String id=switch(attack) {
            case DISASSEMBLE,MAGNETIC_GRID,CRUSHER,FALLING_FORGE,MAGNETIC_SAW -> "alexscaves:azure_magnetic_flow";
            case RADIATION_SWEEP,MELTDOWN_GRID,FINAL_COUNTDOWN -> "alexscaves:fallout";
            case VOID_LINES,END_COLLAPSE,VOID_CROSS,CROSSING_SHADOWS,BLINKING_GRID -> "legendary_monsters:soul_sigil";
            case THORN_ROWS,BLOOM,CLOSING_GARDEN -> "skarrier_mobs:flore_spark";
            case SCULK_PULSE,TENTACLE_CORRIDOR,SCULK_BURIAL,ANCIENT_ROAR -> "legendary_monsters:warning";
            default -> "lodestone:star";
        };
        var type=BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.tryParse(id));
        return type instanceof ParticleOptions options?options:attack==Attack.ANCIENT_ROAR?ParticleTypes.SCULK_SOUL:ParticleTypes.CRIT;
    }

    private static String warningSound(Attack attack) {
        return switch(attack) {
            case DISASSEMBLE,MAGNETIC_GRID,CRUSHER,FALLING_FORGE,MAGNETIC_SAW -> "alexscaves:magnetron_attack";
            case RADIATION_SWEEP,MELTDOWN_GRID,FINAL_COUNTDOWN -> "alexscaves:nucleeper_charge";
            case VOID_LINES,END_COLLAPSE,VOID_CROSS,CROSSING_SHADOWS,BLINKING_GRID -> "legendary_monsters:beam_charge";
            case THORN_ROWS,BLOOM,CLOSING_GARDEN -> "skarrier_mobs:devourer_warning1";
            case SCULK_PULSE,TENTACLE_CORRIDOR,SCULK_BURIAL,ANCIENT_ROAR -> "legendary_monsters:ancient_guardian_roar";
            default -> "";
        };
    }

    private static void play(ServerLevel level,BlockPos position,String id,SoundEvent fallback,float volume,float pitch) {
        ResourceLocation key=ResourceLocation.tryParse(id);
        SoundEvent sound=key!=null&&BuiltInRegistries.SOUND_EVENT.containsKey(key)?BuiltInRegistries.SOUND_EVENT.get(key):fallback;
        level.playSound(null,position,sound,SoundSource.HOSTILE,volume,pitch);
    }

    private static void physicalImpact(ServerLevel level,EventState row,Mob source,Attack attack,AttackGeometry geometry) {
        if(attack==Attack.ARROW_HEAVEN||attack==Attack.IMPALING_FORMATION||attack==Attack.BONE_SPEAR_CHOIR||attack==Attack.GRAVEFALL)
            projectiles(level,row,source,attack);
        int emitted=0;
        for(int x=geometry.minX();x<=geometry.maxX()&&emitted<64;x+=2) for(int z=geometry.minZ();z<=geometry.maxZ()&&emitted<64;z+=2) {
            if(!geometry.front(x+.5,z+.5)) continue;
            BlockPos floor=visibleFloor(level,x,z);
            level.sendParticles(physicalParticle(attack),x+.5,floor.getY()+1.05,z+.5,1,.08,.08,.08,.02);
            emitted++;
        }
        if(attack==Attack.TENTACLE_CORRIDOR||attack==Attack.SCULK_BURIAL||attack==Attack.EARTH_SPLIT||attack==Attack.SEISMIC_LINES) rupture(level,row,geometry);
        level.playSound(null,source.blockPosition(),physicalSound(attack),SoundSource.HOSTILE,2F,.65F);
    }

    private static net.minecraft.core.particles.SimpleParticleType physicalParticle(Attack attack) {
        return switch(attack) {
            case TWIN_CATACLYSM,CROSS_BLAST -> ParticleTypes.EXPLOSION;
            case CRIMSON_DIVIDE,TRINITY_ASSAULT,IMPALING_FORMATION,ARROW_HEAVEN,BONE_SPEAR_CHOIR -> ParticleTypes.SWEEP_ATTACK;
            case ANCIENT_ROAR,SCULK_PULSE,SCULK_BURIAL,TENTACLE_CORRIDOR -> ParticleTypes.SCULK_SOUL;
            case RADIATION_SWEEP,MELTDOWN_GRID,FINAL_COUNTDOWN -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.POOF;
        };
    }

    private static net.minecraft.sounds.SoundEvent physicalSound(Attack attack) {
        return switch(attack) {
            case CRIMSON_DIVIDE,TRINITY_ASSAULT -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case IMPALING_FORMATION -> SoundEvents.TRIDENT_THROW;
            case ANCIENT_ROAR,SCULK_PULSE,SCULK_BURIAL,TENTACLE_CORRIDOR -> SoundEvents.WARDEN_SONIC_BOOM;
            default -> SoundEvents.GENERIC_EXPLODE;
        };
    }

    private static long recoveryTicks(Attack attack) {
        return Math.max(ATTACK_GAP_TICKS,attackCooldown(attack==Attack.FINAL_COUNTDOWN||attack==Attack.LAST_MASS?60:40));
    }

    private static int attackVfxTicks(Attack attack) {
        return switch(attack) {
            case TWIN_CATACLYSM,CRIMSON_DIVIDE,TENTACLE_CORRIDOR,SCULK_BURIAL,RADIATION_SWEEP,MAGNETIC_SAW -> 18;
            default -> 12;
        };
    }

    private static float damage(Attack attack) {
        return switch(attack) {
            case TWIN_CATACLYSM -> 4;
            case CROSS_BLAST,COLLISION_COURSE,EARTH_SPLIT,SEISMIC_LINES,SCULK_PULSE,SCULK_BURIAL,THORN_ROWS,BLOOM,VOID_LINES,BONE_SPEAR_CHOIR -> 14;
            case ARROW_HEAVEN,IMPALING_FORMATION,MAGNETIC_GRID,BLINKING_GRID,CROSSING_SHADOWS,TENTACLE_CORRIDOR,MELTDOWN_GRID,FALLING_FORGE,GRAVEFALL -> 16;
            case CONSTRICTING_HALO,TRIPLE_RIFT,HUNTER_LATTICE -> 18;
            case DISASSEMBLE,WORLD_EATER,ANCIENT_ROAR,RADIATION_SWEEP,CRUSHER,MAGNETIC_SAW,CLOSING_GARDEN,END_COLLAPSE,VOID_CROSS -> 20;
            case CRIMSON_DIVIDE,TRINITY_ASSAULT,FINAL_COUNTDOWN,LAST_MASS -> 28;
        };
    }

    private static void applyMotion(AttackGeometry geometry,Attack attack,ServerPlayer player) {
        double cx=geometry.originX(),cz=geometry.originZ();
        if(attack==Attack.TWIN_CATACLYSM&&player.distanceToSqr(geometry.secondX(),player.getY(),geometry.secondZ())<player.distanceToSqr(cx,player.getY(),cz)) {
            cx=geometry.secondX();
            cz=geometry.secondZ();
        }
        Vec3 away=new Vec3(player.getX()-cx,0,player.getZ()-cz).normalize();
        if(attack==Attack.TWIN_CATACLYSM) player.setDeltaMovement(away.x*.45,2.15,away.z*.45);
        else if(attack==Attack.CROSS_BLAST||attack==Attack.COLLISION_COURSE||attack==Attack.WORLD_EATER||attack==Attack.ANCIENT_ROAR)
            player.setDeltaMovement(away.x*1.35,.85,away.z*1.35);
        else player.setDeltaMovement(player.getDeltaMovement().add(away.x*.35,.22,away.z*.35));
        player.hurtMarked=true;
    }

    private static boolean cover(ServerLevel level,EventState row,Mob source,ServerPlayer player) {
        Vec3 from=source.getEyePosition();
        Vec3 to=player.getEyePosition();
        BlockHitResult hit=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,source));
        if(hit.getType()!=HitResult.Type.BLOCK) return false;
        BlockPos pos=hit.getBlockPos();
        if(hit.getLocation().distanceToSqr(to)>=from.distanceToSqr(to)) return false;
        var tracked=new java.util.LinkedHashMap<>(row.bloodMoonCover());
        long key=pos.asLong();
        if(!tracked.getOrDefault(key,false)||level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()) return false;
        level.destroyBlock(pos,false,source);
        tracked.remove(key);
        row.bloodMoonCover(tracked);
        return true;
    }

    private static void projectiles(ServerLevel level,EventState row,Mob source,Attack attack) {
        int count=attack==Attack.ARROW_HEAVEN?24:16;
        AttackGeometry geometry=geometry(row,attack,List.of(source),1);
        if(attack==Attack.IMPALING_FORMATION) {
            var trident=new net.minecraft.world.entity.projectile.ThrownTrident(level,source,new net.minecraft.world.item.ItemStack(Items.TRIDENT));
            Vec3 direction=new Vec3(geometry.targetX()-source.getX(),0,geometry.targetZ()-source.getZ()).normalize();
            trident.setPos(source.getX()+direction.x*source.getBbWidth()*.55,source.getEyeY()-.2,source.getZ()+direction.z*source.getBbWidth()*.55);
            trident.shoot(direction.x,.03,direction.z,1.9F,0);
            trident.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            EventMobModifiers.signature(trident);
            level.addFreshEntity(trident);
            return;
        }
        for(int i=0;i<count;i++) {
            int x=EventRegions.minX(row)+4+level.random.nextInt(Math.max(1,EventRegions.maxX(row)-EventRegions.minX(row)-7));
            int z=EventRegions.minZ(row)+4+level.random.nextInt(Math.max(1,EventRegions.maxZ(row)-EventRegions.minZ(row)-7));
            if(!geometry.contains(x+.5,z+.5)) continue;
            Arrow arrow=new Arrow(level,source);
            arrow.setPos(x+.5,surface(level,x,z).getY()+14,z+.5);
            arrow.setDeltaMovement(0,-1.5,0);
            arrow.setBaseDamage(EventMobModifiers.signatureDamage(row.worldTier(),row.wave(),6));
            arrow.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            EventMobModifiers.signature(arrow);
            level.addFreshEntity(arrow);
        }
    }

    private static void stageCollision(ServerLevel level,EventState row,List<Mob> bosses) {
        if(bosses.size()<2) return;
        int centerZ=(EventRegions.minZ(row)+EventRegions.maxZ(row))/2;
        BlockPos left=surface(level,EventRegions.minX(row)+6,centerZ-9);
        BlockPos right=surface(level,EventRegions.maxX(row)-6,centerZ+9);
        bosses.get(0).getNavigation().moveTo(left.getX()+.5,left.getY(),left.getZ()+.5,1.4);
        bosses.get(1).getNavigation().moveTo(right.getX()+.5,right.getY(),right.getZ()+.5,1.4);
    }

    private static void chooseTwinPattern(ServerLevel level,EventState row) {
        int previous=row.setPiece().twinPattern();
        int next=level.random.nextInt(8);
        if(next==previous) next=(next+1+level.random.nextInt(7))%8;
        row.editSetPiece().twinPattern(next);
    }

    private static void chooseSafeQuarter(ServerLevel level,EventState row) {
        int previous=Math.floorMod(row.setPiece().safeQuarter(),4);
        int next=level.random.nextInt(4);
        if(next==previous) next=(next+1+level.random.nextInt(3))%4;
        row.editSetPiece().safeQuarter(next);
    }

    private static void stageGeometry(ServerLevel level,EventState row,Attack attack,List<Mob> bosses) {
        Mob source=bosses.get(0);
        Mob second=bosses.size()>1?bosses.get(1):source;
        ServerPlayer target=source.getTarget() instanceof ServerPlayer player?player:new EventSession(row).active(level.getServer()).stream().min(Comparator.comparingDouble(source::distanceToSqr)).orElse(null);
        double targetX=(EventRegions.minX(row)+EventRegions.maxX(row)+1)/2.0;
        double targetZ=(EventRegions.minZ(row)+EventRegions.maxZ(row)+1)/2.0;
        if(target!=null) {
            Vec3 predicted=target.position().add(target.getDeltaMovement().multiply(12,0,12));
            targetX=Mth.clamp(predicted.x,EventRegions.minX(row)+2,EventRegions.maxX(row)-1);
            targetZ=Mth.clamp(predicted.z,EventRegions.minZ(row)+2,EventRegions.maxZ(row)-1);
        }
        if(attack==Attack.CRIMSON_DIVIDE) {
            Vec3 direction=new Vec3(targetX-source.getX(),0,targetZ-source.getZ()).normalize();
            double length=Math.min(EventRegions.maxX(row)-EventRegions.minX(row),EventRegions.maxZ(row)-EventRegions.minZ(row));
            targetX=source.getX()+direction.x*length;
            targetZ=source.getZ()+direction.z*length;
        }
        row.editSetPiece().originX(source.getX());
        row.editSetPiece().originZ(source.getZ());
        row.editSetPiece().secondX(second.getX());
        row.editSetPiece().secondZ(second.getZ());
        row.editSetPiece().targetX(targetX);
        row.editSetPiece().targetZ(targetZ);
    }

    private static AttackGeometry geometry(EventState row,Attack attack,List<Mob> bosses,double progress) {
        double originX=row.setPiece().hasOriginX()?row.setPiece().originX():bosses.get(0).getX();
        double originZ=row.setPiece().hasOriginZ()?row.setPiece().originZ():bosses.get(0).getZ();
        double secondX=row.setPiece().hasSecondX()?row.setPiece().secondX():originX;
        double secondZ=row.setPiece().hasSecondZ()?row.setPiece().secondZ():originZ;
        double targetX=row.setPiece().hasTargetX()?row.setPiece().targetX():(EventRegions.minX(row)+EventRegions.maxX(row)+1)/2.0;
        double targetZ=row.setPiece().hasTargetZ()?row.setPiece().targetZ():(EventRegions.minZ(row)+EventRegions.maxZ(row)+1)/2.0;
        int variant=attack==Attack.END_COLLAPSE?row.setPiece().safeQuarter():attack==Attack.ANCIENT_ROAR?row.setPiece().castParity():row.setPiece().twinPattern();
        double directionX=targetX-originX,directionZ=targetZ-originZ;
        double length=Math.hypot(directionX,directionZ);
        double radius=Math.min(EventRegions.maxX(row)-EventRegions.minX(row),EventRegions.maxZ(row)-EventRegions.minZ(row))/2.0;
        double width=switch(attack) {
            case CRIMSON_DIVIDE -> radius*.62;
            case TENTACLE_CORRIDOR -> 2.5;
            case SCULK_BURIAL -> 5;
            default -> 4;
        };
        long windup=currentTelegraphTicks(row,attack);
        return new AttackGeometry(AttackGeometry.Type.valueOf(attack.name()),EventRegions.minX(row)+3,EventRegions.maxX(row)-3,EventRegions.minZ(row)+3,EventRegions.maxZ(row)-3,
                originX,originZ,secondX,secondZ,targetX,targetZ,directionX,directionZ,radius,width,length,-Math.PI,Math.PI,
                row.setPiece().attackStart(),windup,1,progress,variant,row.setPiece().reverse());
    }

    private static void rupture(ServerLevel level,EventState row,AttackGeometry geometry) {
        Block block=block("deep_dark_regrowth:overloaded_sculk");
        for(int x=geometry.minX();x<=geometry.maxX();x++) for(int z=geometry.minZ();z<=geometry.maxZ();z++) {
            if(!geometry.front(x+.5,z+.5)) continue;
            setAttackTemporary(level,row,visibleFloor(level,x,z),block);
        }
    }

    private static void twinImpact(ServerLevel level,List<Mob> bosses) {
        for(Mob boss:bosses) {
            double y=boss.getY()+boss.getBbHeight()*.35;
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,boss.getX(),y,boss.getZ(),1,0,0,0,0);
            level.playSound(null,boss.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.HOSTILE,4,.55F);
        }
    }

    private static void face(Mob mob,double x,double z,float step) {
        float target=(float)(Mth.atan2(z-mob.getZ(),x-mob.getX())*180/Math.PI)-90;
        float yaw=Mth.rotLerp(step/180F,mob.getYRot(),target);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot=yaw;
    }

    private static void releaseCollision(List<Mob> bosses) {
        if(bosses.size()<2) return;
        Vec3 direction=bosses.get(1).position().subtract(bosses.get(0).position()).normalize();
        bosses.get(0).setDeltaMovement(direction.x*2.2,.15,direction.z*2.2);
        bosses.get(1).setDeltaMovement(-direction.x*2.2,.15,-direction.z*2.2);
        bosses.forEach(mob->mob.hurtMarked=true);
    }

    private static void activateCongregation(ServerLevel level,EventState row) {
        double ratio=healthRatio(level,row);
        int target=ratio<=.25?6:ratio<=.5?4:ratio<=.75?2:0;
        int active=row.setPiece().congregation();
        if(target<=active) return;
        int needed=target-active;
        for(Mob mob:livingSetPieces(level,row)) {
            if(needed<=0) break;
            if(!mob.getPersistentData().getBoolean(DORMANT)) continue;
            mob.getPersistentData().putBoolean(DORMANT,false);
            mob.setGlowingTag(true);
            mob.setNoAi(false);
            mob.setInvulnerable(false);
            needed--;
        }
        row.editSetPiece().congregation(target);
    }

    private static void initializeCooldowns(ServerLevel level,EventState row,Scenario scenario) {
        Map<String,Long> cooldowns=new java.util.LinkedHashMap<>();
        long now=level.getGameTime();
        attacks(scenario).forEach(spec->cooldowns.put(spec.attack().name(),now+attackCooldown(Math.max(80,spec.normal()/2))));
        row.editSetPiece().cooldowns(cooldowns);
        resetCombo(row);
        row.editSetPiece().comboNext(now+comboInterval(level));
    }

    private static boolean beginCombo(ServerLevel level,EventState row,Scenario scenario,List<Mob> bosses,double ratio) {
        List<Attack> pool=new ArrayList<>(attacks(scenario).stream()
                .map(AttackSpec::attack)
                .filter(BloodMoonSetPieces::comboSafe)
                .filter(attack->requirements(row,attack,bosses,ratio))
                .filter(attack->!attack.name().equals(row.setPiece().lastAttack())&&!attack.name().equals(row.setPiece().previousAttack()))
                .toList());
        if(pool.size()<COMBO_ATTACK_COUNT) {
            pool=new ArrayList<>(attacks(scenario).stream()
                    .map(AttackSpec::attack)
                    .filter(BloodMoonSetPieces::comboSafe)
                    .filter(attack->requirements(row,attack,bosses,ratio))
                    .toList());
        }
        if(pool.size()<COMBO_ATTACK_COUNT) {
            row.editSetPiece().comboNext(level.getGameTime()+comboInterval(level));
            return false;
        }
        for(int index=pool.size()-1;index>0;index--) {
            int replacement=level.random.nextInt(index+1);
            Attack attack=pool.get(index);
            pool.set(index,pool.get(replacement));
            pool.set(replacement,attack);
        }
        List<String> queue=new ArrayList<>();
        for(int index=0;index<COMBO_ATTACK_COUNT;index++) queue.add(pool.get(index).name());
        row.editSetPiece().comboQueue(queue);
        row.editSetPiece().comboIndex(0);
        row.editSetPiece().comboActive(true);
        row.editSetPiece().comboNext(level.getGameTime()+comboInterval(level));
        return true;
    }

    private static boolean startNextCombo(ServerLevel level,EventState row,Scenario scenario,List<Mob> bosses) {
        List<String> queue=row.setPiece().comboQueue();
        int index=row.setPiece().comboIndex();
        if(index>=queue.size()) return false;
        Attack attack=Attack.valueOf(queue.get(index));
        row.editSetPiece().comboIndex(index+1);
        start(level,row,scenario,attack,bosses,true);
        return true;
    }

    private static void finishCombo(ServerLevel level,EventState row) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishComboInScope(level, row); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishComboInScope(level, store.events().bind(row)));
    }

    private static void finishComboInScope(ServerLevel level,EventState row) {
        row.editSetPiece().clearComboQueue();
        row.editSetPiece().clearComboIndex();
        row.editSetPiece().clearComboActive();

    }

    private static void resetCombo(EventState row) {
        row.editSetPiece().clearComboQueue();
        row.editSetPiece().clearComboIndex();
        row.editSetPiece().clearComboNext();
        row.editSetPiece().clearComboActive();
    }

    private static boolean comboSafe(Attack attack) {
        return attack!=Attack.TWIN_CATACLYSM&&attack!=Attack.COLLISION_COURSE&&attack!=Attack.CRIMSON_DIVIDE
                &&attack!=Attack.FINAL_COUNTDOWN&&attack!=Attack.FALLING_FORGE&&attack!=Attack.LAST_MASS;
    }

    private static long comboInterval(ServerLevel level) {
        return COMBO_MIN_INTERVAL_TICKS+level.random.nextInt(COMBO_INTERVAL_VARIANCE_TICKS);
    }

    private static long currentTelegraphTicks(EventState row,Attack attack) {
        if(row.setPiece().hasAttackStart()&&row.setPiece().hasImpact()) return Math.max(1,row.setPiece().impact()-row.setPiece().attackStart());
        return telegraphTicks(attack);
    }

    private static long finalTelegraphLead(EventState row,Attack attack) {
        return Math.max(4,Math.min(12,currentTelegraphTicks(row,attack)/3));
    }

    private static boolean requirements(EventState row,Attack attack,List<Mob> bosses,double ratio) {
        if((attack==Attack.TRINITY_ASSAULT&&bosses.size()<3)||(attack==Attack.TWIN_CATACLYSM&&bosses.size()<2)) return false;
        if((attack==Attack.FINAL_COUNTDOWN||attack==Attack.LAST_MASS)&&(ratio>=.25||row.setPiece().finalUsed())) return false;
        return true;
    }

    private static AttackSpec weighted(ServerLevel level,List<AttackSpec> available,String last,String previous) {
        List<AttackSpec> fresh=available.stream().filter(spec->!spec.attack().name().equals(last)&&!spec.attack().name().equals(previous)).toList();
        if(fresh.isEmpty()) fresh=available.stream().filter(spec->!spec.attack().name().equals(last)).toList();
        List<AttackSpec> pool=fresh.isEmpty()?available:fresh;
        double total=pool.stream().mapToDouble(AttackSpec::weight).sum();
        double roll=level.random.nextDouble()*total;
        for(AttackSpec spec:pool) {
            roll-=spec.weight();
            if(roll<=0) return spec;
        }
        return pool.get(pool.size()-1);
    }

    private static double healthRatio(ServerLevel level,EventState row) {
        double initial=row.setPiece().initialHealth();
        if(initial<=0) return 1;
        return Mth.clamp(livingBosses(level,row).stream().mapToDouble(LivingEntity::getHealth).sum()/initial,0,1);
    }

    private static List<Mob> livingBosses(ServerLevel level,EventState row) {
        return entities(level,row.setPiece().bosses());
    }

    private static List<Mob> livingSetPieces(ServerLevel level,EventState row) {
        return entities(level,row.setPiece().mobs());
    }

    private static List<Mob> entities(ServerLevel level,List<UUID> ids) {
        List<Mob> result=new ArrayList<>();
        for(UUID id:ids) {
            Entity entity=level.getEntity(id);
            if(entity instanceof Mob mob&&mob.isAlive()) result.add(mob);
        }
        return result;
    }

    private static void advanceIntroductions(ServerLevel level,EventState row,List<Mob> bosses) {
        for(Mob boss:bosses) {
            ServerPlayer target=new EventSession(row).active(level.getServer()).stream().min(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
            if(target!=null) face(boss,target.getX(),target.getZ(),8);
            boolean wasIntroducing=SpawnPresentation.active(boss);
            if(!SpawnPresentation.tick(level,boss)||!wasIntroducing) continue;
            if(boss instanceof Creeper creeper&&boss.getPersistentData().getBoolean(TWIN)) {
                power(level,creeper);
            }
            if(target!=null) boss.setTarget(target);
        }
    }

    private static boolean introducing(Mob mob) {
        return SpawnPresentation.active(mob);
    }

    private static void advanceArena(ServerLevel level,EventState row,Scenario scenario,List<Mob> bosses) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { advanceArenaInScope(level, row, scenario, bosses); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> advanceArenaInScope(level, store.events().bind(row), scenario, bosses));
    }

    private static void advanceArenaInScope(ServerLevel level,EventState row,Scenario scenario,List<Mob> bosses) {
        List<Long> queue=row.setPiece().floorQueue();
        int index=row.setPiece().floorIndex();
        if(queue.isEmpty()||index>=queue.size()) return;
        FloorPalette palette=floorPalette(scenario);
        int centerX=(EventRegions.minX(row)+EventRegions.maxX(row))/2;
        int centerZ=(EventRegions.minZ(row)+EventRegions.maxZ(row))/2;
        double radius=Math.max(1,Math.hypot((EventRegions.maxX(row)-EventRegions.minX(row))/2.0,(EventRegions.maxZ(row)-EventRegions.minZ(row))/2.0));
        int end=Math.min(queue.size(),index+FLOOR_BLOCKS_PER_TICK);
        BlockPos last=null;
        for(;index<end;index++) {
            BlockPos floor=BlockPos.of(queue.get(index));
            BlockPos cover=floor.above();
            if(thinCover(level.getBlockState(cover))) {
                setTemporary(level,row,cover,Blocks.AIR.defaultBlockState());
            }
            double progress=Math.hypot(floor.getX()-centerX,floor.getZ()-centerZ)/radius;
            setTemporary(level,row,floor,(progress<=FLOOR_CENTER_RADIUS?palette.center():progress<=FLOOR_MIDDLE_RADIUS?palette.middle():palette.outer()).defaultBlockState());
            last=floor;
        }
        row.editSetPiece().floorIndex(index);
        if(last!=null&&(level.getGameTime()&3)==0) {
            level.playSound(null,last,SoundEvents.SCULK_BLOCK_SPREAD,SoundSource.BLOCKS,.9F,.75F+level.random.nextFloat()*.2F);
            level.sendParticles(ParticleTypes.SCULK_SOUL,last.getX()+.5,last.getY()+1.1,last.getZ()+.5,3,.8,.1,.8,.02);
        }

    }

    private static void buildArena(ServerLevel level,EventState row,Scenario scenario,int wave) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { buildArenaInScope(level, row, scenario, wave); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> buildArenaInScope(level, store.events().bind(row), scenario, wave));
    }

    private static void buildArenaInScope(ServerLevel level,EventState row,Scenario scenario,int wave) {
        if(row.setPiece().arenaWave()==wave&&!row.setPiece().terrain().isEmpty()) return;
        if(pendingRestoration(row)) {
            row.editSetPiece().restoring(true);
            resumeRestoration(level,row);
            if(pendingRestoration(row)) return;
        }
        int centerX=(EventRegions.minX(row)+EventRegions.maxX(row))/2;
        int centerZ=(EventRegions.minZ(row)+EventRegions.maxZ(row))/2;
        List<BlockPos> floors=new ArrayList<>();
        Map<BlockPos,Double> floorOrder=new HashMap<>();
        for(int x=EventRegions.minX(row)+3;x<=EventRegions.maxX(row)-3;x++) for(int z=EventRegions.minZ(row)+3;z<=EventRegions.maxZ(row)-3;z++) {
            BlockPos floor=visibleFloor(level,x,z);
            floors.add(floor);
            floorOrder.put(floor,Math.hypot(floor.getX()-centerX,floor.getZ()-centerZ)+level.random.nextDouble()*FLOOR_ORDER_JITTER);
        }
        floors.sort(Comparator.comparingDouble(floorOrder::get));
        row.editSetPiece().floorQueue(floors.stream().map(BlockPos::asLong).toList());
        row.editSetPiece().floorIndex(0);
        row.editSetPiece().arenaWave(wave);

    }

    private static BlockPos visibleFloor(ServerLevel level,int x,int z) {
        BlockPos visible=surface(level,x,z).below();
        return thinCover(level.getBlockState(visible))?visible.below():visible;
    }

    private static boolean thinCover(BlockState state) {
        return state.getBlock() instanceof SnowLayerBlock||state.canBeReplaced()&&state.getFluidState().isEmpty()&&!state.isAir();
    }

    private static void setTemporary(ServerLevel level,EventState row,BlockPos pos,BlockState temporary) {
        BlockState current=level.getBlockState(pos);
        if(current.hasBlockEntity()||!current.getFluidState().isEmpty()||current.is(Blocks.BEDROCK)||current.is(Blocks.BARRIER)) return;
        if(saveChange(level,row,TerrainLayer.ARENA,pos,current,temporary)) level.setBlock(pos,temporary,3);
    }

    private static FloorPalette floorPalette(Scenario scenario) {
        return switch(scenario) {
            case ANCIENT_WARDEN -> new FloorPalette(block("deep_dark_regrowth:sculk_soil"),block("deep_dark_regrowth:infested_sculk"),block("deep_dark_regrowth:overloaded_sculk"));
            case TWIN_CREEPERS,CRITICAL_MASS -> new FloorPalette(block("alexscaves:radrock"),block("alexscaves:acidic_radrock"),block("alexscaves:block_of_uranium"));
            case MAGNETIC_ASSEMBLY,IRON_HEART -> new FloorPalette(block("alexscaves:galena"),block("alexscaves:energized_galena_neutral"),block("alexscaves:magnetic_activator"));
            case CARNIVOROUS_GARDEN -> new FloorPalette(block("skarrier_mobs:wrong_deepslate_tangled"),block("skarrier_mobs:silk_block"),block("skarrier_mobs:resisteel_block"));
            case RED_HUNT,LAST_SENTINEL -> new FloorPalette(block("legendary_monsters:enderstone"),block("legendary_monsters:enderstone_bricks"),block("legendary_monsters:enderitium_block"));
            case BREACH_BROTHERS,BONE_CATHEDRAL -> new FloorPalette(block("skarrier_mobs:wrong_deepslate"),block("skarrier_mobs:wrong_deepslate_tiles"),block("skarrier_mobs:resisteel_block"));
            case RED_KNIGHTS -> new FloorPalette(Blocks.NETHERRACK,Blocks.NETHER_BRICKS,Blocks.RED_NETHER_BRICKS);
            case TREMOR -> new FloorPalette(Blocks.TUFF,Blocks.PACKED_MUD,Blocks.MUD_BRICKS);
        };
    }

    private static SpawnPresentation.Archetype presentation(Scenario scenario) {
        return switch(scenario) {
            case ANCIENT_WARDEN -> SpawnPresentation.Archetype.SCULK_EMERGE;
            case CRITICAL_MASS,TREMOR,CARNIVOROUS_GARDEN -> SpawnPresentation.Archetype.GROUND_RUPTURE;
            case TWIN_CREEPERS,MAGNETIC_ASSEMBLY,IRON_HEART -> SpawnPresentation.Archetype.LIGHTNING_ENTRY;
            case LAST_SENTINEL,RED_HUNT -> SpawnPresentation.Archetype.PORTAL_ENTRY;
            case BONE_CATHEDRAL,BREACH_BROTHERS,RED_KNIGHTS -> SpawnPresentation.Archetype.GROUND_EMERGE;
        };
    }

    private static int presentationDuration(Scenario scenario) {
        return scenario==Scenario.ANCIENT_WARDEN?133:scenario==Scenario.CRITICAL_MASS?70:50;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(id)).orElseThrow(()->new IllegalStateException("Missing Blood Moon floor block: "+id));
    }

    private enum TerrainLayer { ARENA, ATTACK }

    private static Map<Long,EventTerrainChange> terrain(EventState row,TerrainLayer layer) {
        return layer==TerrainLayer.ATTACK?row.setPiece().attackTerrain():row.setPiece().terrain();
    }

    private static void terrain(EventState row,TerrainLayer layer,Map<Long,EventTerrainChange> values) {
        if(layer==TerrainLayer.ATTACK) row.editSetPiece().attackTerrain(values);
        else row.editSetPiece().terrain(values);
    }

    private static BlockState blockState(ServerLevel level,NativeNbtPayload payload) {
        CompoundTag encoded=payload==null?new CompoundTag():(CompoundTag)payload.copyNativeTag();
        return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),encoded);
    }

    private static boolean saveChange(ServerLevel level,EventState row,TerrainLayer layer,BlockPos pos,BlockState original,BlockState temporary) {
        var snapshot=new java.util.LinkedHashMap<>(terrain(row,layer));
        long key=pos.asLong();
        EventTerrainChange saved=snapshot.get(key);
        if(saved!=null) {
            if(saved.hasOriginal()) {
                if(saved.hasTemporary()&&!original.equals(blockState(level,saved.temporary()))) return false;
            } else if(saved.hasLegacyOriginal()&&!legacyTemporary(row,layer,original)) return false;
        }
        EventTerrainChange entry=saved==null?new EventTerrainChange():saved.copy();
        entry.original(saved!=null&&saved.hasOriginal()?saved.original()
                :saved!=null&&saved.hasLegacyOriginal()?saved.legacyOriginal():new NativeNbtPayload(NbtUtils.writeBlockState(original)));
        entry.temporary(new NativeNbtPayload(NbtUtils.writeBlockState(temporary)));
        snapshot.put(key,entry);
        terrain(row,layer,snapshot);
        return true;
    }

    private static void resumeRestoration(ServerLevel level,EventState row) {
        restoreAttackBlocks(level,row);
        if(row.setPiece().attackTerrain().isEmpty()) restoreTerrain(level,row);
        if(!pendingRestoration(row)) {
            row.editSetPiece().clearRestoring();
            if(!row.combatStarted()||!row.state().equals("ACTIVE")) {
                row.clearWave3SetPiece();
                row.clearWave5SetPiece();
            }
        }
    }

    private static boolean pendingRestoration(EventState row) {
        return !row.setPiece().attackTerrain().isEmpty()||!row.setPiece().terrain().isEmpty();
    }

    private static void restoreSnapshot(ServerLevel level,EventState row,TerrainLayer layer) {
        var snapshot=new java.util.LinkedHashMap<>(terrain(row,layer));
        for(long key:new ArrayList<>(snapshot.keySet())) {
            BlockPos pos=BlockPos.of(key);
            if(!level.hasChunkAt(pos)) continue;
            EventTerrainChange saved=snapshot.get(key);
            BlockState original=blockState(level,saved.hasOriginal()?saved.original():saved.legacyOriginal());
            BlockState current=level.getBlockState(pos);
            boolean owned=saved.hasTemporary()?current.equals(blockState(level,saved.temporary())):legacyTemporary(row,layer,current);
            if(owned&&!current.equals(original)) level.setBlock(pos,original,3);
            snapshot.remove(key);
        }
        terrain(row,layer,snapshot);
    }

    private static boolean legacyTemporary(EventState row,TerrainLayer layer,BlockState current) {
        if(layer==TerrainLayer.ATTACK) return current.is(block("deep_dark_regrowth:overloaded_sculk"));
        if(current.isAir()) return true;
        int wave=row.setPiece().arenaWave();
        Scenario scenario=scenario(wave==3?row.wave3SetPiece():row.wave5SetPiece());
        if(scenario==null) return false;
        FloorPalette palette=floorPalette(scenario);
        return current.is(palette.outer())||current.is(palette.middle())||current.is(palette.center());
    }

    private static void restoreTerrain(ServerLevel level,EventState row) {
        restoreSnapshot(level,row,TerrainLayer.ARENA);
        if(row.setPiece().terrain().isEmpty()) {
            row.editSetPiece().clearTerrain();
            row.editSetPiece().clearArenaWave();
            row.editSetPiece().clearFloorQueue();
            row.editSetPiece().clearFloorIndex();
        }
    }

    private static void restoreAttackBlocks(ServerLevel level,EventState row) {
        restoreSnapshot(level,row,TerrainLayer.ATTACK);
        if(row.setPiece().attackTerrain().isEmpty()) {
            row.editSetPiece().clearAttackTerrain();
            row.editSetPiece().clearAttackBlocks();
            row.editSetPiece().clearAttackBlocksUntil();
        }
    }

    private static void setAttackTemporary(ServerLevel level,EventState row,BlockPos pos,Block block) {
        BlockState current=level.getBlockState(pos);
        if(current.hasBlockEntity()||!current.getFluidState().isEmpty()||current.is(Blocks.BEDROCK)||current.is(Blocks.BARRIER)) return;
        BlockState temporary=block.defaultBlockState();
        var terrain=row.setPiece().attackTerrain();
        boolean added=!terrain.containsKey(pos.asLong());
        if(saveChange(level,row,TerrainLayer.ATTACK,pos,current,temporary)) {
            if(added) {
                row.editSetPiece().attackBlocks(append(row.setPiece().attackBlocks(),pos.asLong()));
            }
            level.setBlock(pos,temporary,3);
        }
    }

    private static long telegraphTicks(Attack attack) {
        long base=switch(attack) {
            case TWIN_CATACLYSM -> 30;
            case CROSS_BLAST -> 24;
            case CRIMSON_DIVIDE -> 40;
            case TRINITY_ASSAULT,LAST_MASS -> 44;
            case FINAL_COUNTDOWN -> 80;
            case WORLD_EATER,ANCIENT_ROAR,MAGNETIC_SAW,CLOSING_GARDEN,VOID_CROSS -> 36;
            default -> 30;
        };
        return Math.round(base*ATTACK_TELEGRAPH_SLOWDOWN);
    }

    private static long attackCooldown(long ticks) {
        return ticks==Long.MAX_VALUE?Long.MAX_VALUE:Math.max(1,Math.round(ticks*ATTACK_COOLDOWN_SCALE));
    }

    private static boolean compatible(Scenario scenario) {
        return spawns(scenario).stream().allMatch(spawn->{
            ResourceLocation id=new ResourceLocation(spawn.id());
            if(!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return false;
            EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(id);
            return !spawn.boss()||!type.builtInRegistryHolder().is(CAMPAIGN_BOSSES);
        });
    }

    private static List<String> selections(Set<Scenario> scenarios) {
        return scenarios.stream().map(scenario->scenario.name().toLowerCase(Locale.ROOT)).toList();
    }

    private static Scenario current(EventState row) {
        if(row.wave()==3) return scenario(row.wave3SetPiece());
        if(row.wave()==BloodMoonWaves.WAVES) return scenario(row.wave5SetPiece());
        return null;
    }

    private static Scenario scenario(String name) {
        if(name.isEmpty()) return null;
        try{return Scenario.valueOf(name);}catch(IllegalArgumentException ignored){return null;}
    }

    private static List<Spawn> spawns(Scenario scenario) {
        return switch(scenario) {
            case TWIN_CREEPERS -> List.of(new Spawn("minecraft:creeper",LARGE_CREEPER_SCALE,true,false),new Spawn("minecraft:creeper",LARGE_CREEPER_SCALE,true,false));
            case BREACH_BROTHERS -> copies("skarrier_mobs:breacher",2,2.1F,true,false);
            case RED_KNIGHTS -> List.of(new Spawn("minecraft:vindicator",2.1F,true,false),new Spawn("minecraft:drowned",2.1F,true,false),new Spawn("minecraft:skeleton",2.1F,true,false));
            case MAGNETIC_ASSEMBLY -> copies("alexscaves:magnetron",2,1.7F,true,false);
            case RED_HUNT -> copies("legendary_monsters:ambusher",4,1.65F,true,false);
            case TREMOR -> copies("alexscaves:tremorsaurus",1,2.1F,true,false);
            case ANCIENT_WARDEN -> List.of(new Spawn("minecraft:warden",1.8F,true,false));
            case CRITICAL_MASS -> concat(copies("alexscaves:nucleeper",1,2.2F,true,false),copies("alexscaves:gammaroach",6,1.15F,false,false));
            case IRON_HEART -> copies("alexscaves:magnetron",1,2.25F,true,false);
            case CARNIVOROUS_GARDEN -> concat(copies("skarrier_mobs:carniflore",1,2.1F,true,false),copies("skarrier_mobs:zombiflore",6,1.15F,false,false));
            case LAST_SENTINEL -> concat(copies("legendary_monsters:endersent",1,2.1F,true,false),copies("minecraft:enderman",6,1.15F,false,false));
            case BONE_CATHEDRAL -> concat(List.of(new Spawn("skarrier_mobs:wrought",2F,true,false)),alternating(6,1.2F,false,true,"minecraft:skeleton","minecraft:wither_skeleton"));
        };
    }

    private static List<Spawn> alternating(int count,float scale,boolean boss,boolean dormant,String... ids) {
        List<Spawn> result=new ArrayList<>();
        for(int index=0;index<count;index++) result.add(new Spawn(ids[index%ids.length],scale,boss,dormant));
        return result;
    }

    private static List<Spawn> copies(String id,int count,float scale,boolean boss,boolean dormant) {
        List<Spawn> result=new ArrayList<>();
        for(int i=0;i<count;i++) result.add(new Spawn(id,scale,boss,dormant));
        return result;
    }

    private static List<Spawn> concat(List<Spawn> first,List<Spawn> second) {
        List<Spawn> result=new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private static List<AttackSpec> attacks(Scenario scenario) {
        List<AttackSpec> result=new ArrayList<>(switch(scenario) {
            case TWIN_CREEPERS -> List.of(spec(Attack.TWIN_CATACLYSM,560,360),spec(Attack.CROSS_BLAST,480,320),spec(Attack.EARTH_SPLIT,520,340),spec(Attack.MELTDOWN_GRID,500,320),spec(Attack.CLOSING_GARDEN,620,400));
            case BREACH_BROTHERS -> List.of(spec(Attack.COLLISION_COURSE,480,300),spec(Attack.EARTH_SPLIT,600,380),spec(Attack.CROSS_BLAST,500,320),spec(Attack.SEISMIC_LINES,520,340),spec(Attack.CRUSHER,620,400));
            case RED_KNIGHTS -> List.of(spec(Attack.CRIMSON_DIVIDE,640,400),spec(Attack.IMPALING_FORMATION,540,340),spec(Attack.ARROW_HEAVEN,480,300),spec(Attack.BONE_SPEAR_CHOIR,520,340),new AttackSpec(Attack.TRINITY_ASSAULT,300,220,.25));
            case MAGNETIC_ASSEMBLY -> List.of(spec(Attack.DISASSEMBLE,600,380),spec(Attack.MAGNETIC_GRID,480,300),spec(Attack.MAGNETIC_SAW,560,360),spec(Attack.CRUSHER,500,320),spec(Attack.FALLING_FORGE,620,400));
            case RED_HUNT -> List.of(spec(Attack.BLINKING_GRID,440,280),spec(Attack.CROSSING_SHADOWS,560,360),spec(Attack.VOID_LINES,480,300),spec(Attack.VOID_CROSS,520,340),spec(Attack.END_COLLAPSE,620,400));
            case TREMOR -> List.of(spec(Attack.SEISMIC_LINES,520,340),spec(Attack.WORLD_EATER,680,440),spec(Attack.EARTH_SPLIT,500,320),spec(Attack.CRUSHER,560,360),spec(Attack.GRAVEFALL,620,400));
            case ANCIENT_WARDEN -> List.of(spec(Attack.SCULK_PULSE,540,340),spec(Attack.TENTACLE_CORRIDOR,620,400),spec(Attack.SCULK_BURIAL,680,440),spec(Attack.ANCIENT_ROAR,760,500),spec(Attack.BLINKING_GRID,500,320));
            case CRITICAL_MASS -> List.of(spec(Attack.RADIATION_SWEEP,500,320),spec(Attack.MELTDOWN_GRID,580,380),spec(Attack.WORLD_EATER,620,400),spec(Attack.CROSS_BLAST,520,340),new AttackSpec(Attack.FINAL_COUNTDOWN,Long.MAX_VALUE,Long.MAX_VALUE,.1));
            case IRON_HEART -> List.of(spec(Attack.CRUSHER,480,300),spec(Attack.FALLING_FORGE,540,340),spec(Attack.MAGNETIC_SAW,680,440),spec(Attack.MAGNETIC_GRID,500,320),spec(Attack.DISASSEMBLE,620,400));
            case CARNIVOROUS_GARDEN -> List.of(spec(Attack.THORN_ROWS,460,300),spec(Attack.BLOOM,400,260),spec(Attack.CLOSING_GARDEN,680,440),spec(Attack.SCULK_BURIAL,520,340),spec(Attack.IMPALING_FORMATION,560,360));
            case LAST_SENTINEL -> List.of(spec(Attack.VOID_LINES,440,280),spec(Attack.END_COLLAPSE,560,360),spec(Attack.VOID_CROSS,640,420),spec(Attack.CROSSING_SHADOWS,500,320),spec(Attack.BLINKING_GRID,460,300));
            case BONE_CATHEDRAL -> List.of(spec(Attack.BONE_SPEAR_CHOIR,460,300),spec(Attack.GRAVEFALL,540,360),spec(Attack.IMPALING_FORMATION,500,320),spec(Attack.ARROW_HEAVEN,560,360),new AttackSpec(Attack.LAST_MASS,Long.MAX_VALUE,Long.MAX_VALUE,.1));
        });
        result.add(spec(Attack.CONSTRICTING_HALO,520,340));
        result.add(spec(Attack.TRIPLE_RIFT,500,320));
        result.add(spec(Attack.HUNTER_LATTICE,480,300));
        return List.copyOf(result);
    }

    private static AttackSpec spec(Attack attack,long normal,long aggressive) {
        return new AttackSpec(attack,Math.max(120,Math.round(normal*.35)),Math.max(100,Math.round(aggressive*.25)),1);
    }

    private enum Scenario {
        TWIN_CREEPERS(false),BREACH_BROTHERS(false),RED_KNIGHTS(false),MAGNETIC_ASSEMBLY(false),RED_HUNT(false),TREMOR(true),
        ANCIENT_WARDEN(true),CRITICAL_MASS(true),IRON_HEART(true),CARNIVOROUS_GARDEN(true),LAST_SENTINEL(true),BONE_CATHEDRAL(true);
        private final boolean single;
        Scenario(boolean single){this.single=single;}
        boolean single(){return single;}
    }

    private enum Attack {
        TWIN_CATACLYSM,CROSS_BLAST,COLLISION_COURSE,EARTH_SPLIT,CRIMSON_DIVIDE,IMPALING_FORMATION,ARROW_HEAVEN,TRINITY_ASSAULT,
        DISASSEMBLE,MAGNETIC_GRID,BLINKING_GRID,CROSSING_SHADOWS,SEISMIC_LINES,WORLD_EATER,SCULK_PULSE,TENTACLE_CORRIDOR,SCULK_BURIAL,
        ANCIENT_ROAR,RADIATION_SWEEP,MELTDOWN_GRID,FINAL_COUNTDOWN,CRUSHER,FALLING_FORGE,MAGNETIC_SAW,THORN_ROWS,BLOOM,
        CLOSING_GARDEN,VOID_LINES,END_COLLAPSE,VOID_CROSS,BONE_SPEAR_CHOIR,GRAVEFALL,LAST_MASS,
        CONSTRICTING_HALO,TRIPLE_RIFT,HUNTER_LATTICE
    }

    private record Spawn(String id,float scale,boolean boss,boolean dormant) {}
    private record FloorPalette(Block outer,Block middle,Block center) {}
    private record AttackSpec(Attack attack,long normal,long aggressive,double weight) {}

    private BloodMoonSetPieces() {}
}
