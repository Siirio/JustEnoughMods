package dev.taste.engine.server;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Common protocol values. The optional environment channel never carries renderer state. */
public final class EnvironmentMessages {
    public static final int MAX_ZONES = 32;
    public static final int MAX_CHANGES = 8;
    public static final int MAX_PAYLOAD = 4096;
    public static final int MAX_ZONE_ID = 48;
    public static final int MAX_DIMENSION_ID = 96;
    public static final int ZONES = 1;
    public static final int PULSES = 2;

    private EnvironmentMessages() { }

    public enum Profile { NEUTRAL, MIST, DUST, VOID }
    public enum Effect { MIST, DUST, GLOW }
    public record Hello(int protocol, int features) { }
    public record Preference(boolean acceptEnvironment) { }

    public record Zone(String id, Profile profile, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            int priority, float fade, float fog, float red, float green, float blue, long expiresAt) {
        public Zone {
            validateZoneId(id);
            Objects.requireNonNull(profile);
            axis(minX, maxX, 30_000_000, 8192);
            axis(minY, maxY, 4096, 2048);
            axis(minZ, maxZ, 30_000_000, 8192);
            if (priority < 0 || priority > 127 || expiresAt < 1 || expiresAt > Long.MAX_VALUE / 2)
                throw new IllegalArgumentException("Invalid environment zone priority or expiry");
            range(fade, 0.0F, 128.0F);
            range(fog, 0.0F, 2.0F);
            range(red, 0.0F, 2.0F);
            range(green, 0.0F, 2.0F);
            range(blue, 0.0F, 2.0F);
        }
    }

    public record Snapshot(ResourceLocation dimension, long generation, long worldTime, List<Zone> zones) {
        public Snapshot {
            validateHeader(dimension, generation, worldTime);
            zones = List.copyOf(zones);
            validateZones(zones, MAX_ZONES);
        }
    }

    public record Delta(ResourceLocation dimension, long generation, long worldTime, long previousGeneration,
            List<String> removed, List<Zone> zones) {
        public Delta {
            validateHeader(dimension, generation, worldTime);
            if (previousGeneration <= 0 || generation != previousGeneration + 1)
                throw new IllegalArgumentException("Invalid environment delta generation");
            removed = List.copyOf(removed);
            zones = List.copyOf(zones);
            if (removed.size() + zones.size() > MAX_CHANGES)
                throw new IllegalArgumentException("Too many environment changes");
            validateZones(zones, MAX_CHANGES);
            HashSet<String> identities = new HashSet<>(removed);
            if (identities.size() != removed.size()) throw new IllegalArgumentException("Duplicate environment removal");
            removed.forEach(EnvironmentMessages::validateZoneId);
            for (Zone zone : zones) {
                if (identities.contains(zone.id())) throw new IllegalArgumentException("Conflicting environment changes");
            }
        }
    }

    public record Pulse(ResourceLocation dimension, Effect effect, int x, int y, int z, int duration,
            float intensity, long seed) {
        public Pulse {
            validateDimension(dimension);
            Objects.requireNonNull(effect);
            axis(x, x, 30_000_000, 0);
            axis(y, y, 4096, 0);
            axis(z, z, 30_000_000, 0);
            if (duration < 1 || duration > 200) throw new IllegalArgumentException("Invalid cosmetic pulse duration");
            range(intensity, 0.0F, 1.0F);
        }
    }

    static void validateDimension(ResourceLocation dimension) {
        if (dimension == null || dimension.toString().length() > MAX_DIMENSION_ID)
            throw new IllegalArgumentException("Invalid environment dimension");
    }

    static void validateZoneId(String id) {
        if (id == null || id.length() > MAX_ZONE_ID || ResourceLocation.tryParse(id) == null || !id.contains(":"))
            throw new IllegalArgumentException("Invalid environment zone ID");
    }

    private static void validateHeader(ResourceLocation dimension, long generation, long worldTime) {
        validateDimension(dimension);
        if (generation <= 0 || worldTime < 0) throw new IllegalArgumentException("Invalid environment generation or time");
    }

    private static void validateZones(List<Zone> zones, int maximum) {
        if (zones.size() > maximum) throw new IllegalArgumentException("Too many environment zones");
        HashSet<String> identities = new HashSet<>();
        for (Zone zone : zones) {
            if (!identities.add(zone.id())) throw new IllegalArgumentException("Duplicate environment zone");
        }
    }

    private static void axis(int min, int max, int limit, int span) {
        if (min < -limit || max > limit || min > max || max - min > span)
            throw new IllegalArgumentException("Invalid environment bounds");
    }

    private static void range(float value, float min, float max) {
        if (!Float.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException("Invalid environment scalar");
    }
}
