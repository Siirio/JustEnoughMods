package dev.taste.engine.environment;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Read-only adapter for the installed Serene Seasons API. */
final class SeasonalAppearance {
    private record Adapter(Method state, Method tropicalBiome, Method subSeason, Method tropicalSeason,
            Method cycleTicks, Method subSeasonTicks) { }
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Vector3f[] TINTS = {
            new Vector3f(1.02F, 1.02F, 0.98F), new Vector3f(1.06F, 1.02F, 0.94F),
            new Vector3f(1.12F, 0.99F, 0.90F), new Vector3f(0.92F, 0.99F, 1.10F)
    };
    private Adapter adapter;
    private boolean resolved;

    void tint(Level level, BlockPos position, Vector3f color, float blend) {
        if (blend <= 0) return;
        if (!resolved) resolve();
        if (adapter == null) return;
        try {
            Object state = adapter.state().invoke(null, level);
            if (state == null) return;
            if ((boolean) adapter.tropicalBiome().invoke(null, level.getBiome(position))) {
                Enum<?> tropical = (Enum<?>) adapter.tropicalSeason().invoke(state);
                if (tropical == null) return;
                Vector3f factor = tropical.name().contains("WET")
                        ? new Vector3f(0.97F, 1.01F, 1.04F) : new Vector3f(1.07F, 1.01F, 0.94F);
                color.mul(new Vector3f(1).lerp(factor, blend));
                return;
            }
            Enum<?> season = (Enum<?>) adapter.subSeason().invoke(state);
            if (season == null || season.ordinal() >= 12) return;
            int duration = (int) adapter.subSeasonTicks().invoke(state);
            int elapsed = (int) adapter.cycleTicks().invoke(state);
            float progress = duration <= 0 ? 0 : Math.floorMod(elapsed, duration) / (float) duration;
            float phase = (season.ordinal() + progress) / 3F;
            int whole = (int) phase;
            int index = whole & 3;
            Vector3f factor = new Vector3f(TINTS[index]).lerp(TINTS[(index + 1) % 4], phase - whole);
            color.mul(new Vector3f(1).lerp(factor, blend));
        } catch (ReflectiveOperationException | RuntimeException failure) {
            adapter = null;
            LOGGER.warn("Taste season appearance is unavailable. {}", failure.getMessage());
        }
    }

    private void resolve() {
        resolved = true;
        if (!ModList.get().isLoaded("sereneseasons")) return;
        try {
            Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
            Class<?> state = Class.forName("sereneseasons.api.season.ISeasonState");
            adapter = new Adapter(helper.getMethod("getSeasonState", Level.class),
                    helper.getMethod("usesTropicalSeasons", Holder.class), state.getMethod("getSubSeason"),
                    state.getMethod("getTropicalSeason"), state.getMethod("getSeasonCycleTicks"),
                    state.getMethod("getSubSeasonDuration"));
        } catch (ReflectiveOperationException | LinkageError failure) {
            LOGGER.warn("Taste could not attach to the installed season API. {}", failure.getMessage());
        }
    }
}
