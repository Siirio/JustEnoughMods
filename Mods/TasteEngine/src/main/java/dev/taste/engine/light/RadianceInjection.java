package dev.taste.engine.light;

public final class RadianceInjection {
    public float[] bounded(float red, float green, float blue, float opacity, float maximumEnergy) {
        float scale = Math.max(0.0F, opacity);
        float r = Math.max(0.0F, red) * scale, g = Math.max(0.0F, green) * scale, b = Math.max(0.0F, blue) * scale;
        float energy = r + g + b;
        if (energy > maximumEnergy && energy > 0.0F) { float factor = maximumEnergy / energy; r *= factor; g *= factor; b *= factor; }
        return new float[] { r, g, b };
    }
}
