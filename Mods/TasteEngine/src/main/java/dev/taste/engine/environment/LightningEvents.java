package dev.taste.engine.environment;

import dev.taste.engine.TasteMod;
import dev.taste.engine.render.PipelineParameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tracks the short visual response of real lightning entities received by the client. */
@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightningEvents {
    public record Flash(double x, double y, double z, float strength) { }
    private record Strike(int id, double x, double y, double z, long tick, boolean confirmed) {
        Strike confirm() { return new Strike(id, x, y, z, tick, true); }
    }

    private static final LightningEvents INSTANCE = new LightningEvents();
    private static final int MAX_STRIKES = 24;
    private static final double LIFETIME_TICKS = 14.0;
    private static final float REDUCED_MOTION_CAP = 0.22F;
    private final Map<UUID, Strike> strikes = new LinkedHashMap<>();
    private ClientLevel owner;

    private LightningEvents() { }

    public static LightningEvents instance() { return INSTANCE; }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clientJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ClientLevel level && event.getEntity() instanceof LightningBolt bolt)
            INSTANCE.joined(level, bolt);
    }

    @SubscribeEvent
    public static void clientUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) INSTANCE.clearLevel(level);
    }

    private synchronized void joined(ClientLevel level, LightningBolt bolt) {
        selectLevel(level);
        if (bolt.level() != level) return;
        long tick = level.getGameTime();
        strikes.values().removeIf(strike -> tick - strike.tick() >= LIFETIME_TICKS || tick < strike.tick());
        strikes.remove(bolt.getUUID());
        strikes.put(bolt.getUUID(), new Strike(bolt.getId(), bolt.getX(), bolt.getY(), bolt.getZ(), tick, false));
        if (strikes.size() > MAX_STRIKES) strikes.remove(strikes.keySet().iterator().next());
    }

    public synchronized List<Flash> flashes(ClientLevel level, float partialTick, PipelineParameters parameters) {
        selectLevel(level);
        if (level == null || strikes.isEmpty()) return List.of();
        double now = level.getGameTime() + Math.max(0.0F, Math.min(1.0F, partialTick));
        strikes.values().removeIf(strike -> now - strike.tick() >= LIFETIME_TICKS || now < strike.tick());
        float peak = effectiveStrength(parameters);
        if (peak <= 0.0F) return List.of();
        List<Flash> result = new ArrayList<>(strikes.size());
        for (Map.Entry<UUID, Strike> entry : strikes.entrySet()) {
            Strike strike = entry.getValue();
            if (!strike.confirmed()) {
                if (!(level.getEntity(strike.id()) instanceof LightningBolt bolt) || !entry.getKey().equals(bolt.getUUID()))
                    continue;
                strike = strike.confirm();
                entry.setValue(strike);
            }
            double age = now - strike.tick();
            float decay = (float) Math.exp(-age * 0.24);
            result.add(new Flash(strike.x(), strike.y(), strike.z(), peak * decay));
        }
        return List.copyOf(result);
    }

    public static float effectiveStrength(PipelineParameters parameters) {
        if (!parameters.value("weather.lightningBolts").getAsBoolean()) return 0.0F;
        float cap = parameters.value("accessibility.flashLimit").getAsFloat();
        if (parameters.value("accessibility.reducedMotion").getAsBoolean())
            cap = Math.min(cap, REDUCED_MOTION_CAP);
        return Math.max(0.0F, Math.min(cap, parameters.value("weather.thunderFlash").getAsFloat()));
    }

    public synchronized void clear() {
        owner = null;
        strikes.clear();
    }

    private synchronized void clearLevel(ClientLevel level) {
        if (owner == level) clear();
    }

    private void selectLevel(ClientLevel level) {
        if (owner == level) return;
        owner = level;
        strikes.clear();
    }
}
