package dev.taste.engine.light;

public record ConeTracePass(int cones, float maximumDistance, float aperture, float intensity, boolean secondBounce) {
    public ConeTracePass {
        if (cones < 1 || maximumDistance <= 0 || aperture <= 0 || intensity < 0)
            throw new IllegalArgumentException("Invalid cone trace parameters");
    }
}
