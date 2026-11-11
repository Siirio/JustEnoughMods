package dev.taste.engine.environment;

public final class WindField {
    public record Wind(float x, float y, float z, float gust) { }
    private final long seed;
    public WindField(long seed) { this.seed = seed; }
    public Wind sample(double x, double y, double z, double timeSeconds, float strength) {
        double phase = timeSeconds * 0.35 + hash((long) Math.floor(x / 32.0), (long) Math.floor(z / 32.0)) * 6.2831853;
        float gust = (float) (0.65 + 0.35 * Math.sin(phase * 0.37 + y * 0.01));
        float angle = (float) (phase * 0.11 + Math.sin(phase * 0.07));
        return new Wind((float) Math.cos(angle) * strength * gust, 0.04F * strength * gust,
                (float) Math.sin(angle) * strength * gust, gust);
    }
    private double hash(long x, long z) {
        long value = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ seed;
        value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L; value ^= value >>> 27; value *= 0x94D049BB133111EBL; value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }
}
