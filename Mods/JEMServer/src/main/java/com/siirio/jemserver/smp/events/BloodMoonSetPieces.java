package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
    private static final String WAVE_THREE="wave3SetPiece";
    private static final String WAVE_FIVE="wave5SetPiece";
    private static final String WAVE_THREE_SPAWNED="wave3SetPieceSpawned";
    private static final String WAVE_FIVE_SPAWNED="wave5SetPieceSpawned";
    private static final String SET_PIECE_MOBS="setPieceMobs";
    private static final String SET_PIECE_BOSSES="setPieceBosses";
    private static final String SET_PIECE="jem:setpiece";
    private static final String SET_PIECE_BOSS="jem:setpiece_boss";
    private static final String DORMANT="jem:setpiece_dormant";
    private static final String TWIN="jem:twin_creeper";
    private static final String TWIN_DETONATIONS="jem:twin_detonations";
    private static final String EXPOSED_UNTIL="jem:setpiece_exposed_until";
    private static final String ATTACK="setPieceAttack";
    private static final String IMPACT="setPieceImpact";
    private static final String ATTACK_START="setPieceAttackStart";
    private static final String COOLDOWNS="setPieceCooldowns";
    private static final String INITIAL_HEALTH="setPieceInitialHealth";
    private static final String LAST_ATTACK="setPieceLastAttack";
    private static final String PREVIOUS_ATTACK="setPiecePreviousAttack";
    private static final String RECOVERY_UNTIL="setPieceRecoveryUntil";
    private static final String TERRAIN="setPieceTerrain";
    private static final String ATTACK_BLOCKS="setPieceAttackBlocks";
    private static final String ATTACK_TERRAIN="setPieceAttackTerrain";
    private static final String ATTACK_BLOCKS_UNTIL="setPieceAttackBlocksUntil";
    private static final String RESTORING="setPieceRestoring";
    private static final String ORIGINAL="Original";
    private static final String TEMPORARY="Temporary";
    private static final String COVER="bloodMoonCover";
    private static final String ARENA_WAVE="setPieceArenaWave";
    private static final String FLOOR_QUEUE="setPieceFloorQueue";
    private static final String FLOOR_INDEX="setPieceFloorIndex";
    private static final String COMBO_QUEUE="setPieceComboQueue";
    private static final String COMBO_INDEX="setPieceComboIndex";
    private static final String COMBO_NEXT="setPieceComboNext";
    private static final String COMBO_ACTIVE="setPieceComboActive";
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

    public static boolean select(CompoundTag row,String waveThree,String waveFive) {
        Scenario third=scenario(waveThree.toUpperCase(Locale.ROOT));
        Scenario fifth=scenario(waveFive.toUpperCase(Locale.ROOT));
        if(!WAVE_THREE_SCENARIOS.contains(third)||!WAVE_FIVE_SCENARIOS.contains(fifth)) return false;
        row.putString(WAVE_THREE,third.name());
        row.putString(WAVE_FIVE,fifth.name());
        return true;
    }

    public static int beginWave(ServerLevel level,CompoundTag row,List<ServerPlayer> players,int wave,int tier) {
        Set<Scenario> pool=wave==3?WAVE_THREE_SCENARIOS:wave==BloodMoonWaves.WAVES?WAVE_FIVE_SCENARIOS:Set.of();
        if(pool.isEmpty()) return 0;
        row.put(SET_PIECE_MOBS,new ListTag());
        row.put(SET_PIECE_BOSSES,new ListTag());
        row.remove(ATTACK);
        row.remove(IMPACT);
        row.remove(ATTACK_START);
        row.remove(RECOVERY_UNTIL);
        row.remove("setPieceAttackPhase");
        row.remove("setPiecePhaseUntil");
        row.remove(LAST_ATTACK);
        row.remove(PREVIOUS_ATTACK);
        row.remove("setPieceFinalUsed");
        row.remove("setPieceCongregation");
        row.remove(FLOOR_QUEUE);
        row.remove(FLOOR_INDEX);
        resetCombo(row);
        BossDebuffController.reset(row);
        String key=wave==3?WAVE_THREE:WAVE_FIVE;
        Scenario selected=scenario(row.getString(key));
        if(selected==null||!pool.contains(selected)||!compatible(selected)) {
            var compatible=pool.stream().filter(BloodMoonSetPieces::compatible).toList();
            if(compatible.isEmpty()) return 0;
            selected=compatible.get(level.random.nextInt(compatible.size()));
            row.putString(key,selected.name());
        }
        buildArena(level,row,selected,wave);
        int count=spawnScenario(level,row,players,selected,tier,wave);
        if(count>0) row.putBoolean(spawnedKey(wave),true);
        double initial=livingBosses(level,row).stream().mapToDouble(LivingEntity::getMaxHealth).sum();
        row.putDouble(INITIAL_HEALTH,Math.max(1,initial));
        initializeCooldowns(level,row,selected);
        SmpData.get(level.getServer()).changed(row);
        return count;
    }

    public static int recoverMissingSpawn(ServerLevel level,CompoundTag row,List<ServerPlayer> players,int wave,int tier) {
        if(wave!=3&&wave!=BloodMoonWaves.WAVES) return 0;
        String spawnedKey=spawnedKey(wave);
        if(row.getBoolean(spawnedKey)) return 0;
        if(!livingSetPieces(level,row).isEmpty()) {
            row.putBoolean(spawnedKey,true);
            SmpData.get(level.getServer()).changed(row);
            return 0;
        }
        Scenario selected=current(row);
        if(selected==null) return 0;
        int count=spawnScenario(level,row,players,selected,tier,wave);
        if(count<=0) return 0;
        row.putBoolean(spawnedKey,true);
        row.putDouble(INITIAL_HEALTH,Math.max(1,livingBosses(level,row).stream().mapToDouble(LivingEntity::getMaxHealth).sum()));
        initializeCooldowns(level,row,selected);
        SmpData.get(level.getServer()).changed(row);
        return count;
    }

    public static void prepareSpawn(CompoundTag row,Mob mob,boolean large) {
        int duration=large?LARGE_SPAWN_TELEGRAPH_TICKS:NORMAL_SPAWN_TELEGRAPH_TICKS;
        var position=mob.blockPosition();
        new EventSession(row).active(mob.getServer()).forEach(player->EventNetwork.beam(player,position,duration,mob.getBbWidth()));
    }

    public static double ordinarySpawnPressure(ServerLevel level,CompoundTag row) {
        return 1;
    }

    public static void finishWave(ServerLevel level,CompoundTag row,int wave) {
        if((wave==3||wave==BloodMoonWaves.WAVES)&&row.getInt(ARENA_WAVE)==wave) {
            row.putBoolean(RESTORING,true);
            resumeRestoration(level,row);
            SmpData.get(level.getServer()).changed(row);
        }
    }

    public static boolean survivingTwin(Creeper creeper) {
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
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag row=EventSession.forPlayer(player);
        if(row==null||!EventRegions.contains(row,level,event.getPos())||!"RED_KNIGHTS".equals(row.getString(WAVE_THREE))) return;
        CompoundTag cover=row.getCompound(COVER);
        if(event instanceof BlockEvent.EntityMultiPlaceEvent multiple)
            multiple.getReplacedBlockSnapshots().forEach(snapshot->cover.putBoolean(Long.toString(snapshot.getPos().asLong()),true));
        else cover.putBoolean(Long.toString(event.getPos().asLong()),true);
        row.put(COVER,cover);
        SmpData.get(level.getServer()).changed(row);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        MinecraftServer server=event.getServer();
        for(var row:SmpData.get(server).all("events")) {
            ServerLevel level=EventRegions.level(server,row);
            if(level==null) continue;
            if(row.getBoolean(RESTORING)&&(server.getTickCount()%20)==0) {
                resumeRestoration(level,row);
                SmpData.get(server).changed(row);
            }
            if(!row.getString("state").equals("ACTIVE")||!row.getString("activity").equals("BLOOD_MOON")) continue;
            Scenario selected=current(row);
            if(selected==null) continue;
            tickMechanics(level,row,selected);
        }
    }

    @SubscribeEvent
    public static void exposedDamage(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide||event.getEntity().getPersistentData().getLong(EXPOSED_UNTIL)<event.getEntity().level().getGameTime()) return;
        event.setAmount(event.getAmount()*1.5F);
    }

    public static void cleanup(MinecraftServer server,CompoundTag row) {
        ServerLevel level=EventRegions.level(server,row);
        if(level!=null) {
            livingBosses(level,row).forEach(com.siirio.jemworldbosstiers.api.BossEffectApi::clear);
            row.putBoolean(RESTORING,true);
            resumeRestoration(level,row);
        }
        if(!pendingRestoration(row)) {
            row.remove(WAVE_THREE);
            row.remove(WAVE_FIVE);
            row.remove(RESTORING);
        }
        row.remove(SET_PIECE_MOBS);
        row.remove(SET_PIECE_BOSSES);
        row.remove(ATTACK);
        row.remove(IMPACT);
        row.remove(COOLDOWNS);
        BossDebuffController.reset(row);
        row.remove(INITIAL_HEALTH);
        row.remove(LAST_ATTACK);
        row.remove(ATTACK_START);
        row.remove(RECOVERY_UNTIL);
        row.remove("setPieceAttackPhase");
        row.remove("setPiecePhaseUntil");
        row.remove(COVER);
        row.remove(FLOOR_QUEUE);
        row.remove(FLOOR_INDEX);
        resetCombo(row);
        row.remove("setPieceOriginX");
        row.remove("setPieceOriginZ");
        row.remove("setPieceSecondX");
        row.remove("setPieceSecondZ");
        row.remove("setPieceTargetX");
        row.remove("setPieceTargetZ");
        row.remove("setPieceFinalUsed");
        row.remove("setPieceCongregation");
    }

    private static int spawnScenario(ServerLevel level,CompoundTag row,List<ServerPlayer> players,Scenario scenario,int tier,int wave) {
        List<Spawn> spawns=spawns(scenario);
        int count=0;
        for(int index=0;index<spawns.size();index++) {
            Spawn spawn=spawns.get(index);
            BlockPos position=spawnPosition(level,row,scenario,index,spawns.size());
            Mob mob=create(level,spawn.id(),position,scenario);
            if(mob==null) continue;
            new EventSession(row).mark(mob);
            mob.getPersistentData().putUUID(BloodMoon.EVENT_ID,row.getUUID("id"));
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
            addUuid(row,"mobs",mob.getUUID());
            if(!level.addFreshEntity(mob)) {
                removeUuid(row,"mobs",mob.getUUID());
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
            addUuid(row,SET_PIECE_MOBS,mob.getUUID());
            if(spawn.boss()) addUuid(row,SET_PIECE_BOSSES,mob.getUUID());
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

    private static String spawnedKey(int wave) {
        return wave==3?WAVE_THREE_SPAWNED:WAVE_FIVE_SPAWNED;
    }

    private static void addUuid(CompoundTag row,String key,UUID id) {
        ListTag list=row.getList(key,Tag.TAG_STRING);
        list.add(StringTag.valueOf(id.toString()));
        row.put(key,list);
    }

    private static void removeUuid(CompoundTag row,String key,UUID id) {
        ListTag list=row.getList(key,Tag.TAG_STRING);
        list.remove(StringTag.valueOf(id.toString()));
        row.put(key,list);
    }

    private static void scaleHealth(Mob mob,double factor) {
        var health=mob.getAttribute(Attributes.MAX_HEALTH);
        if(health==null) return;
        health.removeModifier(HEALTH_SCALE);
        health.addPermanentModifier(new AttributeModifier(HEALTH_SCALE,"JEM set-piece health",factor-1,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static double healthFactor(CompoundTag row,int wave,Mob mob,Spawn spawn,List<Spawn> spawns) {
        double dps=row.getDouble("observedPartyDps");
        int bosses=(int)spawns.stream().filter(Spawn::boss).count();
        int allies=spawns.size()-bosses;
        if(dps<=0) return spawn.boss()?(bosses==1?2.5:1.8):.75;
        double duration=wave==BloodMoonWaves.WAVES?120:85;
        double share=spawn.boss()?.8/Math.max(1,bosses):.2/Math.max(1,allies);
        double reference=Math.max(1,mob.getMaxHealth());
        double factor=dps*duration*share/reference;
        return spawn.boss()?Mth.clamp(factor,wave==BloodMoonWaves.WAVES?2:1.5,wave==BloodMoonWaves.WAVES?8:6):Mth.clamp(factor,.5,1.5);
    }

    private static BlockPos spawnPosition(ServerLevel level,CompoundTag row,Scenario scenario,int index,int total) {
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

    private static void tickMechanics(ServerLevel level,CompoundTag row,Scenario scenario) {
        List<Mob> bosses=livingBosses(level,row);
        advanceArena(level,row,scenario,bosses);
        advanceIntroductions(level,row,bosses);
        BossDebuffController.tick(level,row,bosses);
        List<ServerPlayer> participants=new EventSession(row).active(level.getServer());
        bosses.forEach(boss->com.siirio.jemworldbosstiers.api.BossEffectApi.tick(boss,participants));
        if(scenario==Scenario.BONE_CATHEDRAL) activateCongregation(level,row);
        long now=level.getGameTime();
        if(row.contains(ATTACK_BLOCKS_UNTIL)&&row.getLong(ATTACK_BLOCKS_UNTIL)<=now) {
            restoreAttackBlocks(level,row);
            SmpData.get(level.getServer()).changed(row);
        }
        if(bosses.isEmpty()) return;
        if(bosses.stream().anyMatch(BloodMoonSetPieces::introducing)) return;
        if(!row.getString(ATTACK).isEmpty()) {
            Attack attack=Attack.valueOf(row.getString(ATTACK));
            long impact=row.getLong(IMPACT);
            long windupEnd=row.getLong("setPiecePhaseUntil");
            String phase=row.getString("setPieceAttackPhase");
            if(phase.equals("WINDUP")&&now>=windupEnd) {
                row.putString("setPieceAttackPhase","TELEGRAPH");
                row.putLong("setPiecePhaseUntil",impact-finalTelegraphLead(row,attack));
            } else if(phase.equals("TELEGRAPH")&&now>=windupEnd) row.putString("setPieceAttackPhase","LOCKED_PATTERN");
            if(phase.equals("WINDUP")) windup(level,attack,bosses);
            else if((now&1)==0) telegraph(level,row,attack,impact-now,bosses);
            if(now>=impact) execute(level,row,scenario,attack,bosses);
            return;
        }
        if(now<row.getLong(RECOVERY_UNTIL)) return;
        double ratio=healthRatio(level,row);
        if(row.getBoolean(COMBO_ACTIVE)) {
            if(startNextCombo(level,row,scenario,bosses)) return;
            finishCombo(level,row);
        } else if(now>=row.getLong(COMBO_NEXT)&&beginCombo(level,row,scenario,bosses,ratio)) {
            startNextCombo(level,row,scenario,bosses);
            return;
        }
        List<AttackSpec> available=attacks(scenario).stream()
                .filter(spec->requirements(row,spec.attack(),bosses,ratio))
                .filter(spec->row.getCompound(COOLDOWNS).getLong(spec.attack().name())<=now)
                .toList();
        if(available.isEmpty()) return;
        AttackSpec chosen=weighted(level,available,row.getString(LAST_ATTACK),row.getString(PREVIOUS_ATTACK));
        start(level,row,scenario,chosen.attack(),bosses,false);
    }

    private static void start(ServerLevel level,CompoundTag row,Scenario scenario,Attack attack,List<Mob> bosses,boolean combo) {
        long telegraph=combo?COMBO_TELEGRAPH_TICKS:telegraphTicks(attack);
        row.putString(ATTACK,attack.name());
        row.putLong(ATTACK_START,level.getGameTime());
        row.putLong(IMPACT,level.getGameTime()+telegraph);
        row.putString("setPieceAttackPhase","WINDUP");
        row.putLong("setPiecePhaseUntil",level.getGameTime()+(combo?Math.max(4,telegraph/4):Math.max(10,telegraph/4)));
        stageGeometry(level,row,attack,bosses);
        bosses.forEach(mob->mob.getNavigation().stop());
        if(attack==Attack.CRIMSON_DIVIDE) new EventSession(row).active(level.getServer()).forEach(player->player.sendSystemMessage(Component.literal("[Blood Moon] Ставьте блоки перед собой как стены!")));
        if(attack==Attack.TWIN_CATACLYSM) bosses.forEach(mob->mob.setInvulnerable(true));
        if(attack==Attack.COLLISION_COURSE) stageCollision(level,row,bosses);
        if(attack==Attack.CROSS_BLAST||attack==Attack.HUNTER_LATTICE) chooseTwinPattern(level,row);
        if(attack==Attack.ANCIENT_ROAR) row.putInt("setPieceCastParity",row.getInt("setPieceCastParity")+1);
        if(attack==Attack.ANCIENT_ROAR&&bosses.get(0) instanceof Warden warden) warden.setPose(Pose.ROARING);
        if(attack==Attack.RADIATION_SWEEP) row.putBoolean("setPieceReverse",level.random.nextBoolean());
        if(attack==Attack.END_COLLAPSE) chooseSafeQuarter(level,row);
        AttackGeometry geometry=geometry(row,attack,bosses,0);
        new EventSession(row).active(level.getServer()).forEach(player->EventNetwork.telegraph(player,row.getUUID("id"),geometry,(int)telegraph,ATTACK_TELEGRAPH_COLOR));
        play(level,bosses.get(0).blockPosition(),warningSound(attack),SoundEvents.WARDEN_SONIC_CHARGE,1.4F,.85F);
        SmpData.get(level.getServer()).changed(row);
    }

    private static void execute(ServerLevel level,CompoundTag row,Scenario scenario,Attack attack,List<Mob> bosses) {
        Mob source=bosses.get(0);
        AttackGeometry geometry=geometry(row,attack,bosses,1);
        boolean combo=row.getBoolean(COMBO_ACTIVE);
        int attackDuration=combo?COMBO_IMPACT_TICKS:attackVfxTicks(attack);
        new EventSession(row).active(level.getServer()).forEach(player->EventNetwork.attackVfx(player,row.getUUID("id"),geometry,attackDuration,ATTACK_TELEGRAPH_COLOR));
        physicalImpact(level,row,source,attack,geometry);
        for(ServerPlayer player:new EventSession(row).active(level.getServer())) {
            if(!geometry.contains(player.getX(),player.getZ())) continue;
            if(attack==Attack.CRIMSON_DIVIDE&&cover(level,row,source,player)) continue;
            if(attack==Attack.CRIMSON_DIVIDE) {
                player.stopUsingItem();
                player.getCooldowns().addCooldown(Items.SHIELD,100);
            }
            float damage=EventMobModifiers.signatureDamage(row.getInt("worldTier"),row.getInt("wave"),damage(attack));
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
        if(!row.getCompound(ATTACK_TERRAIN).isEmpty()) row.putLong(ATTACK_BLOCKS_UNTIL,level.getGameTime()+40);
        double ratio=healthRatio(level,row);
        AttackSpec spec=attacks(scenario).stream().filter(value->value.attack()==attack).findFirst().orElse(new AttackSpec(attack,600,390,1));
        CompoundTag cooldowns=row.getCompound(COOLDOWNS);
        cooldowns.putLong(attack.name(),level.getGameTime()+attackCooldown(ratio<.5?spec.aggressive():spec.normal()));
        row.put(COOLDOWNS,cooldowns);
        row.putString(PREVIOUS_ATTACK,row.getString(LAST_ATTACK));
        row.putString(LAST_ATTACK,attack.name());
        row.putLong(RECOVERY_UNTIL,level.getGameTime()+(combo?COMBO_GAP_TICKS:recoveryTicks(attack)));
        row.putString("setPieceAttackPhase","RECOVERY");
        if(attack==Attack.FINAL_COUNTDOWN||attack==Attack.LAST_MASS) row.putBoolean("setPieceFinalUsed",true);
        row.remove(ATTACK);
        row.remove(IMPACT);
        row.remove("setPiecePhaseUntil");
        SmpData.get(level.getServer()).changed(row);
    }

    private static void telegraph(ServerLevel level,CompoundTag row,Attack attack,long remaining,List<Mob> bosses) {
        long total=currentTelegraphTicks(row,attack);
        double progress=1-Mth.clamp(remaining/(double)total,0,1);
        AttackGeometry geometry=geometry(row,attack,bosses,progress);
        bosses.forEach(mob->face(mob,geometry.targetX(),geometry.targetZ(),12));
        if(attack==Attack.TENTACLE_CORRIDOR||attack==Attack.SCULK_BURIAL) rupture(level,row,geometry);
        if(remaining==FINAL_WARNING_TICKS||remaining==10||remaining==5) play(level,BlockPos.of(row.getLong("position")),warningSound(attack),SoundEvents.NOTE_BLOCK_HAT.value(),1.1F,remaining==5?1.65F:1.25F);
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

    private static void physicalImpact(ServerLevel level,CompoundTag row,Mob source,Attack attack,AttackGeometry geometry) {
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

    private static boolean cover(ServerLevel level,CompoundTag row,Mob source,ServerPlayer player) {
        Vec3 from=source.getEyePosition();
        Vec3 to=player.getEyePosition();
        BlockHitResult hit=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,source));
        if(hit.getType()!=HitResult.Type.BLOCK) return false;
        BlockPos pos=hit.getBlockPos();
        if(hit.getLocation().distanceToSqr(to)>=from.distanceToSqr(to)) return false;
        CompoundTag tracked=row.getCompound(COVER);
        String key=Long.toString(pos.asLong());
        if(!tracked.getBoolean(key)||level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()) return false;
        level.destroyBlock(pos,false,source);
        tracked.remove(key);
        row.put(COVER,tracked);
        return true;
    }

    private static void projectiles(ServerLevel level,CompoundTag row,Mob source,Attack attack) {
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
            arrow.setBaseDamage(EventMobModifiers.signatureDamage(row.getInt("worldTier"),row.getInt("wave"),6));
            arrow.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            EventMobModifiers.signature(arrow);
            level.addFreshEntity(arrow);
        }
    }

    private static void stageCollision(ServerLevel level,CompoundTag row,List<Mob> bosses) {
        if(bosses.size()<2) return;
        int centerZ=(EventRegions.minZ(row)+EventRegions.maxZ(row))/2;
        BlockPos left=surface(level,EventRegions.minX(row)+6,centerZ-9);
        BlockPos right=surface(level,EventRegions.maxX(row)-6,centerZ+9);
        bosses.get(0).getNavigation().moveTo(left.getX()+.5,left.getY(),left.getZ()+.5,1.4);
        bosses.get(1).getNavigation().moveTo(right.getX()+.5,right.getY(),right.getZ()+.5,1.4);
    }

    private static void chooseTwinPattern(ServerLevel level,CompoundTag row) {
        int previous=row.getInt("setPieceTwinPattern");
        int next=level.random.nextInt(8);
        if(next==previous) next=(next+1+level.random.nextInt(7))%8;
        row.putInt("setPieceTwinPattern",next);
    }

    private static void chooseSafeQuarter(ServerLevel level,CompoundTag row) {
        int previous=Math.floorMod(row.getInt("setPieceSafeQuarter"),4);
        int next=level.random.nextInt(4);
        if(next==previous) next=(next+1+level.random.nextInt(3))%4;
        row.putInt("setPieceSafeQuarter",next);
    }

    private static void stageGeometry(ServerLevel level,CompoundTag row,Attack attack,List<Mob> bosses) {
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
        row.putDouble("setPieceOriginX",source.getX());
        row.putDouble("setPieceOriginZ",source.getZ());
        row.putDouble("setPieceSecondX",second.getX());
        row.putDouble("setPieceSecondZ",second.getZ());
        row.putDouble("setPieceTargetX",targetX);
        row.putDouble("setPieceTargetZ",targetZ);
    }

    private static AttackGeometry geometry(CompoundTag row,Attack attack,List<Mob> bosses,double progress) {
        double originX=row.contains("setPieceOriginX")?row.getDouble("setPieceOriginX"):bosses.get(0).getX();
        double originZ=row.contains("setPieceOriginZ")?row.getDouble("setPieceOriginZ"):bosses.get(0).getZ();
        double secondX=row.contains("setPieceSecondX")?row.getDouble("setPieceSecondX"):originX;
        double secondZ=row.contains("setPieceSecondZ")?row.getDouble("setPieceSecondZ"):originZ;
        double targetX=row.contains("setPieceTargetX")?row.getDouble("setPieceTargetX"):(EventRegions.minX(row)+EventRegions.maxX(row)+1)/2.0;
        double targetZ=row.contains("setPieceTargetZ")?row.getDouble("setPieceTargetZ"):(EventRegions.minZ(row)+EventRegions.maxZ(row)+1)/2.0;
        int variant=attack==Attack.END_COLLAPSE?row.getInt("setPieceSafeQuarter"):attack==Attack.ANCIENT_ROAR?row.getInt("setPieceCastParity"):row.getInt("setPieceTwinPattern");
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
                row.getLong(ATTACK_START),windup,1,progress,variant,row.getBoolean("setPieceReverse"));
    }

    private static void rupture(ServerLevel level,CompoundTag row,AttackGeometry geometry) {
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

    private static void activateCongregation(ServerLevel level,CompoundTag row) {
        double ratio=healthRatio(level,row);
        int target=ratio<=.25?6:ratio<=.5?4:ratio<=.75?2:0;
        int active=row.getInt("setPieceCongregation");
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
        row.putInt("setPieceCongregation",target);
    }

    private static void initializeCooldowns(ServerLevel level,CompoundTag row,Scenario scenario) {
        CompoundTag cooldowns=new CompoundTag();
        long now=level.getGameTime();
        attacks(scenario).forEach(spec->cooldowns.putLong(spec.attack().name(),now+attackCooldown(Math.max(80,spec.normal()/2))));
        row.put(COOLDOWNS,cooldowns);
        resetCombo(row);
        row.putLong(COMBO_NEXT,now+comboInterval(level));
    }

    private static boolean beginCombo(ServerLevel level,CompoundTag row,Scenario scenario,List<Mob> bosses,double ratio) {
        List<Attack> pool=new ArrayList<>(attacks(scenario).stream()
                .map(AttackSpec::attack)
                .filter(BloodMoonSetPieces::comboSafe)
                .filter(attack->requirements(row,attack,bosses,ratio))
                .filter(attack->!attack.name().equals(row.getString(LAST_ATTACK))&&!attack.name().equals(row.getString(PREVIOUS_ATTACK)))
                .toList());
        if(pool.size()<COMBO_ATTACK_COUNT) {
            pool=new ArrayList<>(attacks(scenario).stream()
                    .map(AttackSpec::attack)
                    .filter(BloodMoonSetPieces::comboSafe)
                    .filter(attack->requirements(row,attack,bosses,ratio))
                    .toList());
        }
        if(pool.size()<COMBO_ATTACK_COUNT) {
            row.putLong(COMBO_NEXT,level.getGameTime()+comboInterval(level));
            return false;
        }
        for(int index=pool.size()-1;index>0;index--) {
            int replacement=level.random.nextInt(index+1);
            Attack attack=pool.get(index);
            pool.set(index,pool.get(replacement));
            pool.set(replacement,attack);
        }
        ListTag queue=new ListTag();
        for(int index=0;index<COMBO_ATTACK_COUNT;index++) queue.add(StringTag.valueOf(pool.get(index).name()));
        row.put(COMBO_QUEUE,queue);
        row.putInt(COMBO_INDEX,0);
        row.putBoolean(COMBO_ACTIVE,true);
        row.putLong(COMBO_NEXT,level.getGameTime()+comboInterval(level));
        return true;
    }

    private static boolean startNextCombo(ServerLevel level,CompoundTag row,Scenario scenario,List<Mob> bosses) {
        ListTag queue=row.getList(COMBO_QUEUE,Tag.TAG_STRING);
        int index=row.getInt(COMBO_INDEX);
        if(index>=queue.size()) return false;
        Attack attack=Attack.valueOf(queue.getString(index));
        row.putInt(COMBO_INDEX,index+1);
        start(level,row,scenario,attack,bosses,true);
        return true;
    }

    private static void finishCombo(ServerLevel level,CompoundTag row) {
        row.remove(COMBO_QUEUE);
        row.remove(COMBO_INDEX);
        row.remove(COMBO_ACTIVE);
        SmpData.get(level.getServer()).changed(row);
    }

    private static void resetCombo(CompoundTag row) {
        row.remove(COMBO_QUEUE);
        row.remove(COMBO_INDEX);
        row.remove(COMBO_NEXT);
        row.remove(COMBO_ACTIVE);
    }

    private static boolean comboSafe(Attack attack) {
        return attack!=Attack.TWIN_CATACLYSM&&attack!=Attack.COLLISION_COURSE&&attack!=Attack.CRIMSON_DIVIDE
                &&attack!=Attack.FINAL_COUNTDOWN&&attack!=Attack.FALLING_FORGE&&attack!=Attack.LAST_MASS;
    }

    private static long comboInterval(ServerLevel level) {
        return COMBO_MIN_INTERVAL_TICKS+level.random.nextInt(COMBO_INTERVAL_VARIANCE_TICKS);
    }

    private static long currentTelegraphTicks(CompoundTag row,Attack attack) {
        if(row.contains(ATTACK_START)&&row.contains(IMPACT)) return Math.max(1,row.getLong(IMPACT)-row.getLong(ATTACK_START));
        return telegraphTicks(attack);
    }

    private static long finalTelegraphLead(CompoundTag row,Attack attack) {
        return Math.max(4,Math.min(12,currentTelegraphTicks(row,attack)/3));
    }

    private static boolean requirements(CompoundTag row,Attack attack,List<Mob> bosses,double ratio) {
        if((attack==Attack.TRINITY_ASSAULT&&bosses.size()<3)||(attack==Attack.TWIN_CATACLYSM&&bosses.size()<2)) return false;
        if((attack==Attack.FINAL_COUNTDOWN||attack==Attack.LAST_MASS)&&(ratio>=.25||row.getBoolean("setPieceFinalUsed"))) return false;
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

    private static double healthRatio(ServerLevel level,CompoundTag row) {
        double initial=row.getDouble(INITIAL_HEALTH);
        if(initial<=0) return 1;
        return Mth.clamp(livingBosses(level,row).stream().mapToDouble(LivingEntity::getHealth).sum()/initial,0,1);
    }

    private static List<Mob> livingBosses(ServerLevel level,CompoundTag row) {
        return entities(level,row.getList(SET_PIECE_BOSSES,Tag.TAG_STRING));
    }

    private static List<Mob> livingSetPieces(ServerLevel level,CompoundTag row) {
        return entities(level,row.getList(SET_PIECE_MOBS,Tag.TAG_STRING));
    }

    private static List<Mob> entities(ServerLevel level,ListTag ids) {
        List<Mob> result=new ArrayList<>();
        for(Tag tag:ids) {
            Entity entity=level.getEntity(UUID.fromString(tag.getAsString()));
            if(entity instanceof Mob mob&&mob.isAlive()) result.add(mob);
        }
        return result;
    }

    private static void advanceIntroductions(ServerLevel level,CompoundTag row,List<Mob> bosses) {
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

    private static void advanceArena(ServerLevel level,CompoundTag row,Scenario scenario,List<Mob> bosses) {
        ListTag queue=row.getList(FLOOR_QUEUE,Tag.TAG_LONG);
        int index=row.getInt(FLOOR_INDEX);
        if(queue.isEmpty()||index>=queue.size()) return;
        FloorPalette palette=floorPalette(scenario);
        int centerX=(EventRegions.minX(row)+EventRegions.maxX(row))/2;
        int centerZ=(EventRegions.minZ(row)+EventRegions.maxZ(row))/2;
        double radius=Math.max(1,Math.hypot((EventRegions.maxX(row)-EventRegions.minX(row))/2.0,(EventRegions.maxZ(row)-EventRegions.minZ(row))/2.0));
        int end=Math.min(queue.size(),index+FLOOR_BLOCKS_PER_TICK);
        BlockPos last=null;
        for(;index<end;index++) {
            BlockPos floor=BlockPos.of(((LongTag)queue.get(index)).getAsLong());
            BlockPos cover=floor.above();
            if(thinCover(level.getBlockState(cover))) {
                setTemporary(level,row,cover,Blocks.AIR.defaultBlockState());
            }
            double progress=Math.hypot(floor.getX()-centerX,floor.getZ()-centerZ)/radius;
            setTemporary(level,row,floor,(progress<=FLOOR_CENTER_RADIUS?palette.center():progress<=FLOOR_MIDDLE_RADIUS?palette.middle():palette.outer()).defaultBlockState());
            last=floor;
        }
        row.putInt(FLOOR_INDEX,index);
        if(last!=null&&(level.getGameTime()&3)==0) {
            level.playSound(null,last,SoundEvents.SCULK_BLOCK_SPREAD,SoundSource.BLOCKS,.9F,.75F+level.random.nextFloat()*.2F);
            level.sendParticles(ParticleTypes.SCULK_SOUL,last.getX()+.5,last.getY()+1.1,last.getZ()+.5,3,.8,.1,.8,.02);
        }
        SmpData.get(level.getServer()).changed(row);
    }

    private static void buildArena(ServerLevel level,CompoundTag row,Scenario scenario,int wave) {
        if(row.getInt(ARENA_WAVE)==wave&&!row.getCompound(TERRAIN).isEmpty()) return;
        if(pendingRestoration(row)) {
            row.putBoolean(RESTORING,true);
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
        ListTag queue=new ListTag();
        floors.forEach(pos->queue.add(LongTag.valueOf(pos.asLong())));
        row.put(FLOOR_QUEUE,queue);
        row.putInt(FLOOR_INDEX,0);
        row.putInt(ARENA_WAVE,wave);
        SmpData.get(level.getServer()).changed(row);
    }

    private static BlockPos visibleFloor(ServerLevel level,int x,int z) {
        BlockPos visible=surface(level,x,z).below();
        return thinCover(level.getBlockState(visible))?visible.below():visible;
    }

    private static boolean thinCover(BlockState state) {
        return state.getBlock() instanceof SnowLayerBlock||state.canBeReplaced()&&state.getFluidState().isEmpty()&&!state.isAir();
    }

    private static void setTemporary(ServerLevel level,CompoundTag row,BlockPos pos,BlockState temporary) {
        BlockState current=level.getBlockState(pos);
        if(current.hasBlockEntity()||!current.getFluidState().isEmpty()||current.is(Blocks.BEDROCK)||current.is(Blocks.BARRIER)) return;
        if(saveChange(level,row,TERRAIN,pos,current,temporary)) level.setBlock(pos,temporary,3);
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

    private static boolean saveChange(ServerLevel level,CompoundTag row,String field,BlockPos pos,BlockState original,BlockState temporary) {
        CompoundTag snapshot=row.getCompound(field);
        String key=Long.toString(pos.asLong());
        CompoundTag saved=snapshot.getCompound(key);
        if(!saved.isEmpty()) {
            if(saved.contains(ORIGINAL,Tag.TAG_COMPOUND)) {
                if(saved.contains(TEMPORARY,Tag.TAG_COMPOUND)
                        &&!original.equals(NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),saved.getCompound(TEMPORARY)))) return false;
            } else if(!legacyTemporary(row,field,original)) return false;
        }
        CompoundTag entry=new CompoundTag();
        entry.put(ORIGINAL,saved.contains(ORIGINAL,Tag.TAG_COMPOUND)?saved.getCompound(ORIGINAL).copy()
                :saved.isEmpty()?NbtUtils.writeBlockState(original):saved.copy());
        entry.put(TEMPORARY,NbtUtils.writeBlockState(temporary));
        snapshot.put(key,entry);
        row.put(field,snapshot);
        return true;
    }

    private static void resumeRestoration(ServerLevel level,CompoundTag row) {
        restoreAttackBlocks(level,row);
        if(row.getCompound(ATTACK_TERRAIN).isEmpty()) restoreTerrain(level,row);
        if(!pendingRestoration(row)) {
            row.remove(RESTORING);
            if(!row.getBoolean("combatStarted")||!row.getString("state").equals("ACTIVE")) {
                row.remove(WAVE_THREE);
                row.remove(WAVE_FIVE);
            }
        }
    }

    private static boolean pendingRestoration(CompoundTag row) {
        return !row.getCompound(ATTACK_TERRAIN).isEmpty()||!row.getCompound(TERRAIN).isEmpty();
    }

    private static void restoreSnapshot(ServerLevel level,CompoundTag row,String field) {
        CompoundTag snapshot=row.getCompound(field);
        for(String key:new ArrayList<>(snapshot.getAllKeys())) {
            BlockPos pos=BlockPos.of(Long.parseLong(key));
            if(!level.hasChunkAt(pos)) continue;
            CompoundTag saved=snapshot.getCompound(key);
            BlockState original=saved.contains(ORIGINAL,Tag.TAG_COMPOUND)
                    ?NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),saved.getCompound(ORIGINAL))
                    :NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),saved);
            BlockState current=level.getBlockState(pos);
            boolean owned=saved.contains(TEMPORARY,Tag.TAG_COMPOUND)
                    ?current.equals(NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),saved.getCompound(TEMPORARY)))
                    :legacyTemporary(row,field,current);
            if(owned&&!current.equals(original)) level.setBlock(pos,original,3);
            snapshot.remove(key);
        }
        row.put(field,snapshot);
    }

    private static boolean legacyTemporary(CompoundTag row,String field,BlockState current) {
        if(field.equals(ATTACK_TERRAIN)) return current.is(block("deep_dark_regrowth:overloaded_sculk"));
        if(current.isAir()) return true;
        int wave=row.getInt(ARENA_WAVE);
        Scenario scenario=scenario(row.getString(wave==3?WAVE_THREE:WAVE_FIVE));
        if(scenario==null) return false;
        FloorPalette palette=floorPalette(scenario);
        return current.is(palette.outer())||current.is(palette.middle())||current.is(palette.center());
    }

    private static void restoreTerrain(ServerLevel level,CompoundTag row) {
        restoreSnapshot(level,row,TERRAIN);
        if(row.getCompound(TERRAIN).isEmpty()) {
            row.remove(TERRAIN);
            row.remove(ARENA_WAVE);
            row.remove(FLOOR_QUEUE);
            row.remove(FLOOR_INDEX);
        }
    }

    private static void restoreAttackBlocks(ServerLevel level,CompoundTag row) {
        restoreSnapshot(level,row,ATTACK_TERRAIN);
        if(row.getCompound(ATTACK_TERRAIN).isEmpty()) {
            row.remove(ATTACK_TERRAIN);
            row.remove(ATTACK_BLOCKS);
            row.remove(ATTACK_BLOCKS_UNTIL);
        }
    }

    private static void setAttackTemporary(ServerLevel level,CompoundTag row,BlockPos pos,Block block) {
        BlockState current=level.getBlockState(pos);
        if(current.hasBlockEntity()||!current.getFluidState().isEmpty()||current.is(Blocks.BEDROCK)||current.is(Blocks.BARRIER)) return;
        BlockState temporary=block.defaultBlockState();
        CompoundTag terrain=row.getCompound(ATTACK_TERRAIN);
        String key=Long.toString(pos.asLong());
        boolean added=!terrain.contains(key);
        if(saveChange(level,row,ATTACK_TERRAIN,pos,current,temporary)) {
            if(added) {
                ListTag blocks=row.getList(ATTACK_BLOCKS,Tag.TAG_LONG);
                blocks.add(LongTag.valueOf(pos.asLong()));
                row.put(ATTACK_BLOCKS,blocks);
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

    private static Scenario current(CompoundTag row) {
        if(row.getInt("wave")==3) return scenario(row.getString(WAVE_THREE));
        if(row.getInt("wave")==BloodMoonWaves.WAVES) return scenario(row.getString(WAVE_FIVE));
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
