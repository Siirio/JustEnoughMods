package dev.taste.engine.environment;

import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.server.ClientEnvironmentState;
import dev.taste.engine.server.EnvironmentMessages;
import dev.taste.engine.server.EnvironmentMessages.Zone;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Applies optional remote inputs only to the local atmosphere. */
public final class EnvironmentZoneResolver {
    private static final Comparator<Zone> PRIORITY = Comparator.comparingInt(Zone::priority).reversed().thenComparing(Zone::id);
    private ResourceLocation dimension;
    private final Vector3f color = new Vector3f(1);
    private float density = 1;
    private long lastNanos;

    public record Atmosphere(float density, Vector3f color) { }

    public Atmosphere resolve(ResourceLocation current, Vec3 position, long gameTime, PipelineParameters options,
            float baseDensity, Vector3f baseColor) {
        if (baseDensity <= 0 || !options.value("general.enabled").getAsBoolean()
                || !options.value("server.acceptEnvironment").getAsBoolean()) {
            clear();
            return new Atmosphere(baseDensity, baseColor);
        }
        long now = System.nanoTime();
        if (!current.equals(dimension)) {
            clear();
            dimension = current;
        }
        var snapshot = ClientEnvironmentState.view(current);
        var goal = snapshot == null ? new Atmosphere(1, new Vector3f(1))
                : evaluate(snapshot.zones(), position, Math.max(gameTime, snapshot.worldTime()),
                        options.value("server.maxZones").getAsInt());
        float seconds = options.value("server.blendSeconds").getAsFloat();
        float elapsed = lastNanos == 0 ? 1.0F / 60.0F : Math.min(1, (now - lastNanos) * 1.0e-9F);
        float blend = seconds <= 0 ? 1 : 1 - (float) Math.exp(-elapsed * 3.0F / seconds);
        density += (goal.density() - density) * blend;
        color.lerp(goal.color(), blend);
        lastNanos = now;

        float fog = baseDensity * density;
        Vector3f tint = new Vector3f(baseColor).mul(color);
        if (!options.value("accessibility.reducedMotion").getAsBoolean()) {
            float limit = options.value("accessibility.flashLimit").getAsFloat();
            for (var active : ClientEnvironmentState.pulses(current, now)) {
                var pulse = active.pulse();
                double distance = position.distanceToSqr(pulse.x() + 0.5, pulse.y() + 0.5, pulse.z() + 0.5);
                if (distance >= 24.0 * 24.0) continue;
                float phase = (float) ((now - active.arrivalNanos()) / (pulse.duration() * 50_000_000.0));
                float envelope = (float) Math.sin(Math.PI * phase);
                float seeded = 0.8F + 0.2F * ((pulse.seed() ^ (pulse.seed() >>> 32)) & 255L) / 255.0F;
                float weight = pulse.intensity() * limit * envelope * seeded * (float) (1 - Math.sqrt(distance) / 24);
                float extra = switch (pulse.effect()) { case MIST -> 0.012F; case DUST -> 0.009F; case GLOW -> 0.003F; };
                Vector3f pulseColor = switch (pulse.effect()) {
                    case MIST -> new Vector3f(0.52F, 0.65F, 0.77F);
                    case DUST -> new Vector3f(0.60F, 0.38F, 0.19F);
                    case GLOW -> new Vector3f(0.74F, 0.56F, 0.28F);
                };
                fog += extra * weight;
                tint.lerp(pulseColor, weight * 0.15F);
            }
        }
        return new Atmosphere(Math.min(baseDensity * 2 + 0.024F, fog), tint);
    }

    private static Atmosphere evaluate(List<Zone> zones, Vec3 position, long gameTime, int requestedMaximum) {
        int maximum = Math.max(0, Math.min(EnvironmentMessages.MAX_ZONES, requestedMaximum));
        int priority = -1;
        float total = 0;
        float fog = 0;
        Vector3f tint = new Vector3f();
        for (Zone zone : zones.stream().sorted(PRIORITY).limit(maximum).toList()) {
            if (zone.expiresAt() <= gameTime) continue;
            float weight = weight(zone, position);
            if (weight <= 0) continue;
            if (priority != -1 && zone.priority() < priority) break;
            priority = zone.priority();
            float profileFog = switch (zone.profile()) { case NEUTRAL -> 1; case MIST -> 1.25F; case DUST -> 1.15F; case VOID -> 0.6F; };
            float red = zone.red();
            float green = zone.green();
            float blue = zone.blue();
            switch (zone.profile()) {
                case MIST -> red *= 0.94F;
                case DUST -> { green *= 0.87F; blue *= 0.74F; }
                case VOID -> { red *= 0.82F; green *= 0.78F; }
                default -> { }
            }
            tint.add(red * weight, green * weight, blue * weight);
            fog += Math.min(2, zone.fog() * profileFog) * weight;
            total += weight;
        }
        if (total <= 0) return new Atmosphere(1, new Vector3f(1));
        float influence = Math.min(1, total);
        return new Atmosphere(1 + (fog / total - 1) * influence, new Vector3f(1).lerp(tint.div(total), influence));
    }

    private static float weight(Zone zone, Vec3 position) {
        double x = Math.max(0, Math.max(zone.minX() - position.x, position.x - zone.maxX()));
        double y = Math.max(0, Math.max(zone.minY() - position.y, position.y - zone.maxY()));
        double z = Math.max(0, Math.max(zone.minZ() - position.z, position.z - zone.maxZ()));
        double distance = Math.sqrt(x * x + y * y + z * z);
        if (distance <= 0) return 1;
        if (zone.fade() <= 0 || distance >= zone.fade()) return 0;
        float ratio = 1.0F - (float) (distance / zone.fade());
        return ratio * ratio * (3 - 2 * ratio);
    }

    public void clear() {
        dimension = null;
        color.set(1);
        density = 1;
        lastNanos = 0;
    }
}
