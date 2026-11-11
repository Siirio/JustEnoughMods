package dev.taste.engine.effects;

import dev.taste.engine.environment.WindField;

public final class VegetationDeformer {
    public float[] offset(double x, double y, double z, float anchor, float strength, WindField.Wind wind, float phase) {
        float free = Math.max(0.0F, Math.min(1.0F, 1.0F - anchor));
        float gust = (float) Math.sin(phase + x * 0.41 + z * 0.37 + y * 0.13) * 0.5F + 0.5F;
        return new float[] { wind.x() * strength * free * gust, wind.y() * strength * free, wind.z() * strength * free * gust };
    }
}
