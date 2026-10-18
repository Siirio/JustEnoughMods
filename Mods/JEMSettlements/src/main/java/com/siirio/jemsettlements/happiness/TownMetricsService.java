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
    private static final java.util.Set<java.util.UUID> QUEUED = new java.util.HashSet<>();
    private static final java.util.Map<java.util.UUID, PathFact> PATH_FACTS = new java.util.LinkedHashMap<>();
    private record PathRequest(java.util.UUID resident, GlobalPos site) { }
    private record PathFact(GlobalPos site, long tick, boolean reachable) { }
    private TownMetricsService() { }
    public static void clear() { PATHS.clear(); QUEUED.clear(); PATH_FACTS.clear(); }
    /** Globally at most four native path probes per tick; repeated combat recomputes cannot multiply work. */
    public static void tickPaths(net.minecraft.server.MinecraftServer server) {
        for (int budget=0;budget<4&&!PATHS.isEmpty();budget++) {
            var request=PATHS.removeFirst(); QUEUED.remove(request.resident());
            var level=server.getLevel(request.site().dimension());
            if(level==null||!level.hasChunkAt(request.site().pos())||!(level.getEntity(request.resident()) instanceof Villager villager)||!villager.isAlive())continue;
            boolean reachable=false;
            if(villager.blockPosition().distSqr(request.site().pos())<=48L*48L) {
                var path=villager.getNavigation().createPath(request.site().pos(),1);
                reachable=path!=null&&path.canReach();
            }
            PATH_FACTS.put(request.resident(),new PathFact(request.site(),server.getTickCount(),reachable));
            while(PATH_FACTS.size()>4096)PATH_FACTS.remove(PATH_FACTS.keySet().iterator().next());
        }
    }
    public static void recompute(SettlementSavedData data, TownRecord town, ServerLevel level) {
        var residents = data.residents(town.id()).stream().filter(ResidentMood::living).toList();
        int present=0,adults=0,employed=0,housing=0,food=0,unknown=0,stale=0;
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
                resident.seen(villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME).orElse(null),villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).orElse(null));
                boolean inside=villager.level()==level&&town.contains(villager.blockPosition());
                observation.putBoolean("Known",true);observation.putBoolean("Present",inside);observation.putLong("Time",level.getGameTime());
                observation.putBoolean("Adult",!villager.isBaby()&&villager.getVillagerData().getProfession()!=VillagerProfession.NITWIT);
                observation.putInt("Food",inside?(resident.craving()?resident.satiety()<10?0:35:100):0);
                var home=resident.home().orElse(null);
                boolean homeUnavailable=inside&&home!=null&&home.dimension().equals(level.dimension())&&town.contains(home.pos())&&!level.hasChunkAt(home.pos());
                if(homeUnavailable) { housingUnknown|=!prior.contains("Housing");stale++; }
                else observation.putInt("Housing",inside&&validHome(level,town,home)?Math.min(100,45+resident.homeAmenities().points()*4):0);
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
            housing+=observation.getInt("Housing");food+=observation.getInt("Food");
            if(observation.getBoolean("Adult")){adults++;if(observation.getBoolean("Employed"))employed++;}
        }
        int guards=0;
        if(net.minecraftforge.fml.ModList.get().isLoaded("recruits"))guards=com.siirio.jemsettlements.compat.recruits.RecruitDefenseEvidence.count(data,town,level,metrics);
        int required=required(town.territoryArea(),SettlementMetricsConfig.GUARD_AREA.get(),1);
        int socialRequired=required(town.territoryArea(),SettlementMetricsConfig.RESIDENT_AREA.get(),2);
        int safety=metrics.getBoolean("GuardsUnknown")?town.safetyScore():Math.min(100,guards*100/required);
        int jobs=unknown>0||jobsUnknown?town.jobsScore():adults==0?0:employed*100/adults;
        int social=unknown>0?town.communityScore():Math.min(100,present*100/socialRequired);
        int animals=animalEvidence(level,town,metrics);
        int comfort=Math.min(100,metrics.getInt("Comfort"));
        // Stale positive observations remain explicitly stale, never rewritten as factual absence.
        if(unknown==0)residents.forEach(resident->resident.environment(comfort,social,animals));
        metrics.putInt("PresentResidents",present);metrics.putInt("RegisteredResidents",residents.size());
        metrics.putInt("RequiredResidents",socialRequired);metrics.putInt("EffectiveGuards",guards);metrics.putInt("RequiredGuards",required);
        metrics.putInt("WorkingAdults",adults);metrics.putInt("EmployedAdults",employed);metrics.putInt("Animals",animals);metrics.put("EmploymentReasons",reasons);
        metrics.putBoolean("ResidentsUnavailable",stale>0||unknown>0);metrics.putInt("ResidentsStale",stale);metrics.putInt("ResidentsUnknown",unknown);
        metrics.putBoolean("NeedsStale",stale>0||unknown>0||metrics.getBoolean("GuardsStale")||metrics.getBoolean("AnimalsStale"));
        town.metrics(metrics);
        int divisor=residents.size();
        housing=unknown>0||housingUnknown?town.housingScore():divisor==0?0:housing/divisor;
        food=unknown>0?town.foodScore():divisor==0?0:food/divisor;
        town.happinessEvidence(housing,food,safety,jobs,social,comfort);
        int happiness=divisor==0?0:HappinessService.calculate(housing,food,safety,jobs,social,comfort,town.taxPercent());
        if(divisor>0)happiness=Math.min(happiness+animals*SettlementMetricsConfig.ANIMAL_BONUS.get()/100,HappinessService.socialCap(social));
        town.happiness(happiness);town.lastHappinessDay(Math.floorDiv(level.getGameTime(),24000L));
    }
    private static int required(long area, int coverage, int minimum) { return (int)Math.max(minimum, (area + coverage - 1) / coverage); }
    private static boolean validHome(ServerLevel level, TownRecord town, GlobalPos home) {
        return home != null && home.dimension().equals(level.dimension()) && town.contains(home.pos()) && level.hasChunkAt(home.pos())
                && level.getBlockState(home.pos()).getBlock() instanceof BedBlock;
    }
    private static String employment(ServerLevel level, TownRecord town, Villager villager, GlobalPos site, java.util.Set<GlobalPos> used) {
        if (site == null) return "missing";
        if (!site.dimension().equals(level.dimension()) || !town.contains(site.pos())) return "outside";
        if (!level.hasChunkAt(site.pos())) return "unavailable";
        var poi = level.getPoiManager().getType(site.pos());
        if (poi.isEmpty() || !villager.getVillagerData().getProfession().heldJobSite().test(poi.get())
                || PoiTypes.forState(level.getBlockState(site.pos())).filter(type -> type.equals(poi.get())).isEmpty()) return "invalid";
        if (!used.add(site)) return "duplicate";
        // A remote remembered POI is not proof of accessibility. Bound path work to native follow range.
        if (villager.blockPosition().distSqr(site.pos()) > 48L * 48L) return "unreachable";
        var fact=PATH_FACTS.get(villager.getUUID());
        if(fact!=null&&fact.site().equals(site)&&level.getServer().getTickCount()-fact.tick()<=400L)
            return fact.reachable()?"employed":"unreachable";
        if(PATHS.size()<256&&QUEUED.add(villager.getUUID()))PATHS.addLast(new PathRequest(villager.getUUID(),site));
        return "pending";
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
