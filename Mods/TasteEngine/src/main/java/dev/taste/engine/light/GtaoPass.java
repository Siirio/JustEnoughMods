package dev.taste.engine.light;

public record GtaoPass(float radius, float strength, int directions, int steps, float thickness, float temporalWeight) {
    public GtaoPass {
        if (radius <= 0 || strength < 0 || directions < 1 || steps < 1 || thickness < 0 || temporalWeight < 0 || temporalWeight > 1)
            throw new IllegalArgumentException("Invalid GTAO parameters");
    }
}
