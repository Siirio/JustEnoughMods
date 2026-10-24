package com.siirio.jemserver.smp.events;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.siirio.jemserver.smp.*;
import com.siirio.jemserver.Landmarks;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.RaidArenaApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = "jem_server")
public final class StructureStaging {
    public record RaidTarget(UUID id, ResourceLocation boss, String dimension, BlockPos center, BoundingBox bounds) {}
    private static final int AREA_LIMIT = 1024;
    private static final int CHECK_INTERVAL = 10;
    private static final int BOUNDARY_LIMIT = 16;
    private static final int LANDMARK_RADIUS = 10;
    private static final int EXIT_SEARCH_MARGIN = 6;
    private static final String RAID_EXIT_PENDING = "raidArenaExitPending";
    private static final LinkedHashMap<UUID, Area> AREAS = new LinkedHashMap<>();
    private static final Map<UUID, UUID> INSIDE = new HashMap<>();
    private static final Map<UUID, UUID> NEAR = new HashMap<>();
    private static final Map<UUID, Optional<UUID>> PARTY_CACHE = new HashMap<>();
    private static final Map<UUID, Presence> PRESENCE = new HashMap<>();
    private static final LinkedHashMap<String, Optional<Area>> NATIVE_LOOKUPS = new LinkedHashMap<>();
    private static final Map<String, Long> REGISTERED_AT = new HashMap<>();

    private StructureStaging() {}

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) {
        AREAS.clear(); INSIDE.clear(); NEAR.clear(); PARTY_CACHE.clear(); PRESENCE.clear(); NATIVE_LOOKUPS.clear(); REGISTERED_AT.clear();
    }

    private static Area discover(ServerLevel level, BlockPos position) {
        refreshAreas(level);
        for (Area area : AREAS.values()) if (area.contains(level, position)) return area;
        var discovered = com.siirio.jemworldbosstiers.api.StructureEncounterApi.discover(level, position);
        if (discovered.isEmpty()) return null;
        var nativeArena = discovered.get();
        var area = new Area(nativeArena.id(), nativeArena.dimension(), nativeArena.boss(), nativeArena.bounds());
        AREAS.put(area.id(), area);
        return area;
    }

    /** Projection only: persisted WorldTierData owns registration, geometry and identity. */
    private static void refreshAreas(ServerLevel level) {
        String dimension=level.dimension().location().toString();
        long tick=level.getGameTime();
        if(Objects.equals(REGISTERED_AT.put(dimension,tick),tick)) return;
        for (var arena : com.siirio.jemworldbosstiers.api.StructureEncounterApi.registeredArenas(
                level.getServer(), level.dimension().location()))
            AREAS.put(arena.id(), new Area(arena.id(), arena.dimension(), arena.boss(), arena.bounds()));
    }

    private static PartyState party(ServerLevel level, UUID id) {
        var cached = PARTY_CACHE.get(id);
        if (cached != null) {
            if (cached.isEmpty()) return null;
            var current = SmpData.get(level.getServer()).parties().snapshot(cached.get());
            if (current != null && !current.closed()) return current;
        }
        var row = SmpData.get(level.getServer()).parties().snapshots().stream()
                .filter(value -> value.hasStructureId() && value.structureId().equals(id) && !value.closed()).findFirst();
        if (PARTY_CACHE.size() >= AREA_LIMIT) PARTY_CACHE.clear();
        PARTY_CACHE.put(id, row.map(PartyState::id));
        return row.orElse(null);
    }

    public static boolean allowNative(Level level, BlockPos position) {
        return true;
    }

    public static boolean allowNativeSpawn(Level level, BlockPos position) {
        if (level instanceof ServerLevel nativeLevel && !SmpEnvironment.active(nativeLevel.getServer())) return true;
        if (!(level instanceof ServerLevel serverLevel) || !allowNative(level, position)) return !(level instanceof ServerLevel);
        Area area = discover(serverLevel, position);
        return area == null || livingBoss(serverLevel, area) == null;
    }

    public static boolean managed(LivingEntity boss) {
        if (!(boss.level() instanceof ServerLevel level) || !SmpEnvironment.active(level.getServer())) return false;
        return bossArea(level,boss)!=null;
    }

    public static boolean prompt(ServerPlayer player,LivingEntity boss) {
        if (!SmpEnvironment.active(player)) return false;
        Area area=bossArea(player.serverLevel(),boss);
        if(area==null) return false;
        prompt(player,area.id());
        return true;
    }

    private static Area bossArea(ServerLevel level,LivingEntity boss) {
        com.siirio.jemworldbosstiers.api.StructureEncounterApi.bind(boss).ifPresent(nativeArena -> {
            var area = new Area(nativeArena.id(), nativeArena.dimension(), nativeArena.boss(), nativeArena.bounds());
            AREAS.put(area.id(), area);
        });
        ResourceLocation bossType=BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        Area area = AREAS.values().stream()
                .filter(candidate->candidate.dimension().equals(level.dimension().location().toString())&&candidate.boss().equals(bossType)&&candidate.containsColumn(boss.position()))
                .min(Comparator.comparingDouble(candidate->candidate.distance(boss.position())))
                .orElse(null);
        if (area != null && WorldTierApi.encounter(boss).map(value -> value.provenance() != com.siirio.jemworldbosstiers.encounter.EncounterProvenance.RAID_EVENT).orElse(true))
            RaidArenaApi.observeNativeBoss(level.getServer(), area.id().toString(), boss);
        return area;
    }

    public static boolean arenaAvailable(net.minecraft.server.MinecraftServer server,String id) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return arenaAvailableInScope(server, id); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> arenaAvailableInScope(server, id));
    }

    private static boolean arenaAvailableInScope(net.minecraft.server.MinecraftServer server,String id) {
        if (!SmpEnvironment.active(server)) return false;
        return SmpData.get(server).parties().currentRows().stream().noneMatch(row->row.hasStructureId()
                &&row.structureId().toString().equals(id)&&!SmpData.closed(row));
    }

    public static boolean raidAvailable(ServerPlayer player, UUID id) {
        if (!SmpEnvironment.active(player)) return false;
        Area area = AREAS.get(id);
        EventState event = EventScheduler.active(player.server, "BOSS_RAID");
        if (area == null || !area.near(player.serverLevel(), player.position())
                || party(player.serverLevel(), id) != null) return false;
        if (event == null) return RaidArenaApi.compatible(player.server, id.toString(), area.boss());
        if (event.combatStarted()) return false;
        return event.hasRaidArenaId() ? event.raidArenaId().equals(id.toString())
                : RaidArenaApi.compatible(player.server, id.toString(), area.boss());
    }

    public static RaidTarget availableRaid(ServerLevel level,BlockPos position) {
        if (!SmpEnvironment.active(level.getServer())) return null;
        Area area=nearby(level,Vec3.atCenterOf(position));
        if(area==null||party(level,area.id())!=null||!RaidArenaApi.compatible(level.getServer(),area.id().toString(),area.boss())) return null;
        BlockPos center=RaidArenaApi.respawnPosition(level.getServer(),area.id().toString(),area.boss()).orElse(area.center());
        return new RaidTarget(area.id(),area.boss(),area.dimension(),center,area.bounds());
    }

    public static void choose(ServerPlayer player, UUID id, String mode, boolean solo) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { chooseInScope(player, id, mode, solo); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> chooseInScope(player, id, mode, solo));
    }

    private static void chooseInScope(ServerPlayer player, UUID id, String mode, boolean solo) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        Area area = AREAS.get(id);
        SmpRecords.require(area != null && area.near(player.serverLevel(), player.position()), "travel_to_arena");
        SmpRecords.require(!locked(player, area), "unavailable");
        SmpRecords.require(party(player.serverLevel(), id) == null, "arena_busy");
        SmpRecords.require(blockers(player.serverLevel(), area, Set.of(player.getUUID())).isEmpty(), "arena_occupied");
        if (mode.equals("BOSS_RAID")) {
            EventState event = EventScheduler.active(player.server, "BOSS_RAID");
            SmpRecords.require(raidAvailable(player, id), "raid_unavailable");
            boolean created = event == null;
            if (created) {
                event = SmpData.get(player.server).events().create(player.getUUID());
                event.slot("manual:raid:" + event.id());
                event.activity("BOSS_RAID");
                event.ordinaryRaid(true);
                event.state("ACTIVE");
                event.started(System.currentTimeMillis());
                event.ends(System.currentTimeMillis() + Math.min(3, SmpConfig.EVENT_HOURS.get()) * 3600000L);
            }
            if (!RaidEvent.claimArena(player.server, event, id.toString(), area.boss(), area.dimension(), area.center(), area.bounds())) {
                if (created) EventScheduler.cancelPreparation(player.server, event);
                SmpRecords.require(false, "arena_busy");
            }
            PARTY_CACHE.remove(id);
            EventParties.join(player, event, solo);
            PARTY_CACHE.put(id, SmpData.get(player.server).parties().currentRows().stream()
                    .filter(value -> value.hasStructureId() && value.structureId().equals(id) && !SmpData.closed(value)).map(value -> value.id()).findFirst());
            return;
        }
        SmpRecords.require(mode.equals("BOSS_FIGHT"), "unavailable");
        EventState activeRaid = EventScheduler.active(player.server, "BOSS_RAID");
        SmpRecords.require(activeRaid == null || !activeRaid.raidArenaId().equals(id.toString()), "raid_unavailable");
        SmpRecords.require(livingBoss(player.serverLevel(), area) != null, "boss_not_respawned");
        PartyState row = party(player.serverLevel(), id);
        if (row == null) {
            row = Parties.createHosted(player, SmpPartyRequest.hosted(
                    Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(area.boss()).getDescriptionId()).getString(), solo));
            attachArena(row,id,area.boss(),area.dimension(),area.center(),area.bounds());
            row.nativePending(true);
            PARTY_CACHE.put(id, Optional.of(row.id()));

        }
        if (solo) {
            Parties.makeSolo(player, row);
            HostedParties.start(player, row);
        } else SmpNetwork.open(player, "parties", row.id());
    }

    public static boolean start(ServerPlayer host, PartyState row, List<UUID> ids) {
        var serverForMutation = host.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return startInScope(host, row, ids); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> startInScope(host, store.parties().bind(row), ids));
    }

    private static boolean startInScope(ServerPlayer host, PartyState row, List<UUID> ids) {
        if (!SmpEnvironment.active(host)) return false;
        if (!row.nativePending()) return false;
        Area area = row.hasStructureId() ? AREAS.get(row.structureId()) : null;
        SmpRecords.require(area != null, "unavailable");
        requireParticipants(host, row, ids, area);
        SmpRecords.require(!locked(host, area), "unavailable");
        SmpRecords.require(livingBoss(host.serverLevel(), area) != null, "boss_not_respawned");
        SmpRecords.require(blockers(host.serverLevel(), area, new HashSet<>(ids)).isEmpty(), "arena_occupied");
        row.state("ACTIVE");

        bindExisting(host.serverLevel(), area, row, ids);
        SmpRecords.require(row.hasBossEntity() && !row.nativePending(), "unavailable");
        host.sendSystemMessage(Component.translatable("jem.event.native_ritual_ready"));
        return true;
    }

    public static void prompt(ServerPlayer player, UUID id) {
        if (!SmpEnvironment.active(player)) return;
        Area area = AREAS.get(id);
        SmpRecords.require(area != null && area.near(player.serverLevel(), player.position()), "travel_to_arena");
        SmpRecords.require(!physicallyLocked(player.serverLevel(), area) && !locked(player, area), "unavailable");
        EventNetwork.prompt(player, area.id(), EventEntryKind.BOSS_STRUCTURE, Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(area.boss()).getDescriptionId()).getString(), area.boss().toString());
    }

    public static boolean arrive(ServerPlayer player, PartyState row) {
        if (!SmpEnvironment.active(player)) return false;
        if (!row.hasStructureId()) return false;
        Area area = AREAS.get(row.structureId());
        SmpRecords.require(area != null, "unavailable");
        ServerLevel level = level(player.server, area);
        SmpRecords.require(level != null, "unavailable");
        BlockPos outside=safeExit(player,level,area);
        if(outside==null) throw new IllegalArgumentException("no_safe_arrival");
        teleport(player,level,outside);
        return true;
    }

    public static void finish(net.minecraft.server.MinecraftServer server, PartyState row) {
        HostedBoundary.clearParty(row);
        if (row.raid()) finishRaidParticipants(server, row);
        else PartyTeleportFlow.finish(server,row);
        if (!row.hasStructureId()) {
            return;
        }
        Area area = AREAS.get(row.structureId());
        if (area == null) return;
        ServerLevel level = level(server, area);
        if (level == null) return;
        PARTY_CACHE.remove(area.id());
    }

    private static void finishRaidParticipants(net.minecraft.server.MinecraftServer server, PartyState party) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishRaidParticipantsInScope(server, party); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishRaidParticipantsInScope(server, store.parties().bind(party)));
    }

    private static void finishRaidParticipantsInScope(net.minecraft.server.MinecraftServer server, PartyState party) {
        for (UUID id : SmpRecords.memberIds(party)) {
            if (!Parties.accepted(party, id)) continue;
            PartyMember member = party.member(id);
            member.raidArenaExitPending(true);
        }
        SmpData.get(server).flush(server);
        for (UUID id : SmpRecords.memberIds(party)) {
            PartyMember member=party.member(id);
            if(!member.raidArenaExitPending()) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && player.isAlive() && !finishRaidExit(player, party, member)) throw new IllegalStateException("return_pending");
        }
        PartyTeleportFlow.finish(server, party);

    }

    private static boolean finishRaidExit(ServerPlayer player, PartyState party, PartyMember member) {
        if (!player.isAlive()) return false;
        ResourceLocation dimension = ResourceLocation.tryParse(party.dimension());
        ServerLevel level = dimension == null ? null : player.server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimension));
        var saved = party.structureBounds();
        if (level == null || saved == null) {
            if (!PartyTeleportFlow.forceReturn(player, party)) return false;
        } else {
            BoundingBox shell = arenaShell(new BoundingBox(saved.minX(), saved.minY(), saved.minZ(), saved.maxX(), saved.maxY(), saved.maxZ()));
            boolean insideArena = player.serverLevel() == level && SolidBoundary.movementBox(player).intersects(
                    new AABB(shell.minX(), shell.minY(), shell.minZ(), shell.maxX()+1D, shell.maxY()+1D, shell.maxZ()+1D));
            if (insideArena) {
                BlockPos destination = safeOutside(player, level, shell, player.blockPosition());
                if (destination != null) teleport(player, level, destination);
                else if (!PartyTeleportFlow.forceReturn(player, party)) return false;
            }
        }
        SmpData.flushPlayerPosition(player);
        member.clearRaidArenaExitPending();
        return true;
    }

    private static void finishPendingRaidExit(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishPendingRaidExitInScope(player); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishPendingRaidExitInScope(player));
    }

    private static void finishPendingRaidExitInScope(ServerPlayer player) {
        SmpData data = SmpData.get(player.server);
        for (PartyState party : data.parties().currentRows()) {
            PartyMember member = party.member(player.getUUID());
            if (member == null || !member.raidArenaExitPending()) continue;
            finishRaidExit(player, party, member);
        }
    }

    /** Terminal choice must not leave a survivor sealed behind the next dormant boss. */
    public static boolean safeTerminalExit(ServerPlayer player, PartyState party) {
        if (!party.hasStructureBounds() || !party.dimension().equals(player.level().dimension().location().toString())) return true;
        refreshAreas(player.serverLevel());
        Area registered=party.hasStructureId()?AREAS.get(party.structureId()):null;
        if(!party.raid() && (registered==null || !guarded(player.serverLevel(),registered))) return true;
        var saved = party.structureBounds();
        var bounds = arenaShell(new BoundingBox(saved.minX(), saved.minY(), saved.minZ(), saved.maxX(), saved.maxY(), saved.maxZ()));
        var arena = new AABB(bounds.minX() - SolidBoundary.WALL_THICKNESS, bounds.minY(), bounds.minZ() - SolidBoundary.WALL_THICKNESS,
                bounds.maxX() + 1D + SolidBoundary.WALL_THICKNESS, bounds.maxY() + 1D, bounds.maxZ() + 1D + SolidBoundary.WALL_THICKNESS);
        if (!SolidBoundary.movementBox(player).intersects(arena)) return true;
        BlockPos destination = safeOutside(player, player.serverLevel(), bounds, player.blockPosition());
        if (destination == null) return false;
        teleport(player, player.serverLevel(), destination);
        return !SolidBoundary.movementBox(player).intersects(arena);
    }

    public static boolean hasPendingRaidExit(PartyState party) {
        return SmpRecords.memberIds(party).stream().anyMatch(id ->
                party.member(id).raidArenaExitPending());
    }

    public static boolean hasPendingRaidExit(EventState event) {
        return event.members().values().stream().anyMatch(member -> member.raidArenaExitPending());
    }

    public static void attachArena(PartyState record, UUID id, ResourceLocation boss, String dimension, BlockPos center, BoundingBox bounds) {
        record.structureId(id); record.structureBoss(boss.toString());
        record.structureBounds(savedBounds(bounds));
        EventRegions.locate(record, dimension, center, bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
        record.minY(bounds.minY()); record.maxY(bounds.maxY());
    }

    public static void attachArena(EventState record, UUID id, ResourceLocation boss, String dimension, BlockPos center, BoundingBox bounds) {
        record.structureId(id); record.structureBoss(boss.toString());
        record.structureBounds(savedBounds(bounds));
        EventRegions.locate(record, dimension, center, bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
        record.minY(bounds.minY()); record.maxY(bounds.maxY());
    }

    private static StructureBounds savedBounds(BoundingBox bounds) {
        return new StructureBounds(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    private static BoundingBox combatBounds(Area area, StructureBounds saved, LivingEntity boss) {
        return area != null ? area.bounds() : saved == null ? BossStaging.bounds(boss)
                : new BoundingBox(saved.minX(), saved.minY(), saved.minZ(), saved.maxX(), saved.maxY(), saved.maxZ());
    }

    public static BoundingBox prepareCombat(ServerLevel level, PartyState record, LivingEntity boss) {
        Area area = record.hasStructureId() ? AREAS.get(record.structureId()) : null;
        var bounds = combatBounds(area, record.structureBounds(), boss);
        if (area == null) {
            EventRegions.locate(record, level.dimension().location().toString(), boss.blockPosition(), bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
            record.minY(bounds.minY()); record.maxY(bounds.maxY());
        }
        return bounds;
    }

    public static BoundingBox prepareCombat(ServerLevel level, EventState record, LivingEntity boss) {
        Area area = record.hasStructureId() ? AREAS.get(record.structureId()) : null;
        var bounds = combatBounds(area, record.structureBounds(), boss);
        if (area == null) {
            EventRegions.locate(record, level, boss.blockPosition(), bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
            record.minY(bounds.minY()); record.maxY(bounds.maxY());
        }
        return bounds;
    }

    private static ServerLevel level(net.minecraft.server.MinecraftServer server, Area area) {
        ResourceLocation dimension = ResourceLocation.tryParse(area.dimension());
        return dimension == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimension));
    }

    static boolean safeOutside(ServerPlayer player, ServerLevel level, BlockPos position) {
        if (!level.hasChunkAt(position) || !level.getWorldBorder().isWithinBounds(position)
                || !level.getBlockState(position.below()).isFaceSturdy(level, position.below(), net.minecraft.core.Direction.UP)
                || !level.getFluidState(position).isEmpty() || !level.getFluidState(position.above()).isEmpty()
                || dangerous(level.getBlockState(position.below())) || dangerous(level.getBlockState(position))
                || dangerous(level.getBlockState(position.above()))) return false;
        return safeBody(player,level,position);
    }

    private static boolean dangerous(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.FIRE) || state.is(net.minecraft.world.level.block.Blocks.LAVA)
                || state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                || state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) || state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                || state.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE) || state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE) || state.is(net.minecraft.world.level.block.Blocks.END_PORTAL)
                || state.is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL);
    }

    private static boolean safe(Mob mob,ServerLevel level,BlockPos position) {
        if(!level.hasChunkAt(position)||!level.getWorldBorder().isWithinBounds(position)
                ||!level.getBlockState(position.below()).isFaceSturdy(level,position.below(),net.minecraft.core.Direction.UP)
                ||!level.getFluidState(position).isEmpty()||!level.getFluidState(position.above()).isEmpty()) return false;
        return safeBody(mob,level,position);
    }

    private static boolean safeBody(net.minecraft.world.entity.Entity entity,ServerLevel level,BlockPos position) {
        AABB box=SolidBoundary.destinationBox(entity,Vec3.atBottomCenterOf(position));
        if(box.minY<level.getMinBuildHeight() || box.maxY>=level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box)) return false;
        for(BlockPos occupied:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),
                BlockPos.containing(box.maxX-1.0E-7,box.maxY-1.0E-7,box.maxZ-1.0E-7)))
            if(!level.hasChunkAt(occupied)||!level.getFluidState(occupied).isEmpty()||dangerous(level.getBlockState(occupied))) return false;
        return level.noCollision(entity,box);
    }

    private static BlockPos safeExit(ServerPlayer player, ServerLevel level, Area area) {
        return safeExit(player,level,area,player.blockPosition());
    }

    private static BlockPos safeExit(ServerPlayer player,ServerLevel level,Area area,BlockPos origin) {
        return safeOutside(player,level,area.shell(),origin);
    }

    private static BlockPos safeExit(Mob mob,ServerLevel level,Area area) {
        return safeExit(mob,level,area.shell());
    }

    private static BlockPos safeExit(Mob mob,ServerLevel level,BoundingBox bounds) {
        AABB volume=new AABB(bounds.minX()-SolidBoundary.WALL_THICKNESS,bounds.minY(),bounds.minZ()-SolidBoundary.WALL_THICKNESS,
                bounds.maxX()+1D+SolidBoundary.WALL_THICKNESS,bounds.maxY()+1D,bounds.maxZ()+1D+SolidBoundary.WALL_THICKNESS);
        return safeExit(level,bounds,mob.blockPosition(),position->!SolidBoundary.destinationBox(mob,Vec3.atBottomCenterOf(position)).intersects(volume)
                &&safe(mob,level,position));
    }

    /** One activation preflight, not a tick scan. No valid boss/support/pet is removed. */
    static boolean ejectUnrelated(ServerLevel level,BoundingBox bounds,LivingEntity boss,Collection<UUID> participants) {
        com.siirio.jemworldbosstiers.encounter.HostedGroup.stage(boss);
        var shell=arenaShell(bounds);
        var volume=new AABB(shell.minX(),shell.minY(),shell.minZ(),shell.maxX()+1D,shell.maxY()+1D,shell.maxZ()+1D);
        Map<Mob,BlockPos> exits=new LinkedHashMap<>();
        for(Mob mob:level.getEntitiesOfClass(Mob.class,volume,entity->entity.isAlive()&&entity!=boss)) {
            if(com.siirio.jemworldbosstiers.encounter.HostedGroup.owner(mob)==boss) continue;
            var owner=HostedEncounterApi.damageOwner(mob);
            if(owner instanceof ServerPlayer player&&participants.contains(player.getUUID())) continue;
            // A neighboring registered boss is never collateral cleanup.
            if(WorldTierApi.profile(mob).isPresent() || !SolidBoundary.players(mob).isEmpty()) return false;
            var exit=safeExit(mob,level,shell);
            if(exit==null) return false;
            exits.put(mob,exit);
        }
        for(var exit:exits.entrySet()) {
            Mob mob=exit.getKey();
            if(!BossTeleportSafety.apply(mob,level,Vec3.atBottomCenterOf(exit.getValue()))) return false;
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        return true;
    }

    static BlockPos safeOutside(ServerPlayer player,ServerLevel level,BoundingBox bounds,BlockPos origin) {
        return safeExit(level,bounds,origin,position -> {
            AABB target = SolidBoundary.destinationBox(player, Vec3.atBottomCenterOf(position));
            AABB arena = new AABB(bounds.minX()-.25D, bounds.minY(), bounds.minZ()-.25D, bounds.maxX()+1.25D, bounds.maxY()+1D, bounds.maxZ()+1.25D);
            return !target.intersects(arena) && safeOutside(player,level,position);
        });
    }

    static BlockPos outsideAnchor(BoundingBox bounds, BlockPos origin) {
        int x = net.minecraft.util.Mth.clamp(origin.getX(), bounds.minX(), bounds.maxX());
        int z = net.minecraft.util.Mth.clamp(origin.getZ(), bounds.minZ(), bounds.maxZ());
        int left = Math.abs(origin.getX() - bounds.minX());
        int right = Math.abs(bounds.maxX() - origin.getX());
        int top = Math.abs(origin.getZ() - bounds.minZ());
        int bottom = Math.abs(bounds.maxZ() - origin.getZ());
        int edge = Math.min(Math.min(left, right), Math.min(top, bottom));
        if (edge == left) return new BlockPos(bounds.minX() - 2, origin.getY(), z);
        if (edge == right) return new BlockPos(bounds.maxX() + 2, origin.getY(), z);
        if (edge == top) return new BlockPos(x, origin.getY(), bounds.minZ() - 2);
        return new BlockPos(x, origin.getY(), bounds.maxZ() + 2);
    }

    private static BlockPos safeExit(ServerLevel level,BoundingBox bounds,BlockPos origin,java.util.function.Predicate<BlockPos> safe) {
        int minY=Math.max(level.getMinBuildHeight()+1,bounds.minY()-EXIT_SEARCH_MARGIN);
        int maxY=Math.min(level.getMaxBuildHeight()-2,bounds.maxY()+EXIT_SEARCH_MARGIN);
        int anchorY=Math.max(minY,Math.min(maxY,origin.getY()));
        for(int offset=2;offset<=EXIT_SEARCH_MARGIN;offset++) {
            int minX=bounds.minX()-offset,maxX=bounds.maxX()+offset,minZ=bounds.minZ()-offset,maxZ=bounds.maxZ()+offset;
            BlockPos best=null;
            double distance=Double.MAX_VALUE;
            for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
                if(x!=minX&&x!=maxX&&z!=minZ&&z!=maxZ) continue;
                if(!level.hasChunkAt(new BlockPos(x,anchorY,z))) continue;
                for(int delta=0;delta<=maxY-minY;delta++) for(int direction:delta==0?new int[]{0}:new int[]{delta,-delta}) {
                    int y=anchorY+direction;
                    if(y<minY||y>maxY) continue;
                    BlockPos position=new BlockPos(x,y,z);
                    if(!safe.test(position)) continue;
                    double candidate=position.distSqr(origin);
                    if(candidate<distance) {best=position;distance=candidate;}
                    break;
                }
            }
            if(best!=null) return best;
        }
        return null;
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos position) {
        if(player.serverLevel()==level && (player.isPassenger() || player.isVehicle())) {
            SmpRecords.require(BossTeleportSafety.apply((net.minecraft.world.entity.Entity)player,level,Vec3.atBottomCenterOf(position)), "no_safe_arrival");
            return;
        }
        BossTeleportSafety.apply(player, level, Vec3.atBottomCenterOf(position), player.getYRot(), player.getXRot());
    }

    private static void bindExisting(ServerLevel level, Area area, PartyState row, List<UUID> ids) {
        LivingEntity boss = livingBoss(level, area);
        if (boss != null) bind(level, boss, row, ids);
    }

    private static void bind(ServerLevel level, LivingEntity boss, PartyState row, List<UUID> ids) {
        var serverForMutation = level.getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { bindInScope(level, boss, row, ids); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> bindInScope(level, boss, store.parties().bind(row), ids));
    }

    private static void bindInScope(ServerLevel level, LivingEntity boss, PartyState row, List<UUID> ids) {
        if (row.hasBossEntity() || !row.nativePending() || HostedEncounterApi.isRaid(boss)) return;
        Area area = bossArea(level,boss);
        if (area == null || livingBoss(level, area) != boss) return;
        SmpRecords.require(ejectUnrelated(level,area.bounds(),boss,ids), "arena_occupied");
        boss.getPersistentData().putBoolean("jem_solo", row.solo());
        if (HostedEncounterApi.start(boss, ids, false, prepareCombat(level,row,boss), row.id(),
                attempt -> HostedParties.commitHostedStart(level.getServer(), row, boss, attempt))) {
            row.hostedAttempt(HostedEncounterApi.attemptId(boss));
            row.bossEntity(boss.getUUID());
            row.nativePending(false);
            Profiles.count(level.getServer(), row.owner(), ProfileCounter.BOSSES_HOSTED);

        } else if (HostedEncounterApi.status(boss).isEmpty()) {
            row.clearBossEntity(); row.hostedAttempt(null); row.nativePending(true);
            SmpData.get(level.getServer()).flush(level.getServer());
        }
    }

    public static void stageBoss(ServerLevel level,SmpRecord record,LivingEntity boss) {
        if(!boss.isAlive()||boss.isRemoved()) return;
        HostedEncounterApi.hold(boss);
    }



    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void spawned(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity boss)
                || !SmpEnvironment.active(level.getServer()) || WorldTierApi.profile(boss).isEmpty() || boss.getPersistentData().getBoolean(EventSession.SPAWNED)) return;
        Area area = bossArea(level,boss);
        if (area == null) return;
        PRESENCE.remove(area.id());
        var encounter = WorldTierApi.encounter(boss).orElse(null);
        String spawnType = boss.getPersistentData().getString("forge:spawn_type");
        boolean adminSpawn = spawnType.equals("SPAWN_EGG") || spawnType.equals("COMMAND")
                || encounter != null && encounter.provenance() == com.siirio.jemworldbosstiers.encounter.EncounterProvenance.UNVERIFIED;
        if (!adminSpawn) {
            var ownership = com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
            if (encounter != null && encounter.arenaId() != null
                    && !ownership.canonicalArenaId(encounter.arenaId()).equals(ownership.canonicalArenaId(area.id().toString()))) return;
            UUID known = ownership.liveBoss(area.id().toString()).orElse(null);
            if (known != null && !known.equals(boss.getUUID())) {
                // Saved duplicates stay intact and dormant for explicit diagnosis/quarantine.
                if (!event.loadedFromDisk()) { event.setCanceled(true); return; }
                HostedEncounterApi.hold(boss);
                com.mojang.logging.LogUtils.getLogger().warn("Saved boss identity conflict retained for /jem boss-audit: arena={}, keeper={}, duplicate={}", area.id(), known, boss.getUUID());
                return;
            }
            ownership.recordLiveBoss(area.id().toString(), boss);
        }
        if (!adminSpawn && (encounter == null || encounter.provenance() != com.siirio.jemworldbosstiers.encounter.EncounterProvenance.RAID_EVENT))
            RaidArenaApi.observeNativeBoss(level.getServer(), area.id().toString(), boss);
        if (encounter != null && encounter.provenance() == com.siirio.jemworldbosstiers.encounter.EncounterProvenance.RAID_EVENT) return;
        com.siirio.jemworldbosstiers.encounter.HostedGroup.stage(boss);
        HostedEncounterApi.hold(boss);
        if (encounter == null) return;
        PartyState row = party(level, area.id());
        if (row == null || !row.state().equals("ACTIVE")) return;
        level.getServer().execute(() -> {
            var store = SmpData.get(level.getServer());
            store.edit(() -> {
                var current = store.parties().current(row.id());
                if (boss.isAlive() && !boss.isRemoved() && current != null && current.state().equals("ACTIVE"))
                    bind(level, boss, current, SmpRecords.memberIds(current).stream().filter(id -> Parties.accepted(current, id)).toList());
            });
        });
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !SmpEnvironment.active(player) || !player.isAlive() || player.isSpectator()) return;
        if (player.tickCount % CHECK_INTERVAL != 0) return;
        finishPendingRaidExit(player);
        Area area = discover(player.serverLevel(), player.blockPosition());
        Area close = nearby(player.serverLevel(), player.position());
        if (close == null) NEAR.remove(player.getUUID());
        else if (!close.id().equals(NEAR.put(player.getUUID(), close.id()))) {
            PartyState closeParty = party(player.serverLevel(), close.id());
            if (closeParty == null || !HostedBoundary.authorized(player, closeParty)) {
                if (physicallyLocked(player.serverLevel(), close) || locked(player, close))
                    com.siirio.jemtwelveeyes.api.CampaignApi.explainLocked(player, close.boss());
                else player.sendSystemMessage(Component.literal("▣ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(close.boss()).getDescriptionId())).append(" ")
                        .append(Component.translatable("jem.smp.open_fight").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                                .withClickEvent(new EventPromptRequest(EventPromptRequest.Kind.ARENA, close.id()).clickEvent()))));
            }
        }
        AREAS.values().stream()
                .filter(candidate -> candidate.dimension().equals(player.level().dimension().location().toString()) && candidate.distance(player.position()) <= LANDMARK_RADIUS)
                .forEach(candidate -> Landmarks.publishBoss(player.server, candidate.id(), candidate.boss(), player.level().dimension().location(), candidate.center(), candidate.bounds()));
        if (area == null) { INSIDE.remove(player.getUUID()); return; }
        PartyState activeParty = party(player.serverLevel(), area.id());
        if (activeParty != null) HostedBoundary.observe(player, activeParty, area.shell());
        UUID before = INSIDE.put(player.getUUID(), area.id());
        if (!area.id().equals(before)) {
            PartyState row = party(player.serverLevel(), area.id());
            if (row != null) {
                if (row.state().equals("ACTIVE") && row.nativePending())
                    bindExisting(player.serverLevel(), area, row, SmpRecords.memberIds(row).stream().filter(id -> Parties.accepted(row, id)).toList());
            }
        }
    }

    private static boolean locked(ServerPlayer player, Area area) {
        return ModList.get().isLoaded("jem_twelve_eyes") && !com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(player, area.boss());
    }



    public static String startReason(ServerPlayer host, PartyState row, Collection<UUID> participants) {
        if (!row.hasStructureId()) return "";
        Area area = AREAS.get(row.structureId());
        if (area == null) return "unavailable";
        try { requireParticipants(host, row, participants, area); }
        catch (IllegalArgumentException invalid) { return invalid.getMessage(); }
        return blockers(host.serverLevel(), area, new HashSet<>(participants)).isEmpty() ? "" : "arena_occupied";
    }

    private static void requireParticipants(ServerPlayer host, PartyState row, Collection<UUID> participants, Area area) {
        SmpRecords.require(!participants.isEmpty() && participants.contains(host.getUUID()), "host_must_be_present");
        for (UUID id : participants) {
            ServerPlayer player = host.server.getPlayerList().getPlayer(id);
            SmpRecords.require(player != null && player.isAlive() && !player.isSpectator()
                    && Parties.accepted(row, id) && row.member(id) != null && !row.member(id).eliminated(), "unavailable");
            SmpRecords.require(!locked(player, area), "unavailable");
        }
    }

    public static String nativeBossStartReason(ServerPlayer host, PartyState row) {
        if (!row.hasStructureId()) return "";
        Area area=AREAS.get(row.structureId());
        if(area==null) return "unavailable";
        return livingBoss(host.serverLevel(),area)==null?"boss_not_respawned":"";
    }

    private static LivingEntity livingBoss(ServerLevel level, Area area) {
        if (!area.dimension().equals(level.dimension().location().toString())) return null;
        var data = com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
        var known = data.liveBoss(area.id().toString()).orElse(null);
        if (known != null) {
            var entity = level.getEntity(known);
            return entity instanceof LivingEntity boss && canonicalBoss(data, area, boss) ? boss : null;
        }
        var bounds = area.bounds();
        return level.getEntitiesOfClass(LivingEntity.class,
                new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1),
                boss -> canonicalBoss(data, area, boss)).stream()
                .min(Comparator.comparing(LivingEntity::getStringUUID)).orElse(null);
    }

    private static boolean canonicalBoss(com.siirio.jemworldbosstiers.progression.WorldTierData data, Area area, LivingEntity boss) {
        var encounter = WorldTierApi.encounter(boss).orElse(null);
        return boss.isAlive() && !boss.isRemoved() && BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).equals(area.boss())
                && encounter != null && encounter.arenaId() != null
                && data.canonicalArenaId(encounter.arenaId()).equals(data.canonicalArenaId(area.id().toString()))
                && !boss.getPersistentData().getString("forge:spawn_type").equals("SPAWN_EGG")
                && !boss.getPersistentData().getString("forge:spawn_type").equals("COMMAND");
    }

    private static Area nearby(ServerLevel level, Vec3 position) {
        discover(level, BlockPos.containing(position));
        return AREAS.values().stream().filter(area -> area.near(level, position) && guarded(level, area)).findFirst().orElse(null);
    }

    private static boolean guarded(ServerLevel level, Area area) {
        if (party(level, area.id()) != null || physicallyLocked(level, area)) return true;
        // An unloaded canonical identity is unresolved, never proof that the boss is dead.
        var ownership=com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
        if (ownership.liveBoss(area.id().toString()).isPresent()
                || ownership.arena(area.id().toString()).map(arena->arena.activeEncounterId()!=null).orElse(false)) return true;
        Presence presence = PRESENCE.get(area.id());
        long tick = level.getGameTime();
        if (presence == null || presence.tick() != tick) {
            if (PRESENCE.size() >= AREA_LIMIT) PRESENCE.clear();
            presence = new Presence(tick, livingBoss(level, area) != null);
            PRESENCE.put(area.id(), presence);
        }
        return presence.livingBoss();
    }

    private record Presence(long tick, boolean livingBoss) {}

    public static List<String> occupants(ServerPlayer player, UUID id) {
        Area area = AREAS.get(id);
        if (area == null) return List.of();
        return blockers(player.serverLevel(), area, Set.of(player.getUUID())).stream()
                .map(value -> value.getGameProfile().getName()).toList();
    }

    public static String arenaState(ServerPlayer player, UUID id) {
        Area area = AREAS.get(id);
        if (area == null) return "UNAVAILABLE";
        PartyState row = party(player.serverLevel(), id);
        if (row != null) {
            boolean raid = row.raid();
            return row.state().equals("ACTIVE") ? (raid ? "RAID_ACTIVE" : "BOSS_FIGHT_ACTIVE") : "STARTING";
        }
        return occupants(player, id).isEmpty() ? "FREE" : "OCCUPIED";
    }

    private static List<ServerPlayer> blockers(ServerLevel level, Area area, Set<UUID> participants) {
        return level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator() && !player.hasPermissions(2))
                .filter(player -> !participants.contains(player.getUUID()) && SolidBoundary.movementBox(player).intersects(
                        new AABB(area.bounds().minX(), area.bounds().minY(), area.bounds().minZ(), area.bounds().maxX()+1D, area.bounds().maxY()+1D, area.bounds().maxZ()+1D)))
                .toList();
    }

    public static List<EventBoundary> boundaries(ServerPlayer player) {
        List<EventBoundary> result = new ArrayList<>();
        if (!SmpEnvironment.active(player)) return result;
        for (Area area : AREAS.values()) {
            if (!area.dimension().equals(player.level().dimension().location().toString())) continue;
            var bounds = area.shell();
            double distance = area.distance(player.position());
            if (distance > EventRules.BOUNDARY_DISTANCE.get() || !guarded(player.serverLevel(), area)) continue;
            PartyState row = party(player.serverLevel(), area.id());
            boolean passable = row != null && HostedBoundary.passable(player, row);
            result.add(SolidBoundary.boundary(area.id(), area.dimension(), bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ(),
                    bounds.minY(), bounds.maxY(), EventRules.RAID_COLOR.get(), passable));
            if (result.size() >= BOUNDARY_LIMIT) break;
        }
        return result;
    }

    public static List<EventBoundary> solidBoundaries(net.minecraft.world.entity.Entity entity) {
        return solidBoundaries(entity, null);
    }

    static List<EventBoundary> solidBoundaries(net.minecraft.world.entity.Entity entity, AABB query) {
        if (!(entity.level() instanceof ServerLevel level) || !SmpEnvironment.active(level.getServer()) || entity.isSpectator()) return List.of();
        refreshAreas(level);
        return solidBoundaries(entity, level, query, true);
    }

    static List<EventBoundary> destinationBoundaries(net.minecraft.world.entity.Entity entity, ServerLevel level, Vec3 destination) {
        if (!SmpEnvironment.active(level.getServer()) || entity.isSpectator()) return List.of();
        refreshAreas(level);
        return solidBoundaries(entity, level, SolidBoundary.destinationBox(entity, destination), false);
    }

    /** Only the exact held canonical boss may be restored at its verified native anchor. */
    static boolean canonicalPlacement(net.minecraft.world.entity.Entity entity,ServerLevel level,Vec3 destination) {
        if(!(entity instanceof LivingEntity boss) || !HostedEncounterApi.held(boss) || entity.level()!=level) return false;
        var encounter=WorldTierApi.encounter(boss).orElse(null);
        if(encounter==null || encounter.arenaId()==null) return false;
        var data=com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
        var arena=data.arena(encounter.arenaId()).orElse(null);
        if(arena==null || !arena.structureArena() || !data.nativeAnchorVerified(arena.id())
                || data.liveBoss(arena.id()).filter(boss.getUUID()::equals).isEmpty()
                || Vec3.atBottomCenterOf(arena.respawnPosition()).distanceToSqr(destination)>1.0E-6) return false;
        var shell=arenaShell(arena.bounds());
        var boundary=SolidBoundary.boundary(boss.getUUID(),arena.dimension(),shell.minX(),shell.minZ(),shell.maxX(),shell.maxZ(),shell.minY(),shell.maxY(),0,false);
        var box=SolidBoundary.destinationBox(entity,destination);
        return SolidBoundary.contains(box,boundary)&&!SolidBoundary.intersectsWall(box,boundary)&&level.noCollision(entity,box);
    }

    private static List<EventBoundary> solidBoundaries(net.minecraft.world.entity.Entity entity, ServerLevel level, AABB query, boolean admission) {
        List<EventBoundary> result = new ArrayList<>();
        for (Area area : AREAS.values()) {
            if (!area.dimension().equals(level.dimension().location().toString())) continue;
            BoundingBox bounds = area.shell();
            if (query != null && (query.maxX < bounds.minX() - SolidBoundary.WALL_THICKNESS || query.minX > bounds.maxX() + 1D + SolidBoundary.WALL_THICKNESS
                    || query.maxZ < bounds.minZ() - SolidBoundary.WALL_THICKNESS || query.minZ > bounds.maxZ() + 1D + SolidBoundary.WALL_THICKNESS
                    || query.maxY < bounds.minY() || query.minY > bounds.maxY() + 1D)) continue;
            if (!guarded(level, area)) continue;
            List<ServerPlayer> players = SolidBoundary.players(entity);
            PartyState row = party(level, area.id());
            if (admission && row != null) players.forEach(player -> HostedBoundary.observe(player, row, bounds));
            boolean passable = admission && row != null && !players.isEmpty() && players.stream().allMatch(player -> HostedBoundary.passable(player, row));
            result.add(SolidBoundary.boundary(area.id(), area.dimension(), bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ(),
                    bounds.minY(), bounds.maxY(), EventRules.RAID_COLOR.get(), passable));
        }
        return result;
    }

    static BossTeleportSafety.Decision safeTeleport(ServerPlayer player, ServerLevel level, Vec3 requested) {
        if (!SmpEnvironment.active(player)) return BossTeleportSafety.Decision.unmatched();
        discover(level, BlockPos.containing(requested));
        AABB source = SolidBoundary.movementBox(player);
        AABB destination = SolidBoundary.destinationBox(player, requested);
        for (Area area : AREAS.values()) {
            ServerLevel areaLevel = level(player.server, area);
            if (areaLevel == null) continue;
            BoundingBox shell = area.shell();
            EventBoundary boundary = SolidBoundary.boundary(area.id(), area.dimension(), shell.minX(), shell.minZ(), shell.maxX(), shell.maxZ(),
                    shell.minY(), shell.maxY(), EventRules.RAID_COLOR.get(), false);
            boolean sourceInside = player.serverLevel() == areaLevel && SolidBoundary.contains(source, boundary);
            boolean destinationInside = level == areaLevel && SolidBoundary.contains(destination, boundary);
            boolean destinationWall = level == areaLevel && SolidBoundary.intersectsWall(destination, boundary);
            PartyState row = party(areaLevel, area.id());
            if (!sourceInside && !destinationInside && !destinationWall && (row == null || !HostedBoundary.locked(player, row))) continue;
            if (!guarded(areaLevel, area)) continue;
            if (row != null && HostedBoundary.locked(player, row)) {
                if (level != areaLevel) return BossTeleportSafety.Decision.redirect(null);
                if (destinationInside && !destinationWall && level.noCollision(player, destination)) continue;
                if (sourceInside && !SolidBoundary.intersectsWall(source, boundary) && areaLevel.noCollision(player, source))
                    return BossTeleportSafety.Decision.redirect(player.position());
                BlockPos respawn = RaidArenaApi.respawnPosition(player.server, area.id().toString(), area.boss()).orElse(null);
                Vec3 recovery = respawn == null ? null : Vec3.atBottomCenterOf(respawn);
                AABB recoveryBox = recovery == null ? null : SolidBoundary.destinationBox(player, recovery);
                return BossTeleportSafety.Decision.redirect(recoveryBox != null && SolidBoundary.contains(recoveryBox, boundary)
                        && !SolidBoundary.intersectsWall(recoveryBox, boundary) && areaLevel.noCollision(player, recoveryBox) ? recovery : null);
            }
            if (!destinationInside && !destinationWall) continue;
            if (!destinationWall && row != null && HostedBoundary.authorized(player, row) && level.noCollision(player, destination)) continue;
            BlockPos safe = safeExit(player, areaLevel, area, BlockPos.containing(requested));
            return BossTeleportSafety.Decision.redirect(safe == null ? null : Vec3.atBottomCenterOf(safe));
        }
        return BossTeleportSafety.Decision.unmatched();
    }

    private static boolean physicallyLocked(ServerLevel level, Area area) {
        return ModList.get().isLoaded("jem_twelve_eyes")&&com.siirio.jemtwelveeyes.api.CampaignApi.locked(level.getServer(),area.boss());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && com.siirio.jemworldbosstiers.api.StructureEncounterApi.activationBlock(BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock()))
                && !allowNativeSpawn(level, event.getPos())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("jem.event.start_before_ritual"), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel() instanceof ServerLevel level && com.siirio.jemworldbosstiers.api.StructureEncounterApi.activationItem(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))
                && !allowNativeSpawn(level, event.getEntity().blockPosition())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateBoss(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof LivingEntity boss && managed(boss) && !allowNative(boss.level(), boss.blockPosition())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateBossPart(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof LivingEntity boss && managed(boss) && !allowNative(boss.level(), boss.blockPosition())) event.setCanceled(true);
    }







    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chunkLoaded(net.minecraftforge.event.level.ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        REGISTERED_AT.remove(level.dimension().location().toString());
        NATIVE_LOOKUPS.clear();
        ChunkPos chunk = event.getChunk().getPos();
        BlockPos center=chunk.getMiddleBlockPosition(level.getSeaLevel());
        discover(level,center);
        if (event.getChunk() instanceof LevelChunk loaded) for (BlockPos position : loaded.getBlockEntities().keySet()) {
            Area area = AREAS.values().stream().filter(candidate -> candidate.contains(level, position)).findFirst().orElse(null);
            if (area != null) RaidArenaApi.migrateNativeAnchor(level, area.id().toString(), area.boss(), position);
        }
        if (event.getChunk() instanceof LevelChunk loaded) {
            var sections = loaded.getSections();
            for (int index = 0; index < sections.length; index++) {
                var section = sections[index];
                if (section.hasOnlyAir() || !section.maybeHas(StructureStaging::retiredBarrier)) continue;
                int baseY = loaded.getMinBuildHeight() + index * 16;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    if (retiredBarrier(section.getBlockState(x, y, z)))
                        level.removeBlock(new BlockPos(chunk.getMinBlockX() + x, baseY + y, chunk.getMinBlockZ() + z), false);
                }
            }
        }
    }

    private static boolean retiredBarrier(net.minecraft.world.level.block.state.BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.equals(new ResourceLocation("jem_server", "structure_barrier"))
                || id.equals(new ResourceLocation("jem_twelve_eyes", "locked_boss_barrier"));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finishPendingRaidExit(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { UUID id = event.getEntity().getUUID(); INSIDE.remove(id); NEAR.remove(id); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { AREAS.clear(); INSIDE.clear(); NEAR.clear(); PARTY_CACHE.clear(); PRESENCE.clear(); NATIVE_LOOKUPS.clear(); REGISTERED_AT.clear(); }

    static BoundingBox arenaShell(BoundingBox bounds) {
        return new BoundingBox(bounds.minX()-1,bounds.minY(),bounds.minZ()-1,bounds.maxX()+1,bounds.maxY(),bounds.maxZ()+1);
    }

    private record Area(UUID id, String dimension, ResourceLocation boss, BoundingBox bounds) {
        boolean contains(ServerLevel level, BlockPos pos) {
            return dimension.equals(level.dimension().location().toString()) && bounds.isInside(pos);
        }

        boolean contains(Vec3 pos) {
            return pos.x >= bounds.minX() && pos.x < bounds.maxX() + 1 && pos.y >= bounds.minY() && pos.y < bounds.maxY() + 1
                    && pos.z >= bounds.minZ() && pos.z < bounds.maxZ() + 1;
        }

        boolean containsColumn(Vec3 pos) {
            return pos.x>=bounds.minX()&&pos.x<bounds.maxX()+1&&pos.z>=bounds.minZ()&&pos.z<bounds.maxZ()+1;
        }

        boolean near(ServerLevel level, Vec3 pos) {
            return dimension.equals(level.dimension().location().toString()) && distance(pos) <= 3;
        }

        double distance(Vec3 pos) {
            BoundingBox shell = shell();
            double x = Math.max(Math.max(shell.minX() - pos.x, 0), pos.x - shell.maxX() - 1);
            double y = Math.max(Math.max(shell.minY() - pos.y, 0), pos.y - shell.maxY() - 1);
            double z = Math.max(Math.max(shell.minZ() - pos.z, 0), pos.z - shell.maxZ() - 1);
            return Math.max(x, Math.max(y, z));
        }

        BlockPos center() {
            return new BlockPos(bounds.minX() + bounds.getXSpan() / 2, bounds.minY() + bounds.getYSpan() / 2,
                    bounds.minZ() + bounds.getZSpan() / 2);
        }

        BoundingBox shell() {
            return arenaShell(bounds);
        }

    }
}
