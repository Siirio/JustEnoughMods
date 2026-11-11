package dev.taste.engine.light;

public record DirectionalLight(float x, float y, float z, float red, float green, float blue, float intensity) {
    public DirectionalLight {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (!Float.isFinite(length) || length < 1.0e-6F) throw new IllegalArgumentException("Light direction required");
        x /= length; y /= length; z /= length;
        if (!finite(red, green, blue, intensity) || red < 0 || green < 0 || blue < 0 || intensity < 0)
            throw new IllegalArgumentException("Invalid directional radiance");
    }
    private static boolean finite(float... values) { for (float value : values) if (!Float.isFinite(value)) return false; return true; }
}
