package dev.taste.engine.environment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/** Resolves one coherent client environment snapshot without changing gameplay state. */
public final class EnvironmentResolver {
    public EnvironmentSnapshot resolve(Minecraft minecraft, float partialTick) {
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null) throw new IllegalStateException("No client level");
        long time = level.getGameTime();
        float phase = level.getTimeOfDay(partialTick);
        double windAngle = (time + partialTick) * 0.0025;
        return new EnvironmentSnapshot(level.dimension().location().toString(), time, phase,
                level.getRainLevel(partialTick), level.getThunderLevel(partialTick),
                level.getSkyDarken(partialTick), level.getSunAngle(partialTick), level.getMoonPhase(),
                (float) Math.cos(windAngle), (float) Math.sin(windAngle),
                player == null ? 0.0F : (float) player.getEyeY(),
                player != null && player.isUnderWater(), player != null && player.isInLava(),
                level.dimensionType().hasSkyLight());
    }
}
