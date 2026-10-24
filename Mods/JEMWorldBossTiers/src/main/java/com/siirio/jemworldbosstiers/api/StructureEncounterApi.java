package com.siirio.jemworldbosstiers.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native loaded structure evidence and its canonical encounter identity have one owner. */
@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class StructureEncounterApi {
    public record Arena(UUID id, String dimension, ResourceLocation boss, BoundingBox bounds) { }
    private static volatile Map<ResourceLocation, ResourceLocation> structures = Map.of();
    private static volatile Set<ResourceLocation> activationBlocks = Set.of();
    private static volatile Set<ResourceLocation> activationItems = Set.of();
    private StructureEncounterApi() { }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(), "jem/events/structures") {
            @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
                var registered = new HashMap<ResourceLocation, ResourceLocation>();
                var blocks = new HashSet<ResourceLocation>();
                var items = new HashSet<ResourceLocation>();
                for (var file : files.values()) {
                    var root = file.getAsJsonObject();
                    for (var value : root.getAsJsonArray("structures")) {
                        var entry = value.getAsJsonObject();
                        registered.put(new ResourceLocation(entry.get("structure").getAsString()),
                                new ResourceLocation(entry.get("boss").getAsString()));
                    }
                    root.getAsJsonArray("activation_blocks").forEach(value -> blocks.add(new ResourceLocation(value.getAsString())));
                    root.getAsJsonArray("activation_items").forEach(value -> items.add(new ResourceLocation(value.getAsString())));
                }
                structures = Map.copyOf(registered);
                activationBlocks = Set.copyOf(blocks);
                activationItems = Set.copyOf(items);
            }
        });
    }

    public static boolean activationBlock(ResourceLocation id) { return activationBlocks.contains(id); }
    public static boolean activationItem(ResourceLocation id) { return activationItems.contains(id); }

    /** Saved canonical geometry is authoritative even before a player discovers the arena. */
    public static List<Arena> registeredArenas(net.minecraft.server.MinecraftServer server, ResourceLocation dimension) {
        if (server == null || server.overworld() == null) return List.of();
        return com.siirio.jemworldbosstiers.progression.WorldTierData.get(server).arenas().stream()
                .filter(arena -> arena.structureArena() && arena.dimension().equals(dimension.toString()))
                .map(arena -> RaidArenaApi.arena(server, arena.id())).flatMap(Optional::stream)
                .map(arena -> new Arena(canonicalId(arena.id()), arena.dimension().toString(),
                        arena.entityType(), arena.bounds()))
                .toList();
    }

    private static UUID canonicalId(String id) {
        try { return UUID.fromString(id); }
        catch (IllegalArgumentException namedLegacyArena) {
            return UUID.nameUUIDFromBytes(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public static Optional<Arena> discover(ServerLevel level, BlockPos position) {
        return discoverAll(level, position).stream().filter(arena -> arena.bounds().isInside(position)).findFirst();
    }

    public static Optional<Arena> discover(LivingEntity boss) {
        if (!(boss.level() instanceof ServerLevel level)) return Optional.empty();
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        return discoverAll(level, boss.blockPosition()).stream().filter(arena -> arena.boss().equals(type))
                .filter(arena -> boss.getX() >= arena.bounds().minX() && boss.getX() < arena.bounds().maxX() + 1
                        && boss.getZ() >= arena.bounds().minZ() && boss.getZ() < arena.bounds().maxZ() + 1)
                .min(java.util.Comparator.comparingDouble(arena -> arena.bounds().getCenter().distSqr(boss.blockPosition())));
    }

    private static List<Arena> discoverAll(ServerLevel level, BlockPos position) {
        var chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        if (chunk == null) return List.of();
        var starts = new ArrayList<StructureStart>(chunk.getAllStarts().values());
        for (var reference : chunk.getAllReferences().entrySet()) for (long value : reference.getValue()) {
            ChunkPos origin = new ChunkPos(value);
            var source = level.getChunkSource().getChunkNow(origin.x, origin.z);
            if (source == null) continue;
            var start = source.getStartForStructure(reference.getKey());
            if (start != null) starts.add(start);
        }
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var found = new java.util.LinkedHashMap<UUID, Arena>();
        for (var start : starts) {
            if (!start.isValid()) continue;
            ResourceLocation structure = registry.getKey(start.getStructure());
            ResourceLocation boss = structures.get(structure);
            if (boss == null || com.siirio.jemworldbosstiers.balance.BalanceRegistry.boss(boss).isEmpty()
                    || !net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(boss)) continue;
            BoundingBox bounds = start.getBoundingBox();
            String dimension = level.dimension().location().toString();
            String key = dimension + ":" + structure + ":" + bounds.minX() + ":" + bounds.minZ();
            UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
            RaidArenaApi.register(level.getServer(), id.toString(), boss, level.dimension().location(), bounds);
            RaidArenaApi.migrateNativeAnchor(level.getServer(), id.toString(), boss, start);
            found.put(id, new Arena(id, dimension, boss, bounds));
        }
        return List.copyOf(found.values());
    }

    public static Optional<Arena> bind(LivingEntity boss) {
        var arena = discover(boss);
        if (arena.isPresent() && !HostedEncounterApi.isRaid(boss)) {
            RaidArenaApi.observeNativeBoss(boss.getServer(), arena.get().id().toString(), boss);
        }
        return arena;
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity boss
                && HostedEncounterApi.canHost(boss)) bind(boss);
    }
}
