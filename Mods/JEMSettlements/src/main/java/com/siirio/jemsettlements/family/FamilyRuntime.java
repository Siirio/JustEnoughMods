package com.siirio.jemsettlements.family;

import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.town.SettlementSavedData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;

/** Universal modpack family authority. Town membership is only an optional mood adapter. */
public final class FamilyRuntime {
    private static final long DAY = 24_000L;
    private static final int IDENTITIES_PER_TICK = 4, ACTORS_PER_CYCLE = 8;
    private static final TagKey<Structure> VILLAGE_STRUCTURES = TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "village"));
    private record PendingIdentity(ResourceKey<Level> dimension, UUID entityId) { }
    private static final class PendingRecruitSpawn {
        final ResourceKey<Level> dimension; final BlockPos bell; final int target; int next;
        PendingRecruitSpawn(ResourceKey<Level> dimension,BlockPos bell,int next,int target){this.dimension=dimension;this.bell=bell.immutable();this.next=next;this.target=target;}
    }
    private static final ArrayDeque<PendingIdentity> PENDING_IDENTITIES = new ArrayDeque<>();
    private static final ArrayDeque<PendingRecruitSpawn> PENDING_RECRUITS = new ArrayDeque<>();
    private static final Set<UUID> PENDING_IDENTITY_IDS = new HashSet<>();
    private static final Map<ResourceKey<Level>, Integer> CYCLE_CURSORS = new HashMap<>();
    private static final Map<ResourceKey<Level>, LinkedHashMap<UUID, LivingEntity>> LOADED_ACTORS = new HashMap<>();
    private record ComplaintState(String reason, long nextReminder) { }
    private static final Map<UUID, ComplaintState> COMPLAINTS = new HashMap<>();
    private FamilyRuntime() { }

    public static boolean supported(Entity entity) { return entity instanceof Villager || entity instanceof ZombieVillager || entity instanceof LivingEntity living && ModList.get().isLoaded("recruits") && RecruitsFamilySupport.isRecruit(living); }
    private static boolean familyActor(Entity entity) { return entity instanceof Villager || entity instanceof LivingEntity living && ModList.get().isLoaded("recruits") && RecruitsFamilySupport.isRecruit(living); }

    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity living) || !supported(living)) return;
        if (familyActor(living)) LOADED_ACTORS.computeIfAbsent(level.dimension(), ignored -> new LinkedHashMap<>())
                .put(living.getUUID(), living);
        if (ModList.get().isLoaded("recruits") && RecruitsFamilySupport.isRecruit(living)) {
            if(!living.getPersistentData().contains("JemRecruitProvenance")){boolean table=RecruitsFamilySupport.inferTableOrigin(living);living.getPersistentData().putString("JemRecruitProvenance",table?"table":"independent");}
        }
        if (PENDING_IDENTITY_IDS.add(living.getUUID())) PENDING_IDENTITIES.addLast(new PendingIdentity(level.dimension(), living.getUUID()));
    }
    @SubscribeEvent public static void left(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var actors = LOADED_ACTORS.get(level.dimension());
        if (actors != null) actors.remove(event.getEntity().getUUID());
        COMPLAINTS.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void converted(LivingConversionEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return; LivingEntity from=event.getEntity(),to=event.getOutcome(); if(!supported(from)||!supported(to))return;
        VillagerFamilySavedData.get(level.getServer()).transfer(from, to);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !supported(event.getEntity())) return;
        var actors = LOADED_ACTORS.get(level.dimension()); if (actors != null) actors.remove(event.getEntity().getUUID());
        COMPLAINTS.remove(event.getEntity().getUUID()); VillagerFamilySavedData data = VillagerFamilySavedData.get(level.getServer());
        FamilyRecord dead = data.byEntity(event.getEntity().getUUID()).orElse(null); if (dead == null) return; long now = level.getGameTime(), cooldown = remarriageCooldown(dead.personId(), now);
        if (dead.spouse() != null) data.byPerson(dead.spouse()).ifPresent(spouse -> spouse.widowed(now, cooldown)); dead.died(now, cooldown); data.changed();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        LOADED_ACTORS.clear(); COMPLAINTS.clear(); CYCLE_CURSORS.clear();
        PENDING_IDENTITIES.clear(); PENDING_IDENTITY_IDS.clear(); PENDING_RECRUITS.clear();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void interact(PlayerInteractEvent.EntityInteract event) { interaction(event.getEntity(), event.getTarget(), event); }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) { interaction(event.getEntity(), event.getTarget(), event); }
    private static void interaction(net.minecraft.world.entity.player.Player actor, Entity target, net.minecraftforge.eventbus.api.Event event) {
        if (!(actor instanceof ServerPlayer player) || !(target instanceof LivingEntity living) || !familyActor(target)) return;
        sendView(player, living, false);
        if (target instanceof Villager villager && (villager.getVillagerData().getProfession() == VillagerProfession.NONE || villager.getVillagerData().getProfession() == VillagerProfession.NITWIT)) {
            sendView(player, living, true); event.setCanceled(true);
            if (event instanceof PlayerInteractEvent interaction) interaction.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    public static void request(ServerPlayer player, int entityId) { Entity entity = player.serverLevel().getEntity(entityId); if (entity instanceof LivingEntity living && familyActor(entity) && player.distanceToSqr(entity) <= 16*16) sendView(player, living, true); }
    private static void sendView(ServerPlayer player, LivingEntity entity, boolean open) {
        VillagerFamilySavedData data = VillagerFamilySavedData.get(player.getServer()); FamilyRecord record = initializeIdentity(entity); FamilyHomeService.Status status = FamilyHomeService.inspect(player.serverLevel(), entity, record, data); data.changed();
        CompoundTag view = familyTag(data, record); view.merge(FamilyHomeService.snapshot(player.serverLevel(), record, status)); SettlementNetwork.sendFamilyView(player, entity.getId(), open, view);
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        processIdentities(event.getServer());
        processRecruitSpawns(event.getServer());
        if (event.getServer().getTickCount()%100 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) cycle(level);
    }
    private static void processIdentities(MinecraftServer server) {
        for (int count = 0; count < IDENTITIES_PER_TICK && !PENDING_IDENTITIES.isEmpty(); count++) {
            PendingIdentity pending = PENDING_IDENTITIES.removeFirst(); PENDING_IDENTITY_IDS.remove(pending.entityId());
            ServerLevel level = server.getLevel(pending.dimension()); if (level == null) continue;
            Entity entity = level.getEntity(pending.entityId());
            if (entity instanceof LivingEntity living && living.isAlive() && supported(living)) initializeIdentity(living);
        }
    }
    private static FamilyRecord initializeIdentity(LivingEntity living) {
        VillagerFamilySavedData data = VillagerFamilySavedData.get(living.getServer()); FamilyRecord record = data.ensure(living);
        if (living instanceof Villager villager && villager.getVillagerData().getProfession() == VillagerProfession.NITWIT) record.eternalSingle(true);
        if (ModList.get().isLoaded("recruits") && RecruitsFamilySupport.isRecruit(living)) {
            boolean table=living.getPersistentData().getString("JemRecruitProvenance").equals("table");record.tableOrigin(table);record.eternalSingle(!table);
        }
        data.changed(); return record;
    }
    private static void processRecruitSpawns(MinecraftServer server) {
        PendingRecruitSpawn pending=PENDING_RECRUITS.pollFirst(); if(pending==null)return;
        ServerLevel level=server.getLevel(pending.dimension); if(level==null||!level.hasChunkAt(pending.bell)){PENDING_RECRUITS.addLast(pending);return;}
        ResourceLocation[] types={new ResourceLocation("recruits","recruit"),new ResourceLocation("recruits","bowman"),new ResourceLocation("recruits","recruit_shieldman")};
        EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(types[pending.next%types.length]);Entity made=type.create(level);
        if(made instanceof LivingEntity living){BlockPos at=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pending.bell.offset((pending.next%3)-1,0,(pending.next/3)-1));living.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);RecruitsFamilySupport.initializeSpawn(living);living.getPersistentData().putString("JemRecruitProvenance","village");level.addFreshEntity(living);}
        pending.next++;if(pending.next<pending.target)PENDING_RECRUITS.addLast(pending);
    }
    private static void cycle(ServerLevel level) {
        VillagerFamilySavedData data = VillagerFamilySavedData.get(level.getServer());
        var indexed = LOADED_ACTORS.computeIfAbsent(level.dimension(), ignored -> new LinkedHashMap<>());
        indexed.values().removeIf(entity -> entity.isRemoved() || !entity.isAlive() || entity.level() != level);
        List<LivingEntity> actors = new ArrayList<>(indexed.values());
        if (actors.isEmpty()) { CYCLE_CURSORS.remove(level.dimension()); return; }
        int start = Math.floorMod(CYCLE_CURSORS.getOrDefault(level.dimension(), 0), actors.size());
        int count = Math.min(ACTORS_PER_CYCLE, actors.size()), pairBudget = 2, birthBudget = 1;
        for (int offset = 0; offset < count; offset++) {
            LivingEntity entity = actors.get((start + offset) % actors.size());
            FamilyRecord record = initializeIdentity(entity); FamilyHomeService.Status home = FamilyHomeService.inspect(level, entity, record, data); syncSettlementMood(level, entity, home);
            updateComplaint(entity, home.complaint(), level.getGameTime());
            if (record.spouse() == null && pairBudget > 0 && eligibleCourtship(entity, record, home, level.getGameTime())) {
                LivingEntity partner = findPartner(level, entity, record, actors, data); if (partner != null) { marry(level, entity, partner, data); pairBudget--; }
            }
            if(record.spouse()!=null&&record.orientation()==FamilyRecord.Orientation.SAME_SEX&&Math.floorMod(level.getGameTime()+entity.getId(),600)==0)level.sendParticles(ParticleTypes.HEART,entity.getX(),entity.getY()+1.2,entity.getZ(),2,.25,.25,.25,.01);
            if (record.spouse() != null && birthBudget > 0 && tryBirth(level, entity, record, data)) birthBudget--;
        }
        CYCLE_CURSORS.put(level.dimension(), (start + count) % actors.size());
        data.changed();
    }
    private static void updateComplaint(LivingEntity entity, String reason, long now) {
        ComplaintState prior = COMPLAINTS.get(entity.getUUID());
        if (reason.isEmpty()) {
            if (prior != null) SettlementNetwork.sendFamilyComplaint(entity, "");
            COMPLAINTS.remove(entity.getUUID());
            return;
        }
        if (prior == null || !prior.reason().equals(reason) || now >= prior.nextReminder()) {
            SettlementNetwork.sendFamilyComplaint(entity, reason);
            COMPLAINTS.put(entity.getUUID(), new ComplaintState(reason, now + 2_400L));
        }
    }
    private static void syncSettlementMood(ServerLevel level, LivingEntity entity, FamilyHomeService.Status status) {
        var mood = SettlementSavedData.get(level.getServer()).resident(entity.getUUID()).orElse(null); if (mood == null) return;
        CompoundTag observation = mood.needObservation(); observation.putBoolean("FamilyHome", true); observation.putBoolean("HasBed", status.bed()); observation.putBoolean("HasChest", status.chest()); observation.putBoolean("HasWorkstation", status.workstation()); observation.putBoolean("HasLight", status.light()); mood.needObservation(observation);
        int resources=(status.bed()?0:15)+(status.chest()?0:15)+(!status.child()&&!status.workstation()?15:0); mood.familyHomePenalty(resources,status.light()?0:15); SettlementSavedData.get(level.getServer()).changed();
    }
    private static boolean eligibleCourtship(LivingEntity entity, FamilyRecord record, FamilyHomeService.Status home, long now) { return !entity.isBaby() && home.complete() && home.happiness() >= 70 && !record.eternalSingle() && !record.dead() && record.remarriageAfter() <= now; }
    private static LivingEntity findPartner(ServerLevel level, LivingEntity self, FamilyRecord record, List<LivingEntity> actors, VillagerFamilySavedData data) {
        for (LivingEntity other : actors) { if (other == self || self.distanceToSqr(other)>12*12 || other.isBaby()) continue; FamilyRecord candidate = data.ensure(other); if (candidate.spouse()!=null || candidate.eternalSingle() || candidate.dead() || candidate.remarriageAfter()>level.getGameTime() || !compatible(record,candidate) || related(record,candidate,data)) continue; if (!FamilyHomeService.inspect(level,other,candidate,data).complete()) continue; return other; } return null;
    }
    private static boolean compatible(FamilyRecord a, FamilyRecord b) { if (a.orientation()!=b.orientation()) return false; return a.orientation()==FamilyRecord.Orientation.STRAIGHT ? a.sex()!=b.sex() : a.sex()==b.sex(); }
    private static boolean related(FamilyRecord a, FamilyRecord b, VillagerFamilySavedData data) { if (a.parents().contains(b.personId()) || b.parents().contains(a.personId())) return true; for (UUID parent : a.parents()) if (b.parents().contains(parent)) return true; for (UUID parent : a.parents()) if (data.byPerson(parent).map(FamilyRecord::parents).orElse(Set.of()).contains(b.personId())) return true; for (UUID parent : b.parents()) if (data.byPerson(parent).map(FamilyRecord::parents).orElse(Set.of()).contains(a.personId())) return true; return false; }
    private static void marry(ServerLevel level, LivingEntity first, LivingEntity second, VillagerFamilySavedData data) { FamilyRecord a=data.ensure(first),b=data.ensure(second);a.spouse(b.personId());b.spouse(a.personId());level.sendParticles(ParticleTypes.HEART,first.getX(),first.getY()+1.2,first.getZ(),7,.45,.45,.45,.02);level.sendParticles(ParticleTypes.HEART,second.getX(),second.getY()+1.2,second.getZ(),7,.45,.45,.45,.02); }
    private static boolean tryBirth(ServerLevel level, LivingEntity first, FamilyRecord a, VillagerFamilySavedData data) {
        if (a.orientation()!=FamilyRecord.Orientation.STRAIGHT || a.sex()!=FamilyRecord.Sex.FEMALE || a.nextBirth()>level.getGameTime()) return false; FamilyRecord b=data.byPerson(a.spouse()).orElse(null); if(b==null||b.dead()||b.nextBirth()>level.getGameTime())return false;
        Entity raw=level.getEntity(b.entityId()); if(!(raw instanceof LivingEntity second)||first.distanceToSqr(second)>4*4||first.isBaby()||second.isBaby())return false;
        if(!FamilyHomeService.inspect(level,first,a,data).complete()||!FamilyHomeService.inspect(level,second,b,data).complete()||!ready(first)||!ready(second))return false;
        Villager child=EntityType.VILLAGER.create(level);if(child==null)return false; child.setVillagerData(child.getVillagerData().setType(parentType(first,second)));child.setBaby(true);child.moveTo((first.getX()+second.getX())/2,Math.max(first.getY(),second.getY()),(first.getZ()+second.getZ())/2,first.getYRot(),0);if(!level.addFreshEntity(child))return false;
        FamilyRecord born=data.ensure(child);born.addParent(a.personId());born.addParent(b.personId());a.nextBirth(level.getGameTime()+6000);b.nextBirth(level.getGameTime()+6000);if(first instanceof Villager villager)villager.eatAndDigestFood();if(second instanceof Villager villager)villager.eatAndDigestFood();
        if(first instanceof Villager firstVillager&&second instanceof Villager secondVillager)com.siirio.jemsettlements.runtime.SettlementRuntime.registerBornChild(level,firstVillager,secondVillager,child);level.sendParticles(ParticleTypes.HEART,child.getX(),child.getY()+.6,child.getZ(),9,.5,.3,.5,.03);return true;
    }
    private static boolean ready(LivingEntity entity){return entity instanceof Villager villager?villager.canBreed():ModList.get().isLoaded("recruits")&&RecruitsFamilySupport.ready(entity);}
    private static VillagerType parentType(LivingEntity first,LivingEntity second){if(first instanceof Villager v)return v.getVillagerData().getType();if(second instanceof Villager v)return v.getVillagerData().getType();return VillagerType.PLAINS;}
    private static long remarriageCooldown(UUID id,long now){return DAY*(1L+Math.floorMod(id.hashCode()^Long.hashCode(now),10));}

    public static CompoundTag familyTag(VillagerFamilySavedData data,FamilyRecord record){CompoundTag tag=new CompoundTag();tag.putBoolean("JemFamily",true);tag.putString("JemFamilyName",record.name());tag.putString("JemGender",record.sex().name());tag.putString("JemOrientation",record.orientation().name());tag.putInt("JemFamilyHappiness",record.happiness());tag.putBoolean("JemEternalSingle",record.eternalSingle());if(record.spouse()!=null)data.byPerson(record.spouse()).ifPresent(spouse->{tag.putString("JemSpouseName",spouse.name());tag.putString("JemFamilyRole",record.sex()==FamilyRecord.Sex.MALE?"HUSBAND":"WIFE");});return tag;}

    public static boolean spouseBedExemption(LivingEntity entity,BlockPos bed){if(!(entity.level() instanceof ServerLevel level))return false;VillagerFamilySavedData data=VillagerFamilySavedData.get(level.getServer());FamilyRecord record=data.byEntity(entity.getUUID()).orElse(null);if(record==null||record.spouse()==null)return false;FamilyRecord spouse=data.byPerson(record.spouse()).orElse(null);if(spouse==null||spouse.bed()==null||!spouse.bed().dimension().equals(level.dimension()))return false;BlockPos spouseBed=spouse.bed().pos();int radius=com.siirio.jemsettlements.happiness.HousingConditionConfig.BED_RADIUS.get();boolean foundSpouse=false;for(BlockPos pos:BlockPos.betweenClosed(bed.offset(-radius,0,-radius),bed.offset(radius,0,radius))){if(!level.hasChunkAt(pos)||!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BedBlock))continue;if(pos.distSqr(spouseBed)<=1)foundSpouse=true;else if(pos.distSqr(bed)>1)return false;}return foundSpouse;}

    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) { if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide||event.player.tickCount%200!=0||!ModList.get().isLoaded("recruits")||!(event.player instanceof ServerPlayer player))return;seedVillage(player.serverLevel(),player.blockPosition()); }
    private static void seedVillage(ServerLevel level,BlockPos player){var bell=level.getPoiManager().findClosest(holder->holder.is(PoiTypes.MEETING),player,64,PoiManager.Occupancy.ANY);if(bell.isEmpty())return;if(!level.structureManager().getStructureWithPieceAt(bell.get(),VILLAGE_STRUCTURES).isValid())return;String key=level.dimension().location()+":"+bell.get().asLong();VillagerFamilySavedData data=VillagerFamilySavedData.get(level.getServer());if(!data.seedVillage(key))return;
        AABB area=new AABB(bell.get()).inflate(48,16,48);int existing=(int)level.getEntitiesOfClass(LivingEntity.class,area,entity->ModList.get().isLoaded("recruits")&&RecruitsFamilySupport.isRecruit(entity)).stream().count();int target=4+Math.floorMod(key.hashCode(),2);
        if(existing<target)PENDING_RECRUITS.addLast(new PendingRecruitSpawn(level.dimension(),bell.get(),existing,target));}
}
