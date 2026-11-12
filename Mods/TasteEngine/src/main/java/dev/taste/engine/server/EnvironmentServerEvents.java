package dev.taste.engine.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.taste.engine.TasteMod;
import dev.taste.engine.server.EnvironmentMessages.Delta;
import dev.taste.engine.server.EnvironmentMessages.Profile;
import dev.taste.engine.server.EnvironmentMessages.Pulse;
import dev.taste.engine.server.EnvironmentMessages.Snapshot;
import dev.taste.engine.server.EnvironmentMessages.Zone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnvironmentServerEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SECOND = 1_000_000_000L;
    private static final EnvironmentZoneStore STORE = new EnvironmentZoneStore();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private EnvironmentServerEvents() { }

    @Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }
        @SubscribeEvent public static void setup(FMLCommonSetupEvent event) { EnvironmentChannel.register(); }
    }

    static void hello(ServerPlayer player, int features) {
        if (SESSIONS.putIfAbsent(player.getUUID(), new Session(features)) == null)
            EnvironmentChannel.announce(player);
    }

    static void preference(ServerPlayer player, boolean accept) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.accept == accept) return;
        session.accept = accept;
        session.dimension = null;
        session.zones = Map.of();
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.accept) return;
        long now = System.nanoTime();
        if (session.lastCheck != 0 && now - session.lastCheck < SECOND) return;
        session.lastCheck = now;
        ResourceLocation dimension = player.serverLevel().dimension().location();
        long time = player.serverLevel().getGameTime();
        var current = new LinkedHashMap<String, Zone>();
        STORE.active(dimension, time).forEach(zone -> current.put(zone.id(), zone));
        boolean full = !dimension.equals(session.dimension) || now - session.lastFull >= 20 * SECOND;
        if (!full && current.equals(session.zones)) return;

        long generation = session.generation + 1;
        var removed = session.zones.keySet().stream().filter(id -> !current.containsKey(id)).toList();
        var changed = current.values().stream().filter(zone -> !zone.equals(session.zones.get(zone.id()))).toList();
        if (full || removed.size() + changed.size() > EnvironmentMessages.MAX_CHANGES) {
            EnvironmentChannel.send(player, new Snapshot(dimension, generation, time, List.copyOf(current.values())));
            session.lastFull = now;
        } else {
            EnvironmentChannel.send(player, new Delta(dimension, generation, time, session.generation, removed, changed));
        }
        session.dimension = dimension;
        session.generation = generation;
        session.zones = current;
    }

    public static void cosmeticPulse(ServerPlayer player, Pulse pulse) {
        Session session = SESSIONS.get(player.getUUID());
        long now = System.nanoTime();
        if (session == null || !session.accept || (session.features & EnvironmentMessages.PULSES) == 0
                || !pulse.dimension().equals(session.dimension) || !pulse.dimension().equals(player.serverLevel().dimension().location())
                || player.distanceToSqr(pulse.x(), pulse.y(), pulse.z()) > 128.0 * 128.0
                || session.lastPulse != 0 && now - session.lastPulse < SECOND) return;
        EnvironmentChannel.send(player, pulse);
        session.lastPulse = now;
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SESSIONS.remove(player.getUUID());
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        SESSIONS.clear();
        STORE.clear();
    }

    @SubscribeEvent public static void resources(AddReloadListenerEvent event) {
        event.addListener(new ZonesReload());
    }

    private static final class Session {
        private final int features;
        private Map<String, Zone> zones = Map.of();
        private ResourceLocation dimension;
        private boolean accept;
        private long generation;
        private long lastFull;
        private long lastCheck;
        private long lastPulse;

        private Session(int features) { this.features = features; }
    }

    private static final class ZonesReload extends SimpleJsonResourceReloadListener {
        private ZonesReload() { super(new Gson(), "taste/environment_zones"); }

        @Override protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            try {
                if (data.size() > 256) throw new IllegalArgumentException("Too many environment zone definitions");
                Map<ResourceLocation, List<Zone>> next = new HashMap<>();
                for (var entry : data.entrySet()) {
                    JsonObject value = entry.getValue().getAsJsonObject();
                    ResourceLocation dimension = new ResourceLocation(value.get("dimension").getAsString());
                    Zone zone = new Zone(entry.getKey().toString(),
                            Profile.valueOf(value.get("profile").getAsString().toUpperCase(Locale.ROOT)),
                            bound(value, "min", 0), bound(value, "min", 1), bound(value, "min", 2),
                            bound(value, "max", 0), bound(value, "max", 1), bound(value, "max", 2),
                            integer(value, "priority", 0), decimal(value, "fade", 8.0F), decimal(value, "fog", 1.0F),
                            decimal(value, "red", 1.0F), decimal(value, "green", 1.0F), decimal(value, "blue", 1.0F),
                            value.get("expiresAt").getAsBigDecimal().longValueExact());
                    next.computeIfAbsent(dimension, ignored -> new ArrayList<>()).add(zone);
                }
                STORE.replace(next);
            } catch (RuntimeException invalid) {
                LOGGER.warn("Taste retained the previous environment zone set. {}", invalid.getMessage());
            }
        }

        private static int bound(JsonObject value, String key, int coordinate) {
            var array = value.getAsJsonArray(key);
            if (array.size() != 3) throw new IllegalArgumentException("Environment AABB needs three coordinates");
            return array.get(coordinate).getAsBigDecimal().intValueExact();
        }

        private static int integer(JsonObject value, String key, int fallback) {
            return value.has(key) ? value.get(key).getAsBigDecimal().intValueExact() : fallback;
        }

        private static float decimal(JsonObject value, String key, float fallback) {
            return value.has(key) ? value.get(key).getAsFloat() : fallback;
        }
    }
}
