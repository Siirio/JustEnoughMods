package dev.taste.engine.server;

import dev.taste.engine.server.EnvironmentMessages.Zone;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Datapack zone publication. Reload failure leaves the last valid map published. */
public final class EnvironmentZoneStore {
    private Map<ResourceLocation, List<Zone>> zones = Map.of();

    public synchronized void replace(Map<ResourceLocation, List<Zone>> next) {
        var copy = new java.util.LinkedHashMap<ResourceLocation, List<Zone>>();
        next.forEach((dimension, candidates) -> {
            EnvironmentMessages.validateDimension(dimension);
            if (candidates.size() > EnvironmentMessages.MAX_ZONES)
                throw new IllegalArgumentException("Too many environment zones in " + dimension);
            var immutable = List.copyOf(candidates);
            if (immutable.stream().map(Zone::id).distinct().count() != immutable.size())
                throw new IllegalArgumentException("Duplicate environment zones in " + dimension);
            copy.put(dimension, immutable);
        });
        zones = Map.copyOf(copy);
    }

    public synchronized List<Zone> active(ResourceLocation dimension, long worldTime) {
        return zones.getOrDefault(dimension, List.of()).stream()
                .filter(zone -> worldTime < zone.expiresAt()).toList();
    }

    public synchronized void clear() { zones = Map.of(); }
}
