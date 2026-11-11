package dev.taste.engine.render.reflection;

public record SsrTracer(int steps, float maximumDistance, float thickness, float roughnessCutoff, float resolutionScale) {
    public SsrTracer {
        if (steps < 1 || maximumDistance <= 0 || thickness <= 0 || roughnessCutoff < 0 || roughnessCutoff > 1 || resolutionScale <= 0 || resolutionScale > 1)
            throw new IllegalArgumentException("Invalid SSR settings");
    }
    public float confidence(float edgeDistance, float rayLength, boolean depthHit, boolean reactive) {
        if (!depthHit || reactive) return 0.0F;
        float edge = Math.max(0.0F, Math.min(1.0F, edgeDistance * 8.0F));
        float distance = Math.max(0.0F, 1.0F - rayLength / maximumDistance);
        return edge * distance;
    }
}
