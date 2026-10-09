package com.siirio.jemserver.smp.events;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;
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
    private static final int ROOF_CLEANUP_PER_TICK = 64;
    private static final int EXIT_SEARCH_MARGIN = 6;
    private static final Map<ResourceLocation, ResourceLocation> STRUCTURES = new HashMap<>();
    private static final Set<ResourceLocation> ACTIVATION_BLOCKS = new HashSet<>();
    private static final Set<ResourceLocation> ACTIVATION_ITEMS = new HashSet<>();
    private static final LinkedHashMap<UUID, Area> AREAS = new LinkedHashMap<>();
    private static final Map<UUID, UUID> INSIDE = new HashMap<>();
    private static final Map<UUID, UUID> LAST_COLLISION = new HashMap<>();
    private static final Map<UUID, Optional<CompoundTag>> PARTY_CACHE = new HashMap<>();
    private static final LinkedHashMap<String, Optional<Area>> NATIVE_LOOKUPS = new LinkedHashMap<>();
    private static final Set<String> PREPARED_BARRIER_CHUNKS = new HashSet<>();
    private static final Set<String> SUSPENDED_BARRIER_CHUNKS = new HashSet<>();
    private static final Map<UUID, Integer> ROOF_CURSORS = new HashMap<>();

    private StructureStaging() {}

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(), "jem/events/structures") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
                STRUCTURES.clear(); ACTIVATION_BLOCKS.clear(); ACTIVATION_ITEMS.clear(); AREAS.clear(); INSIDE.clear(); LAST_COLLISION.clear(); PARTY_CACHE.clear(); NATIVE_LOOKUPS.clear(); PREPARED_BARRIER_CHUNKS.clear(); SUSPENDED_BARRIER_CHUNKS.clear(); ROOF_CURSORS.clear();
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
            synchronizeBarrier(level,area);
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
            if (!AREAS.containsKey(id) && AREAS.size() >= AREA_LIMIT) AREAS.remove(AREAS.keySet().iterator().next());
            AREAS.put(id, area);
            synchronizeBarrier(level,area);
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
        if (!(level instanceof ServerLevel serverLevel)) return true;
        String key = level.dimension().location() + ":" + position.asLong();
        if (!NATIVE_LOOKUPS.containsKey(key)) {
            if (NATIVE_LOOKUPS.size() >= AREA_LIMIT) NATIVE_LOOKUPS.remove(NATIVE_LOOKUPS.keySet().iterator().next());
            NATIVE_LOOKUPS.put(key, Optional.ofNullable(discover(serverLevel, position)));
        }
        Area area = NATIVE_LOOKUPS.get(key).orElse(null);
        if (area == null) return true;
        CompoundTag row = party(serverLevel, area.id());
        return row != null && row.getString("state").equals("ACTIVE");
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
        return AREAS.values().stream()
                .filter(area->area.dimension().equals(level.dimension().location().toString())&&area.boss().equals(bossType)&&area.containsColumn(boss.position()))
                .min(Comparator.comparingDouble(area->area.distance(boss.position())))
                .orElse(null);
    }

    public static boolean raidAvailable(ServerPlayer player, UUID id) {
        Area area = AREAS.get(id);
        CompoundTag event = EventScheduler.active(player.server, "BOSS_RAID");
        return area != null && area.near(player.serverLevel(), player.position()) && event != null && !event.contains("raidArenaId")
                && party(player.serverLevel(), id) == null
                && RaidArenaApi.compatible(player.server, id.toString(), area.boss());
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
            EventParty.join(player, event, solo);
            PARTY_CACHE.put(id, SmpData.get(player.server).all("parties").stream()
                    .filter(value -> value.hasUUID("structureId") && value.getUUID("structureId").equals(id) && !SmpData.closed(value)).findFirst());
            return;
        }
        SmpRecords.require(mode.equals("BOSS_FIGHT"), "unavailable");
        SmpRecords.require(livingBoss(player.serverLevel(), area) != null, "boss_not_respawned");
        CompoundTag row = party(player.serverLevel(), id);
        if (row == null) {
            JsonObject args = new JsonObject();
            args.addProperty("activity", "BOSS");
            args.addProperty("title", Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(area.boss()).getDescriptionId()).getString());
            args.addProperty("solo", Boolean.toString(solo));
            Parties.createHosted(player, args);
            row = SmpData.get(player.server).all("parties").stream().filter(value -> value.getUUID("owner").equals(player.getUUID()))
                    .max(Comparator.comparingLong(value -> value.getLong("created"))).orElseThrow();
            attachArena(row,id,area.boss(),area.dimension(),area.center(),area.bounds());
            row.putBoolean("nativePending", true);
            PARTY_CACHE.put(id, Optional.of(row));
            SmpData.get(player.server).changed(row);
        }
        if (solo) {
            SmpRecords.owner(player, row);
            SmpRecords.require(SmpRecords.members(row).size() == 1, "solo");
            row.putBoolean("solo", true);
            row.remove("invitations");
            SmpRecords.members(row).getCompound(player.getStringUUID()).putBoolean("ready", true);
            HostedParties.start(player, row);
        } else SmpNetwork.open(player, "parties", row.getUUID("id"));
    }

    public static boolean start(ServerPlayer host, CompoundTag row, List<UUID> ids) {
        if (!row.getBoolean("nativePending")) return false;
        Area area = row.hasUUID("structureId") ? AREAS.get(row.getUUID("structureId")) : null;
        SmpRecords.require(area != null, "unavailable");
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
        EventNetwork.prompt(player, area.id(), "BOSS_STRUCTURE", Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(area.boss()).getDescriptionId()).getString(), area.boss().toString());
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

    static ArenaTeleportSafety.Decision validateTeleport(ServerPlayer player,ServerLevel level,Vec3 requested) {
        Area area=discover(level,BlockPos.containing(requested));
        if(area==null||!BoundaryCollision.inside(area.bounds(),player.getBbWidth(),player.getBbHeight()).contains(requested))
            return ArenaTeleportSafety.Decision.unmatched();
        CompoundTag row=party(level,area.id());
        if(row!=null&&row.getString("state").equals("ACTIVE")&&HostedBoundary.legalInside(player,row))
            return ArenaTeleportSafety.Decision.allow(requested);
        BlockPos outside=safeExit(player,level,area,BlockPos.containing(requested));
        return ArenaTeleportSafety.Decision.redirect(outside==null?null:Vec3.atBottomCenterOf(outside));
    }

    public static void finish(net.minecraft.server.MinecraftServer server, CompoundTag row) {
        PartyTeleportFlow.finish(server,row);
        if (!row.hasUUID("structureId")) {
            return;
        }
        Area area = AREAS.get(row.getUUID("structureId"));
        if (area == null) return;
        ServerLevel level = level(server, area);
        if (level == null) return;
        PARTY_CACHE.remove(area.id());
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
        if(area!=null) ejectArenaMobs(level,area,boss);
        else {
            EventRegions.locate(record,level,boss.blockPosition(),bounds.minX(),bounds.minZ(),bounds.maxX(),bounds.maxZ());
            record.putInt("minY",bounds.minY());
            record.putInt("maxY",bounds.maxY());
            var box=new AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX()+1,bounds.maxY()+1,bounds.maxZ()+1);
            level.getEntitiesOfClass(Mob.class,box,candidate->candidate!=boss).forEach(mob->EventRegions.eject(mob,record));
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
                || !level.getFluidState(position).isEmpty() || !level.getFluidState(position.above()).isEmpty()) return false;
        return level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(position.getX() + .5, position.getY(), position.getZ() + .5));
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

    private static BlockPos safeExit(ServerLevel level,BoundingBox bounds,BlockPos origin,java.util.function.Predicate<BlockPos> safe) {
        int minY=Math.max(level.getMinBuildHeight()+1,bounds.minY()-EXIT_SEARCH_MARGIN);
        int maxY=Math.min(level.getMaxBuildHeight()-2,bounds.maxY()+EXIT_SEARCH_MARGIN);
        int anchorY=Math.max(minY,Math.min(maxY,origin.getY()));
        for(int offset=1;offset<=EXIT_SEARCH_MARGIN;offset++) {
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
        var bounds = area.bounds();
        for (LivingEntity boss : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(bounds.minX(), level.getMinBuildHeight(), bounds.minZ(), bounds.maxX() + 1, level.getMaxBuildHeight(), bounds.maxZ() + 1),
                entity -> entity.isAlive() && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(area.boss()))) bind(level, boss, row, ids);
    }

    private static void bind(ServerLevel level, LivingEntity boss, CompoundTag row, List<UUID> ids) {
        if (row.hasUUID("bossEntity") || !row.getBoolean("nativePending") || HostedEncounterApi.isRaid(boss)) return;
        Area area = bossArea(level,boss);
        if (area == null || !blockers(level, area, new HashSet<>(ids)).isEmpty()) return;
        boss.getPersistentData().putBoolean("jem_solo", row.getBoolean("solo"));
        if (HostedEncounterApi.start(boss, ids, false, prepareCombat(level,row,boss))) {
            positionBossForCombat(level,row,boss);
            row.putUUID("bossEntity", boss.getUUID());
            row.putBoolean("nativePending", false);
            Profiles.count(level.getServer(), row.getUUID("owner"), "bossesHosted");
            SmpData.get(level.getServer()).changed(row);
        }
    }

    public static void positionBossForCombat(ServerLevel level,CompoundTag record,LivingEntity boss) {
        if(!record.hasUUID("structureId")) return;
        positionBossForCombat(level,record.getUUID("structureId"),boss);
    }

    private static void positionBossForCombat(ServerLevel level,UUID structureId,LivingEntity boss) {
        ResourceLocation bossType=BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        RaidArenaApi.combatPosition(level.getServer(),structureId.toString(),bossType).ifPresent(position->{
            boss.moveTo(position.getX()+.5,position.getY(),position.getZ()+.5,boss.getYRot(),boss.getXRot());
            boss.setDeltaMovement(Vec3.ZERO);
            if(boss instanceof Mob mob) mob.getNavigation().stop();
        });
    }

    public static void stageBoss(ServerLevel level,CompoundTag record,LivingEntity boss) {
        if(!boss.isAlive()||boss.isRemoved()) return;
        positionBossForCombat(level,record,boss);
        HostedEncounterApi.hold(boss);
    }

    private static void ejectArenaMobs(ServerLevel level,Area area,LivingEntity boss) {
        var bounds=area.bounds();
        var box=new AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX()+1,bounds.maxY()+1,bounds.maxZ()+1);
        for(var mob:level.getEntitiesOfClass(Mob.class,box,candidate->candidate!=boss)) {
            BlockPos exit=safeExit(mob,level,area);
            if(exit==null) continue;
            mob.teleportTo(exit.getX()+.5,exit.getY(),exit.getZ()+.5);
            mob.setDeltaMovement(0,0,0);
            mob.getNavigation().stop();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void spawned(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity boss)
                || WorldTierApi.profile(boss).isEmpty() || boss.getPersistentData().getBoolean(EventSession.SPAWNED)) return;
        var encounter = WorldTierApi.encounter(boss).orElse(null);
        if (encounter == null || encounter.provenance() == com.siirio.jemworldbosstiers.encounter.EncounterProvenance.RAID_EVENT) return;
        Area area = bossArea(level,boss);
        if (area == null) return;
        AABB column = new AABB(area.bounds().minX(), level.getMinBuildHeight(), area.bounds().minZ(), area.bounds().maxX() + 1, level.getMaxBuildHeight(), area.bounds().maxZ() + 1);
        boolean duplicate = !level.getEntitiesOfClass(LivingEntity.class, column, entity -> entity != boss && entity.isAlive()
                && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(area.boss())).isEmpty();
        if (duplicate) { boss.discard(); return; }
        CompoundTag row = party(level, area.id());
        if (row == null || !row.getString("state").equals("ACTIVE")) {
            positionBossForCombat(level,area.id(),boss);
            HostedEncounterApi.hold(boss);
            return;
        }
        level.getServer().execute(() -> {
            if (boss.isAlive() && !boss.isRemoved() && row.getString("state").equals("ACTIVE"))
                bind(level, boss, row, SmpRecords.memberIds(row).stream().filter(id -> Parties.accepted(row, id)).toList());
        });
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator()) return;
        enforceStructureBoundary(player);
        if (player.tickCount % CHECK_INTERVAL != 0) return;
        Area area = discover(player.serverLevel(), player.blockPosition());
        AREAS.values().stream()
                .filter(candidate -> candidate.dimension().equals(player.level().dimension().location().toString()) && candidate.distance(player.position()) <= LANDMARK_RADIUS)
                .forEach(candidate -> Landmarks.publishBoss(player.server, candidate.id(), candidate.boss(), player.level().dimension().location(), candidate.center()));
        if (area == null) { INSIDE.remove(player.getUUID()); return; }
        UUID before = INSIDE.put(player.getUUID(), area.id());
        if (!area.id().equals(before)) {
            CompoundTag row = party(player.serverLevel(), area.id());
            if (row != null) {
                if (row.getString("state").equals("ACTIVE") && row.getBoolean("nativePending"))
                    bindExisting(player.serverLevel(), area, row, SmpRecords.memberIds(row).stream().filter(id -> Parties.accepted(row, id)).toList());
            }
        }
    }

    private static void enforceStructureBoundary(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Vec3 current = player.position();
        Area area=nearby(player.serverLevel(),current);
        if (area == null) {
            UUID last = LAST_COLLISION.get(playerId);
            Area lastArea = last == null ? null : AREAS.get(last);
            if (lastArea == null || lastArea.distance(current) > 3) LAST_COLLISION.remove(playerId);
            return;
        }
        if(suspendedByBloodMoon(player.serverLevel(),area)) {
            LAST_COLLISION.remove(playerId);
            return;
        }
        CompoundTag row = party(player.serverLevel(), area.id());
        boolean participating = row != null && row.getString("state").equals("ACTIVE") && Parties.accepted(row, playerId);
        boolean inside=BoundaryCollision.inside(area.bounds(),player.getBbWidth(),player.getBbHeight()).contains(current);
        if(inside) {
            boolean authorized=participating&&HostedBoundary.authorizeWalkIn(player,row,area.bounds(),new Vec3(player.xo,player.yo,player.zo),current);
            if(!authorized) {
                BlockPos outside=safeExit(player,player.serverLevel(),area,BlockPos.containing(current));
                if(outside!=null) teleport(player,player.serverLevel(),outside);
                return;
            }
        }
        if (participating) return;
        if (area.id().equals(LAST_COLLISION.put(playerId, area.id()))) return;
        if (physicallyLocked(player.serverLevel(),area)||locked(player, area)) {
            com.siirio.jemcompat.gate.CampaignGateEvents.explainLocked(player, area.boss());
            return;
        }
        player.sendSystemMessage(Component.literal("▣ ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(BuiltInRegistries.ENTITY_TYPE.get(area.boss()).getDescriptionId())).append(" ")
                .append(Component.literal("[Открыть бой]").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/smp arena " + area.id())))));
    }

    @SubscribeEvent
    public static void roof(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        for (Area area : AREAS.values()) {
            if (!area.dimension().equals(level.dimension().location().toString())) continue;
            if(level.getGameTime()%CHECK_INTERVAL==0) synchronizeBarrier(level,area);
            BoundingBox shell = area.shell();
            int width = shell.getXSpan();
            int depth = shell.getZSpan();
            int count = width * depth;
            int cursor = ROOF_CURSORS.getOrDefault(area.id(), 0);
            for (int checked = 0; checked < Math.min(ROOF_CLEANUP_PER_TICK, count); checked++) {
                int index = (cursor + checked) % count;
                BlockPos position = new BlockPos(shell.minX() + index % width, shell.maxY() + 1, shell.minZ() + index / width);
                if (level.hasChunkAt(position) && level.getBlockState(position).is(Blocks.SNOW)) level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
            }
            ROOF_CURSORS.put(area.id(), (cursor + ROOF_CLEANUP_PER_TICK) % count);
        }
    }

    private static boolean locked(ServerPlayer player, Area area) {
        return ModList.get().isLoaded("jem_twelve_eyes") && !com.siirio.jemcompat.gate.CampaignGateEvents.unlocked(player, area.boss());
    }

    public static List<EventNetwork.Boundary> boundaries(ServerPlayer player) {
        List<EventNetwork.Boundary> result = new ArrayList<>();
        for (Area area : AREAS.values()) {
            if (!area.dimension().equals(player.level().dimension().location().toString())) continue;
            if (physicallyLocked(player.serverLevel(), area)) continue;
            var bounds = area.shell();
            double distance = Math.max(Math.max(bounds.minX() - player.getX(), player.getX() - bounds.maxX()),
                    Math.max(bounds.minZ() - player.getZ(), player.getZ() - bounds.maxZ()));
            if (distance > EventRules.BOUNDARY_DISTANCE.get()) continue;
            CompoundTag row = party(player.serverLevel(), area.id());
            boolean participating = row != null && row.getString("state").equals("ACTIVE") && Parties.accepted(row, player.getUUID());
            boolean entered=participating&&SmpRecords.members(row).getCompound(player.getStringUUID()).getBoolean("arenaEntered");
            result.add(new EventNetwork.Boundary(area.id(), area.dimension(), "BOSS_FIGHT", bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ(), EventRules.RAID_COLOR.get(), entered, participating&&!entered));
            if (result.size() >= BOUNDARY_LIMIT) break;
        }
        return result;
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

    private static LivingEntity livingBoss(ServerLevel level,Area area) {
        if(!area.dimension().equals(level.dimension().location().toString())) return null;
        BoundingBox bounds=area.bounds();
        return level.getEntitiesOfClass(LivingEntity.class,
                new AABB(bounds.minX(),level.getMinBuildHeight(),bounds.minZ(),bounds.maxX()+1,level.getMaxBuildHeight(),bounds.maxZ()+1),
                entity->entity.isAlive()&&BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(area.boss()))
                .stream().findFirst().orElse(null);
    }

    private static Area nearby(ServerLevel level, Vec3 position) {
        discover(level, BlockPos.containing(position));
        return AREAS.values().stream().filter(area -> area.near(level, position)).findFirst().orElse(null);
    }

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
        return level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator() && !player.hasPermissions(2))
                .filter(player -> !participants.contains(player.getUUID()) && area.bounds().isInside(player.blockPosition()))
                .toList();
    }

    public static boolean canPass(ServerPlayer player,BlockPos position) {
        for(Area area:AREAS.values()) {
            if(!area.dimension().equals(player.level().dimension().location().toString())||!area.onBarrier(position)) continue;
            CompoundTag row=party(player.serverLevel(),area.id());
            return row!=null&&HostedBoundary.canEnter(player,row,area.bounds());
        }
        return false;
    }

    public static boolean touchesBoundary(ServerLevel level,BlockPos position) {
        discover(level,position);
        for(Area area:AREAS.values()) {
            if(!area.dimension().equals(level.dimension().location().toString())) continue;
            if(area.onBarrier(position)) return true;
        }
        return false;
    }

    public static boolean protectsFromFluid(ServerLevel level,BlockPos position) {
        discover(level,position);
        for(Area area:AREAS.values()) {
            if(!area.dimension().equals(level.dimension().location().toString())) continue;
            if(area.onBarrier(position)) return true;
        }
        return false;
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void fluid(BlockEvent.FluidPlaceBlockEvent event) {
        if(event.getLevel() instanceof ServerLevel level&&protectsFromFluid(level,event.getPos())) event.setNewState(event.getOriginalState());
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void piston(PistonEvent.Pre event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        for(int reach=1;reach<=13;reach++) if(touchesBoundary(level,event.getPos().relative(event.getDirection(),reach))) {
            event.setCanceled(true);
            return;
        }
    }

    private static void synchronizeBarrier(ServerLevel level,Area area) {
        if(suspendedByBloodMoon(level,area)) {
            suspendBarrier(level,area);
            return;
        }
        if(!physicallyLocked(level,area)) ensureBarrier(level,area);
    }

    private static boolean suspendedByBloodMoon(ServerLevel level,Area area) {
        CompoundTag event=EventScheduler.active(level.getServer(),"BLOOD_MOON");
        if(event==null||!event.getString("dimension").equals(area.dimension())) return false;
        BoundingBox shell=area.shell();
        return shell.maxX()>=EventRegions.minX(event)&&shell.minX()<=EventRegions.maxX(event)
                &&shell.maxZ()>=EventRegions.minZ(event)&&shell.minZ()<=EventRegions.maxZ(event);
    }

    private static void ensureBarrier(ServerLevel level,Area area) {
        forEachLoadedBarrierChunk(level,area,chunk -> {
            String key=barrierChunkKey(area,chunk);
            SUSPENDED_BARRIER_CHUNKS.remove(key);
            if(PREPARED_BARRIER_CHUNKS.add(key)) placeBarrier(level,area,chunk);
        });
    }

    private static void suspendBarrier(ServerLevel level,Area area) {
        forEachLoadedBarrierChunk(level,area,chunk -> {
            String key=barrierChunkKey(area,chunk);
            if(!SUSPENDED_BARRIER_CHUNKS.add(key)) return;
            PREPARED_BARRIER_CHUNKS.remove(key);
            removeBarrier(level,area,chunk);
        });
    }

    private static void forEachLoadedBarrierChunk(ServerLevel level,Area area,java.util.function.Consumer<ChunkPos> action) {
        BoundingBox shell=area.shell();
        for(int chunkX=shell.minX()>>4;chunkX<=shell.maxX()>>4;chunkX++) for(int chunkZ=shell.minZ()>>4;chunkZ<=shell.maxZ()>>4;chunkZ++) {
            if(level.getChunkSource().getChunkNow(chunkX,chunkZ)==null) continue;
            action.accept(new ChunkPos(chunkX,chunkZ));
        }
    }

    private static String barrierChunkKey(Area area,ChunkPos chunk) {
        return area.id()+":"+chunk.x+":"+chunk.z;
    }

    private static void placeBarrier(ServerLevel level,Area area,ChunkPos chunk) {
        forEachBarrierPosition(area,chunk,position -> placeBarrier(level,position));
    }

    private static void removeBarrier(ServerLevel level,Area area,ChunkPos chunk) {
        forEachBarrierPosition(area,chunk,position -> removeBarrier(level,position));
    }

    private static void forEachBarrierPosition(Area area,ChunkPos chunk,java.util.function.Consumer<BlockPos> action) {
        BoundingBox shell=area.shell();
        int minX=Math.max(shell.minX(),chunk.getMinBlockX()),maxX=Math.min(shell.maxX(),chunk.getMaxBlockX());
        int minZ=Math.max(shell.minZ(),chunk.getMinBlockZ()),maxZ=Math.min(shell.maxZ(),chunk.getMaxBlockZ());
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
            action.accept(new BlockPos(x,shell.minY(),z));
            action.accept(new BlockPos(x,shell.maxY(),z));
        }
        for(int y=shell.minY()+1;y<shell.maxY();y++) {
            if(shell.minZ()>=minZ&&shell.minZ()<=maxZ) for(int x=minX;x<=maxX;x++) action.accept(new BlockPos(x,y,shell.minZ()));
            if(shell.maxZ()>=minZ&&shell.maxZ()<=maxZ) for(int x=minX;x<=maxX;x++) action.accept(new BlockPos(x,y,shell.maxZ()));
            if(shell.minX()>=minX&&shell.minX()<=maxX) for(int z=minZ;z<=maxZ;z++) action.accept(new BlockPos(shell.minX(),y,z));
            if(shell.maxX()>=minX&&shell.maxX()<=maxX) for(int z=minZ;z<=maxZ;z++) action.accept(new BlockPos(shell.maxX(),y,z));
        }
    }

    private static void removeBarrier(ServerLevel level,BlockPos position) {
        if(level.getBlockState(position).is(SmpEventBlocks.STRUCTURE_BARRIER.get())) level.setBlock(position,Blocks.AIR.defaultBlockState(),3);
    }

    private static void placeBarrier(ServerLevel level,BlockPos position) {
        var state=level.getBlockState(position);
        if(state.is(SmpEventBlocks.STRUCTURE_BARRIER.get())||!state.getCollisionShape(level,position).isEmpty()) return;
        level.setBlock(position,SmpEventBlocks.STRUCTURE_BARRIER.get().defaultBlockState(),3);
    }

    private static boolean physicallyLocked(ServerLevel level, Area area) {
        return ModList.get().isLoaded("jem_twelve_eyes")&&com.siirio.jemcompat.gate.CampaignGateEvents.locked(level.getServer(),area.boss());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && ACTIVATION_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock()))
                && !allowNative(level, event.getPos())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("jem.event.start_before_ritual"), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void activateItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel() instanceof ServerLevel level && ACTIVATION_ITEMS.contains(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))
                && !allowNative(level, event.getEntity().blockPosition())) event.setCanceled(true);
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
    public static void stagingDamage(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !allowNative(player.level(), player.blockPosition())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void stagingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !allowNative(player.level(), player.blockPosition())) {
            event.setCanceled(true);
            player.setHealth(Math.max(1, player.getHealth()));
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(net.minecraftforge.event.level.ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        NATIVE_LOOKUPS.clear();
        BlockPos center=event.getChunk().getPos().getMiddleBlockPosition(level.getSeaLevel());
        discover(level,center);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { UUID id = event.getEntity().getUUID(); INSIDE.remove(id); LAST_COLLISION.remove(id); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { AREAS.clear(); INSIDE.clear(); LAST_COLLISION.clear(); PARTY_CACHE.clear(); NATIVE_LOOKUPS.clear(); PREPARED_BARRIER_CHUNKS.clear(); SUSPENDED_BARRIER_CHUNKS.clear(); ROOF_CURSORS.clear(); }

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
            return new BoundingBox(bounds.minX()-1,bounds.minY()-1,bounds.minZ()-1,bounds.maxX()+1,bounds.maxY()+1,bounds.maxZ()+1);
        }

        boolean onBarrier(BlockPos position) {
            BoundingBox shell=shell();
            return shell.isInside(position)&&(position.getX()==shell.minX()||position.getX()==shell.maxX()
                    ||position.getY()==shell.minY()||position.getY()==shell.maxY()
                    ||position.getZ()==shell.minZ()||position.getZ()==shell.maxZ());
        }
    }
}
