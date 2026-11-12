package dev.taste.engine.environment;

import net.minecraft.client.Camera;
import dev.taste.engine.effects.PhotoCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.FogType;

/** Resolves one coherent client environment snapshot without changing gameplay state. */
public final class EnvironmentResolver {
    public EnvironmentSnapshot resolve(Minecraft minecraft, Camera camera, float partialTick) {
        return resolve(minecraft, camera, partialTick, -1, PhotoCapture.instance());
    }

    public EnvironmentSnapshot resolve(Minecraft minecraft, Camera camera, float partialTick, float visualTime, PhotoCapture photo) {
        ClientLevel level = minecraft.level;
        if (level == null) throw new IllegalStateException("No client level");
        long time = level.getGameTime();
        float phase = visualTime < 0 ? level.getTimeOfDay(partialTick) : level.dimensionType().timeOfDay((long) visualTime);
        double windAngle = photo.visualTime((time + partialTick) / 20.0) * 0.05;
        return new EnvironmentSnapshot(level.dimension().location().toString(), time, phase,
                level.getRainLevel(partialTick), level.getThunderLevel(partialTick),
                level.getSkyDarken(partialTick), photo.sunAngle(phase * (float) (Math.PI * 2.0)), level.getMoonPhase(),
                (float) Math.cos(windAngle), (float) Math.sin(windAngle),
                (float) camera.getPosition().y,
                camera.getFluidInCamera() == FogType.WATER, camera.getFluidInCamera() == FogType.LAVA,
                level.dimensionType().hasSkyLight());
    }
}
