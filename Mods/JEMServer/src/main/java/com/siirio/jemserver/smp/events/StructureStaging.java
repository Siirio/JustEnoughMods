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
import net.minecraft.nbt.CompoundTag;
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

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class StructureStaging {
    public record RaidTarget(UUID id, ResourceLocation boss, String dimension, BlockPos center, BoundingBox bounds) {}
    private static final int AREA_LIMIT = 1024;
    private static final int CHECK_INTERVAL = 10;
    private static final int BOUNDARY_LIMIT = 16;
    private static final int LANDMARK_RADIUS = 10;
    private static final int EXIT_SEARCH_MARGIN = 6;
    private static final String RAID_EXIT_PENDING = "raidArenaExitPending";
    private static final Map<ResourceLocation, ResourceLocation> STRUCTURES = new HashMap<>();
    private static final Set<ResourceLocation> ACTIVATION_BLOCKS = new HashSet<>();
    private static final Set<ResourceLocation> ACTIVATION_ITEMS = new HashSet<>();
    private static final LinkedHashMap<UUID, Area> AREAS = new LinkedHashMap<>();
    private static final Map<UUID, UUID> INSIDE = new HashMap<>();
    private static final Map<UUID, UUID> NEAR = new HashMap<>();
    private static final Map<UUID, Optional<CompoundTag>> PARTY_CACHE = new HashMap<>();
    private static final Map<UUID, Presence> PRESENCE = new HashMap<>();
    private static final LinkedHashMap<String, Optional<Area>> NATIVE_LOOKUPS = new LinkedHashMap<>();

    private StructureStaging() {}

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(), "jem/events/structures") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
                STRUCTURES.clear(); ACTIVATION_BLOCKS.clear(); ACTIVATION_ITEMS.clear(); AREAS.clear(); INSIDE.clear(); NEAR.clear(); PARTY_CACHE.clear(); PRESENCE.clear(); NATIVE_LOOKUPS.clear();
                for (JsonElement file : files.values()) {
                    var root = file.getAsJsonObject();
                    for (JsonElement value : root.getAsJsonArray("structures")) {
                        var entry = value.getAsJsonObject();
                        STRUCTURES.put(new ResourceLocation(entry.get("structure").getAsString()), new ResourceLocation(entry.get("boss").getAsString()));
                    }
                    for (JsonElement value : root.getAsJsonArray("activation_blocks")) ACTIVATION_BLOCKS.add(new ResourceLocation(value.getAsString()));
                    for (JsonElement value : root.getAsJsonArray("activation_items")) ACTIVATION_ITEMS.add(new ResourceLocation(value.getAsString()));
                }
            }
        });
    }

    private static Area discover(ServerLevel level, BlockPos position) {
        for (Area area : AREAS.values()) if (area.contains(level, position)) {
            return area;
        }
        var chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        if (chunk == null) return null;
        List<StructureStart> starts = new ArrayList<>(chunk.getAllStarts().values());
        for (var reference : chunk.getAllReferences().entrySet()) for (long value : reference.getValue()) {
            ChunkPos origin = new ChunkPos(value);
            var source = level.getChunkSource().getChunkNow(origin.x, origin.z);
            if (source != null) {
                var start = source.getStartForStructure(reference.getKey());
                if (start != null) starts.add(start);
            }
        }
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (var start : starts) {
            var boss = STRUCTURES.get(registry.getKey(start.getStructure()));
            if (!start.isValid() || boss == null) continue;
            BoundingBox bounds = start.getBoundingBox();
            String dimension = level.dimension().location().toString();
            String key = dimension + ":" + registry.getKey(start.getStructure()) + ":" + bounds.minX() + ":" + bounds.minZ();
            UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
            var area = new Area(id, dimension, boss, bounds);
            RaidArenaApi.register(level.getServer(), id.toString(), boss, level.dimension().location(), bounds);
            RaidArenaApi.migrateNativeAnchor(level.getServer(), id.toString(), boss, start);
            if (!AREAS.containsKey(id) && AREAS.size() >= AREA_LIMIT) AREAS.remove(AREAS.keySet().iterator().next());
            AREAS.put(id, area);
            if (area.contains(level, position)) return area;
        }
        return null;
    }

    private static CompoundTag party(ServerLevel level, UUID id) {
        var cached = PARTY_CACHE.get(id);
        if (cached != null && (cached.isEmpty() || !SmpData.closed(cached.get()))) return cached.orElse(null);
        var row = SmpData.get(level.getServer()).all("parties").stream()
                .filter(value -> value.hasUUID("structureId") && value.getUUID("structureId").equals(id) && !SmpData.closed(value))
                .findFirst();
        if (PARTY_CACHE.size() >= AREA_LIMIT) PARTY_CACHE.clear();
        PARTY_CACHE.put(id, row);
        return row.orElse(null);
    }

    public static boolean allowNative(Level level, BlockPos position) {
        return true;
    }

    public static boolean allowNativeSpawn(Level level, BlockPos position) {
        if (!(level instanceof ServerLevel serverLevel) || !allowNative(level, position)) return !(level instanceof ServerLevel);
        Area area = discover(serverLevel, position);
        return area == null || livingBoss(serverLevel, area) == null;
    }

    public static boolean managed(LivingEntity boss) {
        if (!(boss.level() instanceof ServerLevel level)) return false;
        return bossArea(level,boss)!=null;
    }

    public static boolean prompt(ServerPlayer player,LivingEntity boss) {
        Area area=bossArea(player.serverLevel(),boss);
        if(area==null) return false;
        prompt(player,area.id());
        return true;
    }

    private static Area bossArea(ServerLevel level,LivingEntity boss) {
        discover(level,boss.blockPosition());
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
        return SmpData.get(server).all("parties").stream().noneMatch(row->row.hasUUID("structureId")
                &&row.getUUID("structureId").toString().equals(id)&&!SmpData.closed(row));
    }

    public static boolean raidAvailable(ServerPlayer player, UUID id) {
        Area area = AREAS.get(id);
        CompoundTag event = EventScheduler.active(player.server, "BOSS_RAID");
        if (area == null || !area.near(player.serverLevel(), player.position()) || event == null
                || event.getBoolean("combatStarted") || party(player.serverLevel(), id) != null) return false;
        return event.contains("raidArenaId") ? event.getString("raidArenaId").equals(id.toString())
                : RaidArenaApi.compatible(player.server, id.toString(), area.boss());
    }

    public static RaidTarget availableRaid(ServerLevel level,BlockPos position) {
        Area area=nearby(level,Vec3.atCenterOf(position));
        if(area==null||party(level,area.id())!=null||!RaidArenaApi.compatible(level.getServer(),area.id().toString(),area.boss())) return null;
        BlockPos center=RaidArenaApi.respawnPosition(level.getServer(),area.id().toString(),area.boss()).orElse(area.center());
        return new RaidTarget(area.id(),area.boss(),area.dimension(),center,area.bounds());
    }

    public static void choose(ServerPlayer player, UUID id, String mode, boolean solo) {
        Area area = AREAS.get(id);
        SmpRecords.require(area != null && area.near(player.serverLevel(), player.position()), "travel_to_arena");
        SmpRecords.require(!locked(player, area), "unavailable");
        SmpRecords.require(party(player.serverLevel(), id) == null, "arena_busy");
        SmpRecords.require(blockers(player.serverLevel(), area, Set.of(player.getUUID())).isEmpty(), "arena_occupied");
        if (mode.equals("BOSS_RAID")) {
            CompoundTag event = EventScheduler.active(player.server, "BOSS_RAID");
            SmpRecords.require(event != null && raidAvailable(player, id), "raid_unavailable");
            SmpRecords.require(RaidEvent.claimArena(player.server, event, id.toString(), area.boss(), area.dimension(), area.center(), area.bounds()), "arena_busy");
            PARTY_CACHE.remove(id);
            EventParties.join(player, event, solo);
            PARTY_CACHE.put(id, SmpData.get(player.server).all("parties").stream()
                    .filter(value -> value.hasUUID("structureId") && value.getUUID("structureId").equals(id) && !SmpData.closed(value)).findFirst());
            return;
        }
        SmpRecords.require(mode.equals("BOSS_FIGHT"), "unavailable");
        CompoundTag activeRaid = EventScheduler.active(player.server, "BOSS_RAID");
        SmpRecords.require(activeRaid == null || !activeRaid.getString("raidArenaId").equals(id.toString()), "raid_unavailable");
        SmpRecords.require(livingBoss(player.serverLevel(), area) != null, "boss_not_respawned");
        CompoundTag row = party(player.serverLevel(), id);
        if (row == null) {
            row = Parties.createHosted(player, SmpPartyRequest.hosted(
                    Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(area.boss()).getDescriptionId()).getString(), solo));
            attachArena(row,id,area.boss(),area.dimension(),area.center(),area.bounds());
            row.putBoolean("nativePending", true);
            PARTY_CACHE.put(id, Optional.of(row));
            SmpData.get(player.server).changed(row);
        }
        if (solo) {
            Parties.makeSolo(player, row);
            HostedParties.start(player, row);
        } else SmpNetwork.open(player, "parties", row.getUUID("id"));
    }

    public static boolean start(ServerPlayer host, CompoundTag row, List<UUID> ids) {
        if (!row.getBoolean("nativePending")) return false;
        Area area = row.hasUUID("structureId") ? AREAS.get(row.getUUID("structureId")) : null;
        SmpRecords.require(area != null, "unavailable");
        SmpRecords.require(!locked(host, area), "unavailable");
        SmpRecords.require(livingBoss(host.serverLevel(), area) != null, "boss_not_respawned");
        SmpRecords.require(blockers(host.serverLevel(), area, new HashSet<>(ids)).isEmpty(), "arena_occupied");
        row.putString("state", "ACTIVE");
        SmpData.get(host.server).changed(row);
        bindExisting(host.serverLevel(), area, row, ids);
        host.sendSystemMessage(Component.translatable("jem.event.native_ritual_ready"));
        return true;
    }

    public static void prompt(ServerPlayer player, UUID id) {
        Area area = AREAS.get(id);
        SmpRecords.require(area != null && area.near(player.serverLevel(), player.position()), "travel_to_arena");
        SmpRecords.require(!physicallyLocked(player.serverLevel(), area) && !locked(player, area), "unavailable");
        EventNetwork.prompt(player, area.id(), EventEntryKind.BOSS_STRUCTURE, Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(area.boss()).getDescriptionId()).getString(), area.boss().toString());
    }

    public static boolean arrive(ServerPlayer player, CompoundTag row) {
        if (!row.hasUUID("structureId")) return false;
        Area area = AREAS.get(row.getUUID("structureId"));
        SmpRecords.require(area != null, "unavailable");
        ServerLevel level = level(player.server, area);
        SmpRecords.require(level != null, "unavailable");
        BlockPos outside=safeExit(player,level,area);
        if(outside==null) throw new IllegalArgumentException("no_safe_arrival");
        teleport(player,level,outside);
        return true;
    }

    public static void finish(net.minecraft.server.MinecraftServer server, CompoundTag row) {
        HostedBoundary.clearParty(row);
        if (row.getBoolean("raid")) finishRaidParticipants(server, row);
        else PartyTeleportFlow.finish(server,row);
        if (!row.hasUUID("structureId")) {
            return;
        }
        Area area = AREAS.get(row.getUUID("structureId"));
        if (area == null) return;
        ServerLevel level = level(server, area);
        if (level == null) return;
        PARTY_CACHE.remove(area.id());
    }

    private static void finishRaidParticipants(net.minecraft.server.MinecraftServer server, CompoundTag party) {
        for (UUID id : SmpRecords.memberIds(party)) {
            if (!Parties.accepted(party, id)) continue;
            CompoundTag member = SmpRecords.members(party).getCompound(id.toString());
            member.putBoolean(RAID_EXIT_PENDING, true);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) finishRaidExit(player, party, member);
        }
        clearRaidExitSession(party);
        SmpData.get(server).changed(party);
    }

    private static boolean finishRaidExit(ServerPlayer player, CompoundTag party, CompoundTag member) {
        member.remove(RAID_EXIT_PENDING);
        PartyTeleportFlow.clearMember(party, player.getUUID());
        return true;
    }

    private static void finishPendingRaidExit(ServerPlayer player) {
        SmpData data = SmpData.get(player.server);
        for (CompoundTag party : data.all("parties")) {
            CompoundTag member = SmpRecords.members(party).getCompound(player.getStringUUID());
            if (!member.getBoolean(RAID_EXIT_PENDING)) continue;
            if (finishRaidExit(player, party, member)) {
                clearRaidExitSession(party);
                data.changed(party);
            }
        }
    }

    private static void clearRaidExitSession(CompoundTag party) {
        if (!hasPendingRaidExit(party)) PartyTeleportFlow.clearSession(party);
    }

    public static boolean hasPendingRaidExit(CompoundTag party) {
        return SmpRecords.memberIds(party).stream().anyMatch(id ->
                SmpRecords.members(party).getCompound(id.toString()).getBoolean(RAID_EXIT_PENDING));
    }

    public static void attachArena(CompoundTag record,UUID id,ResourceLocation boss,String dimension,BlockPos center,BoundingBox bounds) {
        record.putUUID("structureId",id);
        record.putString("structureBoss",boss.toString());
        record.putIntArray("structureBounds",new int[]{bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()});
        EventRegions.locate(record,dimension,center,bounds.minX(),bounds.minZ(),bounds.maxX(),bounds.maxZ());
        record.putInt("minY",bounds.minY());
        record.putInt("maxY",bounds.maxY());
    }

    public static BoundingBox prepareCombat(ServerLevel level,CompoundTag record,LivingEntity boss) {
        Area area=record.hasUUID("structureId")?AREAS.get(record.getUUID("structureId")):null;
        int[] saved=record.getIntArray("structureBounds");
        BoundingBox bounds=area!=null?area.bounds():saved.length==6
                ?new BoundingBox(saved[0],saved[1],saved[2],saved[3],saved[4],saved[5])
                :BossStaging.bounds(boss);
        if(area==null) {
            EventRegions.locate(record,level,boss.blockPosition(),bounds.minX(),bounds.minZ(),bounds.maxX(),bounds.maxZ());
            record.putInt("minY",bounds.minY());
            record.putInt("maxY",bounds.maxY());
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
        return level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(position.getX() + .5, position.getY(), position.getZ() + .5));
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
        return level.noCollision(mob,mob.getDimensions(mob.getPose()).makeBoundingBox(position.getX()+.5,position.getY(),position.getZ()+.5));
    }

    private static BlockPos safeExit(ServerPlayer player, ServerLevel level, Area area) {
        return safeExit(player,level,area,player.blockPosition());
    }

    private static BlockPos safeExit(ServerPlayer player,ServerLevel level,Area area,BlockPos origin) {
        return safeOutside(player,level,area.shell(),origin);
    }

    private static BlockPos safeExit(Mob mob,ServerLevel level,Area area) {
        return safeExit(level,area.shell(),mob.blockPosition(),position->safe(mob,level,position));
    }

    static BlockPos safeOutside(ServerPlayer player,ServerLevel level,BoundingBox bounds,BlockPos origin) {
        return safeExit(level,bounds,origin,position->safeOutside(player,level,position));
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
        player.teleportTo(level, position.getX() + .5, position.getY(), position.getZ() + .5, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
    }

    private static void bindExisting(ServerLevel level, Area area, CompoundTag row, List<UUID> ids) {
        LivingEntity boss = livingBoss(level, area);
        if (boss != null) bind(level, boss, row, ids);
    }

    private static void bind(ServerLevel level, LivingEntity boss, CompoundTag row, List<UUID> ids) {
        if (row.hasUUID("bossEntity") || !row.getBoolean("nativePending") || HostedEncounterApi.isRaid(boss)) return;
        Area area = bossArea(level,boss);
        if (area == null || livingBoss(level, area) != boss) return;
        boss.getPersistentData().putBoolean("jem_solo", row.getBoolean("solo"));
        if (HostedEncounterApi.start(boss, ids, false, prepareCombat(level,row,boss))) {
            row.putUUID("bossEntity", boss.getUUID());
            row.putBoolean("nativePending", false);
            Profiles.count(level.getServer(), row.getUUID("owner"), "bossesHosted");
            SmpData.get(level.getServer()).changed(row);
        }
    }

    public static void stageBoss(ServerLevel level,CompoundTag record,LivingEntity boss) {
        if(!boss.isAlive()||boss.isRemoved()) return;
        HostedEncounterApi.hold(boss);
    }



    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void spawned(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity boss)
                || WorldTierApi.profile(boss).isEmpty() || boss.getPersistentData().getBoolean(EventSession.SPAWNED)) return;
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
        CompoundTag row = party(level, area.id());
        if (row == null || !row.getString("state").equals("ACTIVE")) return;
        level.getServer().execute(() -> {
            if (boss.isAlive() && !boss.isRemoved() && row.getString("state").equals("ACTIVE"))
                bind(level, boss, row, SmpRecords.memberIds(row).stream().filter(id -> Parties.accepted(row, id)).toList());
        });
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator()) return;
        if (player.tickCount % CHECK_INTERVAL != 0) return;
        finishPendingRaidExit(player);
        Area area = discover(player.serverLevel(), player.blockPosition());
        Area close = nearby(player.serverLevel(), player.position());
        if (close == null) NEAR.remove(player.getUUID());
        else if (!close.id().equals(NEAR.put(player.getUUID(), close.id()))) {
            CompoundTag closeParty = party(player.serverLevel(), close.id());
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
        CompoundTag activeParty = party(player.serverLevel(), area.id());
        UUID before = INSIDE.put(player.getUUID(), area.id());
        if (!area.id().equals(before)) {
            CompoundTag row = party(player.serverLevel(), area.id());
            if (row != null) {
                if (row.getString("state").equals("ACTIVE") && row.getBoolean("nativePending"))
                    bindExisting(player.serverLevel(), area, row, SmpRecords.memberIds(row).stream().filter(id -> Parties.accepted(row, id)).toList());
            }
        }
    }

    private static boolean locked(ServerPlayer player, Area area) {
        return ModList.get().isLoaded("jem_twelve_eyes") && !com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(player, area.boss());
    }



    public static String startReason(ServerPlayer host, CompoundTag row, Collection<UUID> participants) {
        if (!row.hasUUID("structureId")) return "";
        Area area = AREAS.get(row.getUUID("structureId"));
        if (area == null) return "unavailable";
        return blockers(host.serverLevel(), area, new HashSet<>(participants)).isEmpty() ? "" : "arena_occupied";
    }

    public static String nativeBossStartReason(ServerPlayer host, CompoundTag row) {
        if (!row.hasUUID("structureId")) return "";
        Area area=AREAS.get(row.getUUID("structureId"));
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
        if (party(level, area.id()) != null) return true;
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
        CompoundTag row = party(player.serverLevel(), id);
        if (row != null) {
            boolean raid = row.getBoolean("raid");
            return row.getString("state").equals("ACTIVE") ? (raid ? "RAID_ACTIVE" : "BOSS_FIGHT_ACTIVE") : "STARTING";
        }
        return occupants(player, id).isEmpty() ? "FREE" : "OCCUPIED";
    }

    private static List<ServerPlayer> blockers(ServerLevel level, Area area, Set<UUID> participants) {
        return List.of();
    }







    private static boolean physicallyLocked(ServerLevel level, Area area) {
        return ModList.get().isLoaded("jem_twelve_eyes")&&com.siirio.jemtwelveeyes.api.CampaignApi.locked(level.getServer(),area.boss());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && ACTIVATION_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock()))
                && !allowNativeSpawn(level, event.getPos())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("jem.event.start_before_ritual"), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel() instanceof ServerLevel level && ACTIVATION_ITEMS.contains(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))
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
    public static void stopped(ServerStoppedEvent event) { AREAS.clear(); INSIDE.clear(); NEAR.clear(); PARTY_CACHE.clear(); PRESENCE.clear(); NATIVE_LOOKUPS.clear(); }

    private static BoundingBox arenaShell(BoundingBox bounds) {
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
