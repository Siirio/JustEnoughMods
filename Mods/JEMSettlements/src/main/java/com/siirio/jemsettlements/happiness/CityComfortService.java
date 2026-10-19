package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

/** Comfort belongs to occupied homes and their used streets, never to empty claimed land. */
public final class CityComfortService {
    private static final ArrayDeque<CityComfortObservation> QUEUE = new ArrayDeque<>();
    private static final Set<UUID> QUEUED = new HashSet<>();
    private static int townOffset;
    private CityComfortService() { }

    public static void clear() { QUEUE.clear(); QUEUED.clear(); townOffset = 0; }

    public static void invalidate(MinecraftServer server,
            ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos changed) {
        for (var town : SettlementSavedData.get(server).towns()) {
            if (!town.dimension().equals(dimension.location()) || changed.getX() < town.territoryMinX() - 12
                    || changed.getX() > town.territoryMaxX() + 12 || changed.getZ() < town.territoryMinZ() - 12
                    || changed.getZ() > town.territoryMaxZ() + 12) continue;
            CompoundTag evidence = town.comfortChunks().getCompound("OccupiedStreets");
            evidence.putLong("Time", Long.MIN_VALUE);
            town.comfortChunks().put("OccupiedStreets", evidence);
            // A generation cannot combine blocks observed before and after an edit.
            QUEUE.removeIf(job -> job.town.id().equals(town.id()));
            QUEUED.remove(town.id());
        }
    }

    public static void tick(MinecraftServer server) {
        var data = SettlementSavedData.get(server);
        if (server.getTickCount() % 20 == 0) {
            var towns = java.util.List.copyOf(data.towns());
            if (!towns.isEmpty()) {
                TownRecord town = towns.get(Math.floorMod(townOffset++, towns.size()));
                var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, town.dimension()));
                if (level != null) {
                    publishMetrics(town);
                    var prior = town.comfortChunks().getCompound("OccupiedStreets");
                    long time = prior.getLong("Time"), age = level.getGameTime() - time;
                    if (QUEUE.size() < 4 && !QUEUED.contains(town.id())
                            && (!prior.getBoolean("Confirmed") || time == Long.MIN_VALUE || age < 0 || age >= 600
                            || !CityComfortObservation.signature(data, town).equals(prior.getString("Signature")))) {
                        QUEUE.addLast(new CityComfortObservation(data, town)); QUEUED.add(town.id());
                    }
                    data.changed();
                }
            }
        }
        int budget = SettlementMetricsConfig.SCAN_BUDGET.get();
        // One slice per admitted town per tick also bounds work on cached observations.
        for (int jobs = QUEUE.size(); jobs > 0 && budget > 0; jobs--) {
            var job = QUEUE.removeFirst();
            var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, job.town.dimension()));
            if (level == null || data.town(job.town.id()).orElse(null) != job.town
                    || !job.signature.equals(CityComfortObservation.signature(data, job.town))) {
                QUEUED.remove(job.town.id()); continue;
            }
            budget -= job.advance(level, Math.min(256, budget));
            if (!job.done) { QUEUE.addLast(job); continue; }
            QUEUED.remove(job.town.id());
            if (!job.unavailable) {
                job.town.comfortChunks().getAllKeys().stream().filter(key -> !key.equals("OccupiedStreets"))
                        .toList().forEach(job.town.comfortChunks()::remove);
                job.town.comfortChunks().put("OccupiedStreets", job.result(level.getGameTime()));
            } else {
                // Unknown observations do not overwrite confirmed coverage, including after restart.
                var prior = job.town.comfortChunks().getCompound("OccupiedStreets");
                prior.putLong("Time", level.getGameTime() - 500);
                job.town.comfortChunks().put("OccupiedStreets", prior);
            }
            publishMetrics(job.town); data.changed();
        }
    }

    private static void publishMetrics(TownRecord town) {
        var metrics = town.metrics();
        var evidence = town.comfortChunks().getCompound("OccupiedStreets");
        var categories = new CompoundTag();
        for (String key : new String[] {"Path", "Lighting", "Greenery"})
            categories.putDouble(key, evidence.getBoolean("Confirmed") ? evidence.getDouble(key) : 0);
        categories.putDouble("Animals", Math.max(0, Math.min(100, metrics.getDouble("Animals"))) / 4D);
        double comfort = categories.getAllKeys().stream().mapToDouble(categories::getDouble).sum();
        metrics.put("ComfortCategories", categories); metrics.putDouble("ComfortExact", comfort);
        metrics.putBoolean("ComfortConfirmed", evidence.getBoolean("Confirmed"));
        metrics.putInt("Comfort", (int) Math.round(comfort)); town.metrics(metrics);
    }

    /** Registry-group facts remain explicitly aggregate; coverage is not a per-block reward. */
    public static Map<String, CompoundTag> projectionFacts(TownRecord town) {
        var facts = new java.util.HashMap<String, CompoundTag>();
        var categories = town.metrics().getCompound("ComfortCategories");
        var sources = observedSources(town);
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.getCompound(i); var row = new CompoundTag();
            String category = source.getString("Category");
            row.putString("Category", category); row.putBoolean("CityAggregate", true);
            row.putDouble("CategoryPoints", categories.getDouble(category)); row.putInt("CategoryCap", 25);
            row.putBoolean("Counted", categories.getDouble(category) > 0);
            facts.put(source.getString("Block"), row);
        }
        return Map.copyOf(facts);
    }

    public static ListTag observedSources(TownRecord town) {
        return town.comfortChunks().getCompound("OccupiedStreets").getList("Sources", Tag.TAG_COMPOUND).copy();
    }
}
