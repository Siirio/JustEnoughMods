package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.compat.recruits.RecruitsAdapter;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer.Continuation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/** Derived observations only; canonical registration and native base needs retain their owners. */
public final class ResidentConditions {
    private static final int ACTORS_PER_TICK = 8, OCCUPANCY_PER_TICK = 256, THREATS_PER_SCAN = 64;
    private record Actor(ResidentMood mood, WorkforceRecord recruit) { }
    private static final class Observation {
        GlobalPos home;
        long nextHome, nextLight, nextFear;
        boolean roomUnknown = true, lightUnknown = true, fearUnknown;
        Occupancy pending;
    }
    private static final class Occupancy {
        final Actor actor;
        final Observation observation;
        final GlobalPos home;
        final ResidentialRooms.Snapshot room;
        final List<Actor> roster;
        int cursor, residents;
        boolean unknown;
        Occupancy(Actor actor, Observation observation, GlobalPos home, ResidentialRooms.Snapshot room, List<Actor> roster) {
            this.actor = actor; this.observation = observation; this.home = home; this.room = room; this.roster = roster;
        }
    }
    private static final Map<UUID, Observation> OBSERVATIONS = new HashMap<>();
    private static final ArrayDeque<Occupancy> OCCUPANCY = new ArrayDeque<>();
    private static List<Actor> roster = List.of();
    private static long nextRoster;
    private static int cursor;
    private ResidentConditions() { }

    public static void clear() {
        OBSERVATIONS.clear(); OCCUPANCY.clear(); roster = List.of(); nextRoster = 0; cursor = 0;
        ResidentialRooms.clear();
        ThreatAssessment.clear();
    }
    public static void invalidate(ServerLevel level, BlockPos changed) {
        ResidentialRooms.invalidate(level, changed);
        ThreatAssessment.invalidate(level, changed);
        // Room topology can change beyond the edited block's immediate neighbours.
        for (var state : OBSERVATIONS.values()) if (state.home != null && state.home.dimension().equals(level.dimension())) {
            state.nextHome = 0; state.nextLight = 0; state.roomUnknown = true; state.lightUnknown = true; state.pending = null;
        }
    }
    public static void tick(MinecraftServer server) {
        if (!SettlementAuthority.active(server)) return;
        ResidentialRooms.tick(server);
        ThreatAssessment.tick(server);
        var data = SettlementSavedData.get(server); long now = server.overworld().getGameTime();
        if (now >= nextRoster || now + 100 < nextRoster) {
            refreshRoster(data); nextRoster = now + 100;
        }
        for (int i = 0, count = Math.min(ACTORS_PER_TICK, roster.size()); i < count; i++) {
            if (cursor >= roster.size()) cursor = 0;
            Actor actor = roster.get(cursor++); LivingEntity entity = loaded(server, actor);
            if (entity == null) continue;
            Observation state = OBSERVATIONS.computeIfAbsent(actor.mood().villagerId(), ignored -> new Observation());
            if (actor.recruit() != null) RecruitsAdapter.syncNeeds(entity, actor.mood());
            actor.mood().advanceTime(now);
            BedPersonalSpace.refresh(entity, actor.mood());
            var previousBed = actor.mood().observedBed();
            GlobalPos home = home(entity, actor.mood(), state);
            if (!previousBed.equals(actor.mood().observedBed())) data.changed();
            if (!Objects.equals(home, state.home)) {
                state.home = home; state.nextHome = 0; state.nextLight = 0; state.pending = null;
                state.roomUnknown = true; state.lightUnknown = true;
            }
            ServerLevel level = (ServerLevel) entity.level();
            if (now >= state.nextHome && state.pending == null) observeHousing(actor, state, level, now);
            if (now >= state.nextFear) { observeFear(entity, actor.mood(), state); state.nextFear = now + HousingConditionConfig.FEAR_INTERVAL.get(); }
            updateUnknown(actor.mood(), state, data);
        }
        countOccupants(server, data);
    }
    private static void refreshRoster(SettlementSavedData data) {
        var next = new ArrayList<Actor>(); var ids = new HashSet<UUID>();
        for (var town : data.towns()) for (var resident : data.residents(town.id()))
            if (resident.living() && ids.add(resident.villagerId())) next.add(new Actor(resident, null));
        if (ModList.get().isLoaded("recruits")) for (var recruit : data.workforce())
            if (eligible(recruit) && ids.add(recruit.entityId())) next.add(new Actor(recruit.foodHistory(), recruit));
        OBSERVATIONS.keySet().retainAll(ids);
        if (!roster.equals(next)) {
            roster = List.copyOf(next);
            // Cancel only when canonical membership changed; large stable rosters must finish bounded jobs.
            OCCUPANCY.clear(); OBSERVATIONS.values().forEach(state -> { state.pending = null; state.nextHome = 0; });
        }
    }
    private static boolean eligible(WorkforceRecord row) {
        return row.type() == WorkforceType.RECRUIT && row.needsEnrolled() && row.settlementActive();
    }
    private static LivingEntity loaded(MinecraftServer server, Actor actor) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(actor.mood().villagerId());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            if (actor.recruit() == null) return living instanceof Villager ? living : null;
            return ModList.get().isLoaded("recruits") && eligible(actor.recruit())
                    && RecruitsAdapter.ownedBy(living, actor.recruit().ownerId()) ? living : null;
        }
        return null;
    }
    private static GlobalPos home(LivingEntity entity, ResidentMood mood, Observation prior) {
        if (entity instanceof Villager villager)
            return villager.getBrain().getMemory(MemoryModuleType.HOME)
                    .orElseGet(() -> mood.observedBed().orElseGet(() -> mood.crowdedHome().orElse(null)));
        var sleeping = entity.getSleepingPos().map(pos -> GlobalPos.of(entity.level().dimension(), pos));
        sleeping.ifPresent(mood::observedBed);
        return sleeping.orElseGet(() -> mood.observedBed().orElseGet(() -> mood.crowdedHome().orElse(prior.home)));
    }
    private static void observeHousing(Actor actor, Observation state, ServerLevel level, long now) {
        GlobalPos home = state.home;
        if (home == null || home.dimension().equals(level.dimension()) && level.hasChunkAt(home.pos())
                && !(level.getBlockState(home.pos()).getBlock() instanceof BedBlock)) {
            // Missing housing has its own common shortfall, not a fictitious zero-area room.
            actor.mood().crowdingPenalty(0); actor.mood().lightPenalty(0);
            state.roomUnknown = false; state.lightUnknown = false; state.nextHome = now + 100; return;
        }
        if (!home.dimension().equals(level.dimension()) || !level.hasChunkAt(home.pos())) {
            state.roomUnknown = true; state.lightUnknown = true; state.nextHome = now + 100; return;
        }
        var room = ResidentialRooms.request(level, home.pos());
        if (!room.complete() || room.unavailable()) {
            state.roomUnknown = true; state.nextHome = now + 20; return;
        }
        state.roomUnknown = false;
        state.pending = new Occupancy(actor, state, home, room, roster); OCCUPANCY.addLast(state.pending);
        state.nextHome = now + 100;
        if (now < state.nextLight) return;
        int light = level.getBrightness(LightLayer.BLOCK, home.pos()); boolean known = true;
        // The room owner already bounds and validates accessible floors. Sample across its whole footprint.
        Iterable<LightingPolicy.Point> floor = () -> room.floors().stream()
                .map(pos -> new LightingPolicy.Point(pos.getX(), pos.getY(), pos.getZ())).iterator();
        for (var point : LightingPolicy.sample(floor, 12)) {
            BlockPos sample = new BlockPos(point.x(), point.y(), point.z());
            if (!level.hasChunkAt(sample)) { known = false; continue; }
            light = Math.min(light, level.getBrightness(LightLayer.BLOCK, sample));
        }
        state.lightUnknown = !known;
        if (known) {
            int penalty = LightingPolicy.penalty(light, HousingConditionConfig.LIGHT_GOOD.get(), HousingConditionConfig.LIGHT_MILD.get(),
                    HousingConditionConfig.LIGHT_MILD_PENALTY.get(), HousingConditionConfig.LIGHT_SEVERE_PENALTY.get());
            if (actor.mood().lightPenalty() != penalty) { actor.mood().lightPenalty(penalty); SettlementSavedData.get(level.getServer()).changed(); }
        }
        state.nextLight = now + HousingConditionConfig.LIGHT_INTERVAL.get();
    }
    private static void countOccupants(MinecraftServer server, SettlementSavedData data) {
        int budget = OCCUPANCY_PER_TICK;
        while (budget > 0 && !OCCUPANCY.isEmpty()) {
            Occupancy job = OCCUPANCY.peekFirst();
            if (job.observation.pending != job || !Objects.equals(job.home, job.observation.home)) { OCCUPANCY.removeFirst(); continue; }
            ServerLevel level = server.getLevel(job.home.dimension());
            if (level == null || !level.hasChunkAt(job.home.pos())
                    || !(level.getBlockState(job.home.pos()).getBlock() instanceof BedBlock)) {
                job.observation.roomUnknown = true; job.observation.pending = null; OCCUPANCY.removeFirst();
                updateUnknown(job.actor.mood(), job.observation, data); continue;
            }
            while (budget > 0 && job.cursor < job.roster.size()) {
                Actor neighbour = job.roster.get(job.cursor++); budget--;
                if (!neighbour.mood().living() || neighbour.recruit() != null && !eligible(neighbour.recruit())) continue;
                Observation observation = OBSERVATIONS.get(neighbour.mood().villagerId());
                GlobalPos bed = observation == null ? neighbour.mood().home().orElseGet(() -> neighbour.mood().observedBed()
                        .orElseGet(() -> neighbour.mood().crowdedHome().orElse(null))) : observation.home;
                if (bed == null || !bed.dimension().equals(job.home.dimension())) continue;
                // An anchor can be either half: its other half plus one neighbour is at most two steps away.
                // This is only a coarse exclusion; physical normalized bed membership remains authoritative below.
                if (!possiblyAdjacent(job.room, bed.pos())) continue;
                // An unloaded native bed cannot be treated as an absent occupant.
                if (!level.hasChunkAt(bed.pos())) { job.unknown = true; continue; }
                Boolean member = belongs(level, job.room, bed.pos());
                if (member == null) job.unknown = true;
                else if (member) job.residents++;
            }
            if (job.cursor == job.roster.size()) {
                if (job.unknown) {
                    job.observation.roomUnknown = true; job.observation.pending = null; OCCUPANCY.removeFirst();
                    updateUnknown(job.actor.mood(), job.observation, data); continue;
                }
                int residents = Math.max(1, job.residents);
                int penalty = CrowdingPolicy.penalty(job.room.area(), residents, HousingConditionConfig.FLOOR_PER_RESIDENT.get(),
                        HousingConditionConfig.CROWD_MAX.get(), true, job.actor.mood().crowdingPenalty());
                if (job.actor.mood().crowdingPenalty() != penalty) { job.actor.mood().crowdingPenalty(penalty); data.changed(); }
                job.observation.pending = null; OCCUPANCY.removeFirst(); updateUnknown(job.actor.mood(), job.observation, data);
            }
        }
    }
    private static boolean possiblyAdjacent(ResidentialRooms.Snapshot room, BlockPos bed) {
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            if (Math.abs(x) + Math.abs(z) <= 2 && room.floors().contains(bed.offset(x, 0, z))) return true;
        return false;
    }
    private static Boolean belongs(ServerLevel level, ResidentialRooms.Snapshot room, BlockPos bed) {
        if (!level.hasChunkAt(bed)) return null;
        var state = level.getBlockState(bed);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        var facing = state.getValue(BedBlock.FACING);
        BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? bed : bed.relative(facing);
        BlockPos foot = head.relative(facing.getOpposite());
        if (!level.hasChunkAt(head) || !level.hasChunkAt(foot)) return null;
        var headState = level.getBlockState(head); var footState = level.getBlockState(foot);
        if (!(headState.getBlock() instanceof BedBlock) || !(footState.getBlock() instanceof BedBlock)
                || headState.getValue(BedBlock.PART) != BedPart.HEAD || footState.getValue(BedBlock.PART) != BedPart.FOOT
                || headState.getValue(BedBlock.FACING) != facing || footState.getValue(BedBlock.FACING) != facing) return false;
        for (BlockPos half : new BlockPos[] {head, foot}) {
            if (room.floors().contains(half)) return true;
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL)
                if (room.floors().contains(half.relative(direction))) return true;
        }
        return false;
    }
    private static boolean hostile(Mob mob, LivingEntity resident) { return mob instanceof Enemy || mob.getTarget() == resident; }
    private static void observeFear(LivingEntity entity, ResidentMood mood, Observation state) {
        ServerLevel level = (ServerLevel) entity.level(); int[] visited = {0}, observed = {0}; boolean[] unknown = {false};
        double radius = HousingConditionConfig.FEAR_RADIUS.get();
        var threats = new EntityTypeTest<Entity, Mob>() {
            @Override public Mob tryCast(Entity candidate) { return candidate instanceof Mob mob && mob.isAlive() && hostile(mob, entity) ? mob : null; }
            @Override public Class<? extends Entity> getBaseClass() { return Mob.class; }
        };
        level.getEntities().get(threats, entity.getBoundingBox().inflate(radius), mob -> {
            if (++visited[0] > THREATS_PER_SCAN) { unknown[0] = true; return Continuation.ABORT; }
            if (mob.distanceToSqr(entity) > radius * radius) return Continuation.CONTINUE;
            var threat = ThreatAssessment.request(level, entity, mob);
            unknown[0] |= !threat.complete();
            int penalty = threat.kind() == ThreatAssessment.Kind.OPEN ? HousingConditionConfig.FEAR_VISIBLE.get()
                    : threat.kind() == ThreatAssessment.Kind.GLASS ? Math.min(HousingConditionConfig.FEAR_GLASS.get(), HousingConditionConfig.FEAR_VISIBLE.get()) : 0;
            observed[0] = Math.max(observed[0], penalty);
            if (threat.kind() == ThreatAssessment.Kind.OPEN) return Continuation.ABORT;
            return Continuation.CONTINUE;
        });
        state.fearUnknown = unknown[0];
        int fear = mood.fearPenalty();
        int next = ThreatRouteModel.fear(fear, observed[0], !unknown[0], HousingConditionConfig.FEAR_MAX.get(), HousingConditionConfig.FEAR_RECOVERY.get());
        if (next != fear) { mood.fearPenalty(next); SettlementSavedData.get(level.getServer()).changed(); }
    }
    private static void updateUnknown(ResidentMood mood, Observation state, SettlementSavedData data) {
        boolean unknown = state.roomUnknown || state.lightUnknown || state.fearUnknown || state.pending != null;
        if (mood.conditionsUnknown() != unknown) { mood.conditionsUnknown(unknown); data.changed(); }
    }
    @SubscribeEvent
    public static void damaged(LivingDamageEvent event) {
        if (!isHostileDamage(event) || !(event.getEntity().level() instanceof ServerLevel level)
                || !SettlementAuthority.active(level.getServer())) return;
        var data = SettlementSavedData.get(level.getServer()); LivingEntity victim = event.getEntity();
        ResidentMood mood = victim instanceof Villager ? data.resident(victim.getUUID()).filter(ResidentMood::living).orElse(null) : null;
        if (mood == null && ModList.get().isLoaded("recruits")) mood = data.workforceEntity(victim.getUUID())
                .filter(ResidentConditions::eligible).filter(row -> RecruitsAdapter.ownedBy(victim, row.ownerId())).map(WorkforceRecord::foodHistory).orElse(null);
        if (mood == null) return;
        mood.fearPenalty(HousingConditionConfig.FEAR_MAX.get());
        var state = OBSERVATIONS.computeIfAbsent(victim.getUUID(), ignored -> new Observation());
        state.nextFear = level.getServer().overworld().getGameTime() + HousingConditionConfig.FEAR_INTERVAL.get();
        data.changed();
    }
    public static boolean isHostileDamage(LivingDamageEvent event) {
        return event.getAmount() > 0 && event.getSource().getEntity() instanceof Mob attacker
                && hostile(attacker, event.getEntity());
    }
}
