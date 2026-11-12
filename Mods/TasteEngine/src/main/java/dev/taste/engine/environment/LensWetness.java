package dev.taste.engine.environment;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Keeps a visual water film while the camera moves between rain, water and shelter. */
public final class LensWetness {
    private ClientLevel level;
    private long sampled;
    private float amount;

    public float update(ClientLevel world, Vec3 camera, EnvironmentSnapshot environment,
            boolean enabled, boolean locked, float drySeconds) {
        if (!enabled || world != level) {
            level = world;
            sampled = 0;
            amount = 0;
        }
        if (!enabled) return 0;
        long now = System.nanoTime();
        float elapsed = sampled == 0 ? 1F / 20F : Math.min(0.25F, (now - sampled) * 1.0e-9F);
        sampled = now;
        if (!locked) {
            if (environment.underwater()) amount = 1;
            else {
                float target = world.isRainingAt(BlockPos.containing(camera.x, camera.y, camera.z)) ? environment.rain() : 0;
                if (target > amount) amount = Math.min(target, amount + elapsed * 2.5F);
                else amount = Math.max(target, amount - elapsed / Math.max(0.01F, drySeconds));
            }
        }
        return environment.underwater() || environment.lava() ? 0 : amount;
    }

    public void clear() { level = null; sampled = 0; amount = 0; }
}
