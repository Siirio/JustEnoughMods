package com.siirio.jemserver.smp.events;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.RaidArenaApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.encounter.HostedRaidScaling;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;

/** Loaded-only observation; explicit repair preserves the exact entity before removal. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BossArenaDiagnostics {
    private static final String ARCHIVE = "boss_quarantine";
    private static final int ENTITY_LIMIT = 16_384, ARENA_LIMIT = 4_096, PAGE_SIZE = 16;
    private static final int PLAN_LIMIT = 16, ARCHIVE_LIMIT = 64, SNAPSHOT_BYTES = 2_097_152;
    private static final long PLAN_TICKS = 1_200L;
    private static final Map<MinecraftServer, LinkedHashMap<UUID, Plan>> PLANS = new WeakHashMap<>();
    private record Plan(UUID keeper, UUID duplicate, String arena, String actor, long expires) {}
    private record Pair(LivingEntity keeper, LivingEntity duplicate, ArenaRecord arena) {}
    private record Scan(List<String> rows, boolean complete, int inspected) {}
    private BossArenaDiagnostics() {}

    /** Replay only an already-confirmed exact removal if a crash retained the old entity chunk. */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void confirmedRemoval(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        var serverForMutation = event.getEntity().getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { confirmedRemovalInScope(event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> confirmedRemovalInScope(event));
    }

    private static void confirmedRemovalInScope(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity boss)) return;
        CompoundTag row = SmpData.get(level.getServer()).bossQuarantine().current( boss.getUUID());
        if (row == null || !row.getString("state").equals("QUARANTINED") || !row.hasUUID("keeper")
                || !row.getString("dimension").equals(level.dimension().location().toString())) return;
        CompoundTag snapshot = row.getCompound("entity");
        if (!snapshot.hasUUID("UUID") || !snapshot.getUUID("UUID").equals(boss.getUUID())
                || !snapshot.getString("id").equals(BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString())) return;
        var data = WorldTierData.get(level.getServer());
        var encounter = WorldTierApi.encounter(boss).orElse(null);
        if (encounter == null || encounter.arenaId() == null
                || !data.canonicalArenaId(encounter.arenaId()).equals(data.canonicalArenaId(row.getString("arena")))
                || !data.liveBoss(row.getString("arena")).filter(row.getUUID("keeper")::equals).isPresent()) return;
        event.setCanceled(true);
        data.clearLiveBoss(row.getString("arena"), boss.getUUID());
        com.mojang.logging.LogUtils.getLogger().warn("Replayed confirmed quarantined boss removal: UUID={}, keeper={}, arena={}",
                boss.getUUID(), row.getUUID("keeper"), row.getString("arena"));
    }

    /** Register as a child of the existing /jem root on the dedicated dispatcher. */
    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        var keeper = Commands.argument("keeper", UuidArgument.uuid());
        keeper.then(Commands.argument("duplicate", UuidArgument.uuid()).executes(context ->
                plan(context.getSource(), UuidArgument.getUuid(context, "keeper"), UuidArgument.getUuid(context, "duplicate"))));
        var confirmKeeper = Commands.argument("keeper", UuidArgument.uuid());
        confirmKeeper.then(Commands.argument("duplicate", UuidArgument.uuid()).executes(context ->
                confirm(context.getSource(), UuidArgument.getUuid(context, "token"), UuidArgument.getUuid(context, "keeper"),
                        UuidArgument.getUuid(context, "duplicate"))));
        return Commands.literal("boss-audit").requires(source -> source.hasPermission(2) && source.getServer().isDedicatedServer())
                .executes(context -> audit(context.getSource(), 1))
                .then(Commands.literal("list").executes(context -> audit(context.getSource(), 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1, 4096))
                                .executes(context -> audit(context.getSource(), IntegerArgumentType.getInteger(context, "page")))))
                .then(Commands.literal("plan").then(keeper))
                .then(Commands.literal("confirm").then(Commands.argument("token", UuidArgument.uuid()).then(confirmKeeper)))
                .then(Commands.literal("archive").then(Commands.argument("duplicate", UuidArgument.uuid())
                        .executes(context -> archive(context.getSource(), UuidArgument.getUuid(context, "duplicate")))));
    }

    private static int audit(CommandSourceStack source, int page) {
        Scan scan = scan(source.getServer());
        int pages = Math.max(1, (scan.rows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        tell(source, "Boss audit: loaded entities inspected=" + scan.inspected + "; scanComplete=" + scan.complete
                + "; page=" + page + "/" + pages + ". Unloaded entities/structures are NOT inspected; absence is unknown.");
        if (page > pages) return reject(source, "Page is beyond this bounded snapshot.");
        scan.rows.stream().skip((long) (page - 1) * PAGE_SIZE).limit(PAGE_SIZE).forEach(row -> tell(source, row));
        tell(source, "Read-only. A repeated type or nearby position is not duplicate proof. Use plan <keeperUUID> <duplicateUUID> only after inspecting the exact arena.");
        return 1;
    }

    private static Scan scan(MinecraftServer server) {
        List<String> rows = new ArrayList<>();
        WorldTierData data = WorldTierData.get(server);
        int inspected = 0; boolean complete = true;
        Map<String, Integer> groups = new TreeMap<>();
        outer: for (ServerLevel level : server.getAllLevels()) for (Entity entity : level.getAllEntities()) {
            if (++inspected > ENTITY_LIMIT) { complete = false; break outer; }
            if (!(entity instanceof LivingEntity boss) || WorldTierApi.profile(boss).isEmpty()) continue;
            var encounter = WorldTierApi.encounter(boss).orElse(null);
            String rawArena = encounter == null ? null : encounter.arenaId();
            String arena = rawArena == null || rawArena.isBlank() ? "unclassified" : data.canonicalArenaId(rawArena);
            var projected = RaidArenaApi.arena(server, BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()),
                    level.dimension().location(), boss.blockPosition()).orElse(null);
            UUID claimed = data.liveBoss(arena).orElse(null);
            groups.merge(level.dimension().location() + "|" + arena, 1, Integer::sum);
            rows.add("entity=" + boss.getUUID() + " type=" + BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType())
                    + " dimension=" + level.dimension().location() + " pos=" + boss.blockPosition().toShortString()
                    + " ArenaId=" + arena + " savedArenaId=" + rawArena + " positionalArena=" + (projected == null ? "unknown" : projected.id())
                    + " provenance=" + (encounter == null ? "unverified" : encounter.provenance())
                    + " progressionEligible=" + (encounter != null && encounter.progressionEligible())
                    + " canonicalLive=" + claimed + " held=" + HostedEncounterApi.held(boss)
                    + " hosted=" + !HostedEncounterApi.status(boss).isEmpty() + " raid=" + HostedEncounterApi.isRaid(boss)
                    + " spawnType=" + boss.getPersistentData().getString("forge:spawn_type")
                    + " eventSpawned=" + boss.getPersistentData().getBoolean(EventSession.SPAWNED)
                    + " eventOwner=" + (boss.getPersistentData().hasUUID(EventSession.SESSION)
                        ? boss.getPersistentData().getUUID(EventSession.SESSION) : "none")
                    + " hostedBossOwner=" + (boss.getPersistentData().hasUUID("JEMHostedBoss")
                        ? boss.getPersistentData().getUUID("JEMHostedBoss") : "none"));
        }
        groups.forEach((key, count) -> { if (count > 1) rows.add("REVIEW ONLY group=" + key + " loadedCount=" + count); });
        int count = 0;
        for (ArenaRecord arena : data.arenas()) {
            if (++count > ARENA_LIMIT) { complete = false; break; }
            if (!arena.structureArena()) continue;
            ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
            ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            UUID live = data.liveBoss(arena.id()).orElse(null);
            Entity loaded = level == null || live == null ? null : level.getEntity(live);
            rows.add("registryArena=" + arena.id() + " dimension=" + arena.dimension() + " bounds=" + arena.bounds()
                    + " canonicalLive=" + live + " liveState=" + (live == null ? "unclaimed" : loaded == null ? "unloaded_or_missing_unknown" : "loaded")
                    + " encounterReservation=" + arena.activeEncounterId()
                    + " anchorChunkLoaded=" + (level != null && level.hasChunkAt(arena.respawnPosition())));
        }
        rows.sort(String::compareTo);
        return new Scan(List.copyOf(rows), complete, Math.min(inspected, ENTITY_LIMIT));
    }

    private static int plan(CommandSourceStack source, UUID keeper, UUID duplicate) {
        var serverForMutation = source.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return planInScope(source, keeper, duplicate); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> planInScope(source, keeper, duplicate));
    }

    private static int planInScope(CommandSourceStack source, UUID keeper, UUID duplicate) {
        try {
            Pair pair = validate(source.getServer(), keeper, duplicate);
            var archive = SmpData.get(source.getServer());
            if (archive.bossQuarantine().current( duplicate) != null) return reject(source, "A preserved record already exists for this UUID; inspect archive before any further action.");
            var plans = PLANS.computeIfAbsent(source.getServer(), ignored -> new LinkedHashMap<>());
            long now = source.getServer().overworld().getGameTime();
            plans.values().removeIf(plan -> plan.expires < now);
            if (plans.size() >= PLAN_LIMIT) return reject(source, "Too many pending plans; wait for expiry.");
            UUID token = UUID.randomUUID();
            plans.put(token, new Plan(keeper, duplicate, pair.arena.id(), actor(source), now + PLAN_TICKS));
            tell(source, "PLAN ONLY: preserve keeper=" + keeper + "; quarantine duplicate=" + duplicate + "; ArenaId=" + pair.arena.id()
                    + "; current canonical claim=" + WorldTierData.get(source.getServer()).liveBoss(pair.arena.id()).orElse(null)
                    + ". Preserve/read back full duplicate NBT, then transfer its canonical claim to the explicit keeper if needed, before discard; no death/loot/progression event.");
            tell(source, "Confirm within0.05Minecraft-day: /jem boss-audit confirm " + token + " " + keeper + " " + duplicate);
            return 1;
        } catch (IllegalStateException failure) { return reject(source, failure.getMessage()); }
    }

    private static int confirm(CommandSourceStack source, UUID token, UUID keeper, UUID duplicate) {
        var serverForMutation = source.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return confirmInScope(source, token, keeper, duplicate); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> confirmInScope(source, token, keeper, duplicate));
    }

    private static int confirmInScope(CommandSourceStack source, UUID token, UUID keeper, UUID duplicate) {
        var plans = PLANS.get(source.getServer());
        Plan plan = plans == null ? null : plans.remove(token);
        long now = source.getServer().overworld().getGameTime();
        if (plan == null || plan.expires < now || !plan.keeper.equals(keeper) || !plan.duplicate.equals(duplicate)
                || !plan.actor.equals(actor(source))) return reject(source, "Expired/mismatched one-use plan; create a fresh exact plan.");
        try {
            Pair pair = validate(source.getServer(), keeper, duplicate);
            if (!pair.arena.id().equals(plan.arena)) return reject(source, "Arena changed; nothing removed.");
            SmpData data = SmpData.get(source.getServer());
            if (data.bossQuarantine().current( duplicate) != null || data.bossQuarantine().currentRows().size() >= ARCHIVE_LIMIT)
                return reject(source, "Quarantine already contains this UUID or is full; nothing removed.");
            CompoundTag snapshot = new CompoundTag();
            if (!pair.duplicate.save(snapshot)) return reject(source, "Native entity refused complete serialization; nothing removed.");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(snapshot, bytes);
            if (bytes.size() > SNAPSHOT_BYTES) return reject(source, "Snapshot exceeds the bounded quarantine size; nothing removed.");
            CompoundTag row = new CompoundTag();
            row.putUUID("id", duplicate); row.putUUID("keeper", keeper); row.putUUID("token", token);
            row.putString("arena", pair.arena.id()); row.putString("dimension", pair.arena.dimension());
            row.putString("operator", actor(source)); row.putLong("created", now); row.putString("state", "PRESERVED_BEFORE_REMOVAL");
            row.putUUID("previousCanonical", WorldTierData.get(source.getServer()).liveBoss(pair.arena.id()).orElseThrow());
            row.put("entity", snapshot); data.bossQuarantine().put( row); data.flush(source.getServer());
            verifyArchive(source.getServer(), row);
            // Save is proven durable before the only destructive operation. Do not invoke kill/death callbacks.
            validate(source.getServer(), keeper, duplicate);
            WorldTierData world = WorldTierData.get(source.getServer());
            UUID claimed = world.liveBoss(pair.arena.id()).orElseThrow();
            if (claimed.equals(duplicate) && !world.replaceLiveBoss(pair.arena.id(), duplicate, keeper))
                throw new IllegalStateException("Canonical keeper transfer rejected; nothing removed");
            if (!world.liveBoss(pair.arena.id()).filter(keeper::equals).isPresent())
                throw new IllegalStateException("Canonical keeper changed; nothing removed");
            var reservation = world.arena(pair.arena.id()).orElseThrow();
            if (duplicate.equals(reservation.activeEncounterId()))
                world.putArena(reservation.release(duplicate).recover(keeper, source.getServer().overworld().getGameTime()));
            if (!world.flush(source.getServer())) throw new IllegalStateException("Canonical keeper transfer was not durably saved; nothing removed");
            pair.duplicate.getPersistentData().putBoolean("JEMQuarantinedDuplicate", true);
            pair.duplicate.discard();
            if (pair.duplicate.isRemoved()) {
                world.clearLiveBoss(pair.arena.id(), duplicate);
                if (!world.flush(source.getServer())) throw new IllegalStateException("Duplicate is archived; identity retirement requires recovery before further repair");
            }
            row.putString("state", pair.duplicate.isRemoved() ? "QUARANTINED" : "REMOVAL_INCOMPLETE");
            data.flush(source.getServer());
            tell(source, "Quarantine=" + duplicate + "; keeper=" + keeper + "; ArenaId=" + pair.arena.id()
                    + "; state=" + row.getString("state") + ". Full original NBT remains in jem_smp_v2.dat table " + ARCHIVE + ".");
            return pair.duplicate.isRemoved() ? 1 : 0;
        } catch (RuntimeException | IOException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Explicit boss quarantine failed token={} keeper={} duplicate={}", token, keeper, duplicate, failure);
            return reject(source, "Quarantine did not complete cleanly; inspect archive and loaded UUIDs. No automatic retry or restore is performed.");
        }
    }

    private static Pair validate(MinecraftServer server, UUID keeperId, UUID duplicateId) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return validateInScope(server, keeperId, duplicateId); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> validateInScope(server, keeperId, duplicateId));
    }

    private static Pair validateInScope(MinecraftServer server, UUID keeperId, UUID duplicateId) {
        if (!server.isDedicatedServer() || !server.isSameThread() || keeperId.equals(duplicateId)) throw new IllegalStateException("Dedicated server thread and two different UUIDs required.");
        LivingEntity keeper = loaded(server, keeperId), duplicate = loaded(server, duplicateId);
        if (keeper == null || duplicate == null || keeper.level() != duplicate.level() || keeper.getType() != duplicate.getType())
            throw new IllegalStateException("Both exact same-type entities must be alive and fully loaded in one dimension.");
        var first = WorldTierApi.encounter(keeper).orElse(null); var second = WorldTierApi.encounter(duplicate).orElse(null);
        WorldTierData data = WorldTierData.get(server);
        Set<EncounterProvenance> allowed = Set.of(EncounterProvenance.STRUCTURE, EncounterProvenance.NATIVE, EncounterProvenance.REVIVAL);
        if (first == null || second == null || first.arenaId() == null || first.arenaId().isBlank()
                || second.arenaId() == null || second.arenaId().isBlank()
                || !data.canonicalArenaId(first.arenaId()).equals(data.canonicalArenaId(second.arenaId()))
                || !allowed.contains(first.provenance()) || !allowed.contains(second.provenance()))
            throw new IllegalStateException("Same canonical ArenaId and proven STRUCTURE/NATIVE/REVIVAL provenance required; admin/egg/unverified/Raid identities are audit-only.");
        var profile = WorldTierApi.profile(keeper).orElse(null); var other = WorldTierApi.profile(duplicate).orElse(null);
        if (profile == null || other == null || !profile.key().equals(other.key()) || profile.entityIds().size() != 1)
            throw new IllegalStateException("Unambiguous single-type registered profile required; multi-boss/phase profiles are audit-only.");
        ArenaRecord arena = data.arena(data.canonicalArenaId(first.arenaId())).orElse(null);
        String canonical = data.canonicalArenaId(second.arenaId());
        boolean aliasedRevival = second.provenance() == EncounterProvenance.REVIVAL
                && !second.arenaId().equals(canonical);
        if (arena == null || !arena.structureArena() || !arena.profileKey().equals(profile.key())
                || !arena.dimension().equals(keeper.level().dimension().location().toString())
                || !arena.bounds().isInside(keeper.blockPosition())
                || !arena.bounds().isInside(duplicate.blockPosition()) && !aliasedRevival
                || !data.nativeAnchorVerified(arena.id())
                || !data.liveBoss(arena.id()).filter(id -> id.equals(keeperId) || id.equals(duplicateId)).isPresent())
            throw new IllegalStateException("Verified structural arena and an exact keeper/candidate claim required; outside candidates need a persisted canonical alias and REVIVAL proof.");
        if (data.arenas().size() > ARENA_LIMIT) throw new IllegalStateException("Arena registry exceeds bounded repair review; nothing removed.");
        for (ArenaRecord candidate : data.arenas()) {
            if (!data.canonicalArenaId(candidate.id()).equals(arena.id())
                    && data.liveBoss(candidate.id()).filter(duplicateId::equals).isPresent())
                throw new IllegalStateException("Candidate is canonically claimed; independent identities are never removed.");
            if (!data.canonicalArenaId(candidate.id()).equals(arena.id()) && candidate.structureArena() && candidate.dimension().equals(arena.dimension())
                    && candidate.profileKey().equals(arena.profileKey())
                    && (candidate.bounds().isInside(keeper.blockPosition()) || candidate.bounds().isInside(duplicate.blockPosition())))
                throw new IllegalStateException("Overlapping independent structure identity is ambiguous; audit-only.");
        }
        for (LivingEntity entity : List.of(keeper, duplicate)) {
            ServerLevel level = (ServerLevel) entity.level();
            if (level.getEntity(entity.getUUID()) != entity || !level.hasChunkAt(entity.blockPosition())
                    || entity.isRemoved() || !entity.isAlive() || entity.isMultipartEntity() || entity.isPassenger() || !entity.getPassengers().isEmpty()
                    || !HostedEncounterApi.status(entity).isEmpty() || HostedEncounterApi.isRaid(entity) || HostedRaidScaling.prepared(entity)
                    || Set.of("COMMAND", "SPAWN_EGG", "DISPENSER").contains(entity.getPersistentData().getString("forge:spawn_type"))
                    || entity.getPersistentData().hasUUID("JEMHostedBoss")
                    || entity.getPersistentData().getBoolean(EventSession.SPAWNED) || !HostedEncounterApi.held(entity))
                throw new IllegalStateException("Both entities must be dormant loaded standalone roots outside all hosted/Raid/event ownership.");
        }
        if (arena.activeEncounterId() != null && !arena.activeEncounterId().equals(keeperId) && !arena.activeEncounterId().equals(duplicateId))
            throw new IllegalStateException("A separate encounter/reservation still owns the arena; repair is unavailable.");
        var smp = SmpData.get(server);
        for (var rows : List.of(smp.parties().currentRows(), smp.events().currentRows())) {
            if (rows.size() > ENTITY_LIMIT) throw new IllegalStateException("Party/event registry exceeds bounded repair review; nothing removed.");
            for (CompoundTag row : rows) {
                if (SmpData.closed(row)) continue;
                if (row.hasUUID("bossEntity") && (row.getUUID("bossEntity").equals(keeperId) || row.getUUID("bossEntity").equals(duplicateId))
                        || data.canonicalArenaId(row.getString("raidArenaId")).equals(arena.id())
                        || data.canonicalArenaId(row.getString("arenaId")).equals(arena.id()))
                    throw new IllegalStateException("An open party/event owns this boss/arena; close it through the canonical lifecycle first.");
            }
        }
        return new Pair(keeper, duplicate, arena);
    }

    private static LivingEntity loaded(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) if (level.getEntity(id) instanceof LivingEntity entity && entity.isAlive()) return entity;
        return null;
    }
    private static void verifyArchive(MinecraftServer server, CompoundTag expected) throws IOException {
        var file = server.getWorldPath(LevelResource.ROOT).resolve("data/jem_smp_v2.dat").toFile();
        var rows = NbtIo.readCompressed(file).getCompound("data").getCompound("tables").getList(ARCHIVE, Tag.TAG_COMPOUND);
        for (int index = 0; index < rows.size(); index++) {
            CompoundTag row = rows.getCompound(index);
            if (row.equals(expected)) return;
        }
        throw new IOException("Exact boss quarantine snapshot was not persisted");
    }
    private static int archive(CommandSourceStack source, UUID id) {
        var serverForMutation = source.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { return archiveInScope(source, id); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> archiveInScope(source, id));
    }

    private static int archiveInScope(CommandSourceStack source, UUID id) {
        CompoundTag row = SmpData.get(source.getServer()).bossQuarantine().current( id);
        if (row == null) return reject(source, "No quarantine snapshot for that UUID.");
        tell(source, "Quarantine=" + id + " keeper=" + row.getUUID("keeper") + " ArenaId=" + row.getString("arena")
                + " dimension=" + row.getString("dimension") + " state=" + row.getString("state")
                + "; original full entity NBT is retained in jem_smp_v2.dat. Restoration requires separate reviewed recovery; this command never respawns duplicates.");
        return 1;
    }
    private static String actor(CommandSourceStack source) { return source.getEntity() == null ? source.getTextName() : source.getEntity().getStringUUID(); }
    private static void tell(CommandSourceStack source, String text) { source.sendSuccess(() -> Component.literal(text), false); }
    private static int reject(CommandSourceStack source, String text) { source.sendFailure(Component.literal(text)); return 0; }
}
