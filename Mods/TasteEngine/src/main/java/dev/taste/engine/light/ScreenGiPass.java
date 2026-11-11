package dev.taste.engine.light;

public record ScreenGiPass(int samples, float maximumDistance, float intensity, float temporalWeight) {
    public ScreenGiPass {
        if (samples < 1 || maximumDistance <= 0 || intensity < 0 || temporalWeight < 0 || temporalWeight > 1)
            throw new IllegalArgumentException("Invalid screen GI parameters");
    }
}
