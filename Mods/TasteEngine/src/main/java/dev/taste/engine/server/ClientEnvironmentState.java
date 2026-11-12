package dev.taste.engine.server;

import dev.taste.engine.server.EnvironmentMessages.Delta;
import dev.taste.engine.server.EnvironmentMessages.Pulse;
import dev.taste.engine.server.EnvironmentMessages.Snapshot;
import dev.taste.engine.server.EnvironmentMessages.Zone;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;

/** Bounded common inbox. Rendering only reads packets that match its actual client dimension. */
public final class ClientEnvironmentState {
    private static final List<TimedPulse> pulses = new ArrayList<>();
    private static Snapshot snapshot;
    private static boolean supported;
    private static boolean accepted;
    private static Connection connection;
    private static Supplier<ResourceLocation> dimension = () -> null;

    private ClientEnvironmentState() { }

    public record TimedPulse(Pulse pulse, long arrivalNanos) { }

    public static synchronized void begin(Connection peer, Supplier<ResourceLocation> currentDimension) {
        clear();
        connection = peer;
        dimension = currentDimension;
    }

    public static synchronized void preference(boolean value) {
        accepted = value;
        if (!accepted) {
            snapshot = null;
            pulses.clear();
        }
    }

    static synchronized void hello(Connection peer, EnvironmentMessages.Hello message) {
        if (peer != connection) return;
        supported = message.protocol() == EnvironmentChannel.PROTOCOL
                && (message.features() & EnvironmentMessages.ZONES) != 0;
    }

    static synchronized void snapshot(Connection peer, Snapshot next) {
        if (!matches(peer, next.dimension()) || snapshot != null && next.generation() <= snapshot.generation()) return;
        snapshot = next;
        pulses.removeIf(pulse -> !pulse.pulse().dimension().equals(next.dimension()));
    }

    static synchronized void delta(Connection peer, Delta next) {
        if (!matches(peer, next.dimension()) || snapshot == null || !snapshot.dimension().equals(next.dimension())
                || snapshot.generation() != next.previousGeneration()) return;
        LinkedHashMap<String, Zone> zones = new LinkedHashMap<>();
        snapshot.zones().forEach(zone -> zones.put(zone.id(), zone));
        next.removed().forEach(zones::remove);
        next.zones().forEach(zone -> zones.put(zone.id(), zone));
        if (zones.size() > EnvironmentMessages.MAX_ZONES) return;
        snapshot = new Snapshot(next.dimension(), next.generation(), next.worldTime(), List.copyOf(zones.values()));
    }

    static synchronized void pulse(Connection peer, Pulse next) {
        if (!matches(peer, next.dimension()) || snapshot == null || !snapshot.dimension().equals(next.dimension())) return;
        if (pulses.size() == 8) pulses.remove(0);
        pulses.add(new TimedPulse(next, System.nanoTime()));
    }

    private static boolean matches(Connection peer, ResourceLocation expected) {
        return supported && accepted && peer == connection && expected.equals(dimension.get());
    }

    public static synchronized Snapshot view(ResourceLocation dimension) {
        return supported && accepted && snapshot != null && snapshot.dimension().equals(dimension) ? snapshot : null;
    }

    public static synchronized List<TimedPulse> pulses(ResourceLocation dimension, long now) {
        pulses.removeIf(pulse -> now - pulse.arrivalNanos() >= pulse.pulse().duration() * 50_000_000L);
        return pulses.stream().filter(pulse -> pulse.pulse().dimension().equals(dimension)).toList();
    }

    public static synchronized void clear() {
        supported = false;
        accepted = false;
        snapshot = null;
        pulses.clear();
        connection = null;
        dimension = () -> null;
    }
}
