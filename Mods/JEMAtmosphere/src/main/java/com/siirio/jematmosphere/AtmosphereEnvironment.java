package com.siirio.jematmosphere;

import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;

public final class AtmosphereEnvironment {
    private AtmosphereEnvironment() {
    }

    public static boolean isShaderPackActive() {
        return ModList.get().isLoaded("oculus") && Oculus.shadersEnabled();
    }

    static Celestial sun(ClientLevel level, float partialTick) {
        float angle = level.getSunAngle(partialTick);
        float altitude = Mth.cos(angle);
        float strength = 0F;
        if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling()) {
            strength = Mth.clamp(altitude * 4F, 0F, 1F) * (1F - level.getRainLevel(partialTick) * 0.85F);
        }
        return new Celestial(-Mth.sin(angle), altitude, strength);
    }

    static Celestial celestial(ClientLevel level, float partialTick) {
        Celestial day = sun(level, partialTick);
        if (day.y() >= 0F) {
            return day;
        }
        float strength = 0F;
        if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling()) {
            float phase = level.getMoonBrightness();
            strength = Mth.clamp(-day.y() * 4F, 0F, 1F) * (0.16F + 0.24F * phase)
                    * (1F - level.getRainLevel(partialTick) * 0.85F);
        }
        return new Celestial(-day.x(), -day.y(), strength);
    }

    record Celestial(float x, float y, float strength) {
    }

    private static final class Oculus {
        private static boolean shadersEnabled() {
            return IrisApi.getInstance().getConfig().areShadersEnabled();
        }
    }
}
