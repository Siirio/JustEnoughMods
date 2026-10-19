package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;

/** Actual entity, POI and navigation evidence; no generated workers or client authority. */
public final class TownMetricsService {
    private static final java.util.ArrayDeque<PathRequest> PATHS = new java.util.ArrayDeque<>();
    private static final java.util.Set<PathRequest> QUEUED = new java.util.HashSet<>();
    private static final java.util.Map<PathRequest, PathFact> PATH_FACTS = new java.util.LinkedHashMap<>();
    private record PathRequest(java.util.UUID resident, GlobalPos site) { }
    private record PathFact(GlobalPos site, long tick, boolean reachable) { }
    private TownMetricsService() { }
    public static void clear() { PATHS.clear(); QUEUED.clear(); PATH_FACTS.clear(); }
    /** Globally at most four native path probes per tick; repeated combat recomputes cannot multiply work. */
    public static void tickPaths(net.minecraft.server.MinecraftServer server) {
        for (int budget=0;budget<4&&!PATHS.isEmpty();budget++) {
            var request=PATHS.removeFirst(); QUEUED.remove(request);
            var level=server.getLevel(request.site().dimension());
            if(level==null||!level.hasChunkAt(request.site().pos())||!(level.getEntity(request.resident()) instanceof Villager villager)||!villager.isAlive())continue;
            boolean reachable=false;
            if(villager.blockPosition().distSqr(request.site().pos())<=48L*48L) {
                var path=villager.getNavigation().createPath(request.site().pos(),1);
                reachable=path!=null&&path.canReach();
            }
            PATH_FACTS.put(request,new PathFact(request.site(),server.getTickCount(),reachable));
            while(PATH_FACTS.size()>4096)PATH_FACTS.remove(PATH_FACTS.keySet().iterator().next());
        }
    }
    public static void recompute(SettlementSavedData data, TownRecord town, ServerLevel level) {
        var residents = data.residents(town.id()).stream().filter(ResidentMood::living).toList();
        int present=0,adults=0,employed=0,unknown=0,stale=0;
        double housing=0,food=0;
        long now=level.getServer().overworld().getGameTime();
        var usedBeds=new java.util.HashSet<GlobalPos>();
        boolean housingUnknown=false,jobsUnknown=false;
        var usedJobs=new java.util.HashSet<GlobalPos>();
        var creditedJobs=new java.util.HashSet<Long>();
        CompoundTag reasons=new CompoundTag(),metrics=town.metrics();
        for(var resident:residents) {
            var prior=resident.needObservation();var observation=prior.copy();
            var entity=level.getEntity(resident.villagerId());
            if(entity==null)for(var other:level.getServer().getAllLevels())if(other!=level&&other.getEntity(resident.villagerId())!=null){entity=other.getEntity(resident.villagerId());break;}
            if(entity instanceof Villager villager&&villager.isAlive()) {
                resident.observeName(villager);
                BedPersonalSpace.refresh(villager, resident);
                resident.seen(villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME).orElse(null),villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).orElse(null));
                boolean inside=villager.level()==level&&town.contains(villager.blockPosition());
                observation.putBoolean("Known",true);observation.putBoolean("Present",inside);observation.putLong("Time",level.getGameTime());
                observation.putBoolean("Adult",!villager.isBaby()&&villager.getVillagerData().getProfession()!=VillagerProfession.NITWIT);
                resident.advanceTime(now);
                observation.putDouble("Food", .75 * resident.satiety() + 5 * resident.mealQuality(now));
                var home=resident.home().orElseGet(()->resident.observedBed().orElseGet(()->resident.crowdedHome().orElse(null)));
                boolean bedUnavailable=inside&&home!=null&&home.dimension().equals(level.dimension())&&town.contains(home.pos())&&!level.hasChunkAt(home.pos());
                if (bedUnavailable) { housingUnknown|=!prior.contains("Housing"); stale++; }
                else {
                    GlobalPos normalized=normalizeBed(level,town,home);
                    String access=normalized==null?"missing":accessibility(level,villager,normalized);
                    if (access.equals("pending")||access.equals("unavailable")) { housingUnknown|=!prior.contains("Housing");stale++; }
                    else {
                        boolean bed=inside&&normalized!=null&&!access.equals("unreachable")&&usedBeds.add(normalized);
                        boolean roof=bed&&roofed(level,normalized.pos());
                        observation.putBoolean("HasBed",bed);observation.putBoolean("Roofed",roof);
                        observation.putDouble("Housing",bed?(roof?100:50):0);
                    }
                }
                String reason=!inside?"outside":villager.isBaby()?"child":!observation.getBoolean("Adult")?"not_workforce":employment(level,town,villager,resident.workplace().orElse(null),usedJobs);
                if(reason.equals("unavailable")||reason.equals("pending")) {
                    boolean sameWork=resident.workplace().isPresent()&&prior.contains("WorkPos")&&prior.getLong("WorkPos")==resident.workplace().get().pos().asLong();
                    if(!sameWork) { observation.remove("Employed");jobsUnknown=true; }
                    else jobsUnknown|=!prior.contains("Employed");
                    stale++;
                } else observation.putBoolean("Employed",reason.equals("employed"));
                resident.workplace().ifPresentOrElse(site->observation.putLong("WorkPos",site.pos().asLong()),()->observation.remove("WorkPos"));
                reasons.putString(resident.villagerId().toString(),reason);
                resident.needObservation(observation);
            } else {
                stale++;
                reasons.putString(resident.villagerId().toString(),"unavailable");
            }
            if(!observation.getBoolean("Known")) { unknown++;continue; }
            if(observation.getBoolean("Employed")&&observation.contains("WorkPos")) {
                BlockPos site=BlockPos.of(observation.getLong("WorkPos"));
                boolean valid=town.contains(site);
                if(valid&&level.hasChunkAt(site)) {
                    var poi=level.getPoiManager().getType(site);
                    var professionId=net.minecraft.resources.ResourceLocation.tryParse(resident.profession());
                    var profession=professionId==null?VillagerProfession.NONE:net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.get(professionId);
                    valid=poi.isPresent()&&profession.heldJobSite().test(poi.get())&&PoiTypes.forState(level.getBlockState(site)).filter(type->type.equals(poi.get())).isPresent();
                }
                if(!valid) {
                    observation.putBoolean("Employed",false);resident.needObservation(observation);
                    reasons.putString(resident.villagerId().toString(),"invalid");
                } else if(!creditedJobs.add(site.asLong())) {
                    observation.putBoolean("Employed",false);resident.needObservation(observation);
                    reasons.putString(resident.villagerId().toString(),"duplicate");
                }
            }
            if(observation.getBoolean("Present"))present++;
            housing+=observation.getDouble("Housing");food+=.75*resident.satiety()+5*resident.mealQuality(now);
            if(observation.getBoolean("Adult")){adults++;if(observation.getBoolean("Employed"))employed++;}
        }
        int guards=0;
        if(net.minecraftforge.fml.ModList.get().isLoaded("recruits"))guards=com.siirio.jemsettlements.compat.recruits.RecruitDefenseEvidence.count(data,town,level,metrics);
        guards+=GuardObservationService.count(town,level,metrics);
        int required=required(town.territoryArea(),SettlementMetricsConfig.GUARD_AREA.get(),1);
        int socialRequired=required(town.territoryArea(),SettlementMetricsConfig.RESIDENT_AREA.get(),2);
        double safety=metrics.getBoolean("GuardsUnknown")?metrics.getDouble("SafetyExact"):Math.min(100,guards*100.0/required);
        double jobs=unknown>0||jobsUnknown?metrics.getDouble("JobsExact"):adults==0?100:employed*100.0/adults;
        double social=Math.min(100,residents.size()*100.0/socialRequired);
        int animals=animalEvidence(level,town,metrics);
        double comfort=Math.min(100,metrics.contains("ComfortExact")?metrics.getDouble("ComfortExact"):metrics.getInt("Comfort"));
        // Stale positive observations remain explicitly stale, never rewritten as factual absence.

        metrics.putInt("PresentResidents",present);metrics.putInt("RegisteredResidents",residents.size());
        metrics.putInt("RequiredResidents",socialRequired);metrics.putInt("EffectiveGuards",guards);metrics.putInt("RequiredGuards",required);
        metrics.putInt("WorkingAdults",adults);metrics.putInt("EmployedAdults",employed);metrics.putInt("Animals",animals);metrics.put("EmploymentReasons",reasons);
        metrics.putBoolean("ResidentsUnavailable",stale>0||unknown>0);metrics.putInt("ResidentsStale",stale);metrics.putInt("ResidentsUnknown",unknown);
        metrics.putBoolean("NeedsStale",stale>0||unknown>0||metrics.getBoolean("GuardsStale")||metrics.getBoolean("AnimalsStale"));
        town.metrics(metrics);
        int divisor=residents.size();
        housing=unknown>0||housingUnknown?metrics.getDouble("HousingExact"):divisor==0?0:housing/divisor;
        food=unknown>0?metrics.getDouble("FoodExact"):divisor==0?0:food/divisor;
        town.happinessEvidence((int)Math.round(housing),(int)Math.round(food),(int)Math.round(safety),(int)Math.round(jobs),(int)Math.round(social),(int)Math.round(comfort));
        double core=HappinessService.core(housing,food,safety,jobs,social,comfort);
        metrics.putInt("HappinessModel",2);
        metrics.putDouble("HousingExact",housing);metrics.putDouble("FoodExact",food);metrics.putDouble("SafetyExact",safety);
        metrics.putDouble("JobsExact",jobs);metrics.putDouble("CommunityExact",social);metrics.putDouble("SharedCore",core);
        for(var resident:residents)resident.sharedNeeds(core,HappinessService.taxPenalty(town.taxPercent()),now);
        int happiness=divisor==0?0:(int)Math.round(residents.stream().mapToDouble(ResidentMood::exactMood).average().orElse(0));
        int crowdPenalty=divisor==0?0:(int)Math.round(residents.stream().mapToInt(ResidentMood::crowdingPenalty).average().orElse(0));
        int lightPenalty=divisor==0?0:(int)Math.round(residents.stream().mapToInt(ResidentMood::lightPenalty).average().orElse(0));
        int fearPenalty=divisor==0?0:(int)Math.round(residents.stream().mapToInt(ResidentMood::fearPenalty).average().orElse(0));
        metrics.putInt("CrowdingPenalty",crowdPenalty);metrics.putInt("LightPenalty",lightPenalty);metrics.putInt("FearPenalty",fearPenalty);
        metrics.putBoolean("ConditionsUnknown",residents.stream().anyMatch(ResidentMood::conditionsUnknown));town.metrics(metrics);
        town.happiness(happiness);town.lastHappinessDay(Math.floorDiv(level.getGameTime(),24000L));
    }
    private static int required(long area, int coverage, int minimum) { return (int)Math.max(minimum, (area + coverage - 1) / coverage); }
    private static GlobalPos normalizeBed(ServerLevel level,TownRecord town,GlobalPos home) {
        if(home==null||!home.dimension().equals(level.dimension())||!town.contains(home.pos())||!level.hasChunkAt(home.pos()))return null;
        var state=level.getBlockState(home.pos());if(!(state.getBlock() instanceof BedBlock))return null;
        var head=state.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD?home.pos():home.pos().relative(state.getValue(BedBlock.FACING));
        var foot=head.relative(state.getValue(BedBlock.FACING).getOpposite());
        if(!level.hasChunkAt(head)||!level.hasChunkAt(foot))return null;
        var h=level.getBlockState(head);var f=level.getBlockState(foot);
        if(h.getBlock()!=state.getBlock()||f.getBlock()!=state.getBlock()||h.getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD
            ||f.getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.FOOT||h.getValue(BedBlock.FACING)!=f.getValue(BedBlock.FACING))return null;
        return GlobalPos.of(level.dimension(),head);
    }
    private static boolean roofed(ServerLevel level,BlockPos bed) {
        var state=level.getBlockState(bed);
        for(var half:new BlockPos[]{bed,bed.relative(state.getValue(BedBlock.FACING).getOpposite())}) {
            boolean covered=false;
            for(int y=1;y<=24&&half.getY()+y<level.getMaxBuildHeight();y++) {
                var at=half.above(y);var block=level.getBlockState(at);
                if(!block.is(net.minecraft.tags.BlockTags.LEAVES)&&!block.getCollisionShape(level,at).isEmpty()){covered=true;break;}
            }
            if(!covered)return false;
        }
        return true;
    }
    private static String accessibility(ServerLevel level,Villager villager,GlobalPos site) {
        if(!level.hasChunkAt(site.pos()))return "unavailable";
        if(villager.blockPosition().distSqr(site.pos())>48L*48L)return "unreachable";
        var request=new PathRequest(villager.getUUID(),site);var fact=PATH_FACTS.get(request);
        if(fact!=null&&level.getServer().getTickCount()-fact.tick()<=400L)return fact.reachable()?"reachable":"unreachable";
        if(PATHS.size()<256&&QUEUED.add(request))PATHS.addLast(request);
        return "pending";
    }
    private static String employment(ServerLevel level, TownRecord town, Villager villager, GlobalPos site, java.util.Set<GlobalPos> used) {
        if (site == null) return "missing";
        if (!site.dimension().equals(level.dimension()) || !town.contains(site.pos())) return "outside";
        if (!level.hasChunkAt(site.pos())) return "unavailable";
        var poi = level.getPoiManager().getType(site.pos());
        if (poi.isEmpty() || !villager.getVillagerData().getProfession().heldJobSite().test(poi.get())
                || PoiTypes.forState(level.getBlockState(site.pos())).filter(type -> type.equals(poi.get())).isEmpty()) return "invalid";
        if (!used.add(site)) return "duplicate";
        String access=accessibility(level,villager,site);
        return access.equals("reachable")?"employed":access;
    }
    private static int animalEvidence(ServerLevel level,TownRecord town,CompoundTag metrics) {
        CompoundTag observations=metrics.getCompound("AnimalObservations").copy();
        boolean stale=false;
        for(String key:java.util.Set.copyOf(observations.getAllKeys())) {
            var row=observations.getCompound(key);var pos=BlockPos.of(row.getLong("Position"));
            var entity=level.getEntity(java.util.UUID.fromString(key));
            if(entity instanceof net.minecraft.world.entity.Mob animal && AnimalObservationService.supported(animal) && animal.isAlive() && town.contains(animal.blockPosition())) {
                if(animal.getTarget()!=null||animal instanceof net.minecraft.world.entity.monster.Enemy||animal instanceof net.minecraft.world.entity.NeutralMob neutral&&neutral.isAngry())observations.remove(key);
            } else if(entity!=null||!town.contains(pos)||level.hasChunkAt(pos))observations.remove(key);
            else stale=true;
        }
        var types=new java.util.HashSet<String>();for(String key:observations.getAllKeys())types.add(observations.getCompound(key).getString("Type"));
        int loaded=0,total=0;
        for(int x=town.territoryMinX()>>4;x<=town.territoryMaxX()>>4;x++)for(int z=town.territoryMinZ()>>4;z<=town.territoryMaxZ()>>4;z++){total++;if(level.getChunkSource().getChunkNow(x,z)!=null)loaded++;}
        stale|=loaded<total;
        metrics.put("AnimalObservations",observations);metrics.putBoolean("AnimalsStale",stale);metrics.putInt("AnimalTypes",types.size());metrics.putInt("RequiredAnimalTypes",SettlementMetricsConfig.ANIMAL_TYPES.get());
        int score=Math.min(100,types.size()*100/SettlementMetricsConfig.ANIMAL_TYPES.get());
        // Before any animal evidence exists, unavailable territory must not erase a previous score.
        return stale&&observations.isEmpty()?metrics.getInt("Animals"):score;
    }
}
