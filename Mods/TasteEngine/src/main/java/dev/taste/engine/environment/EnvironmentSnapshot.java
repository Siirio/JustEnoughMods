package dev.taste.engine.environment;

/** Immutable environment inputs shared by all passes in one frame. */
public record EnvironmentSnapshot(String dimension, long gameTime, float dayPhase, float rain, float thunder,
        float skyDarken, float sunAngle, float moonPhase, float windX, float windZ,
        float cameraY, boolean underwater, boolean lava, boolean hasSky) {
    public EnvironmentSnapshot {
        if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("Dimension required");
        if (!finite(dayPhase, rain, thunder, skyDarken, sunAngle, moonPhase, windX, windZ, cameraY))
            throw new IllegalArgumentException("Non-finite environment value");
    }
    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }
}
