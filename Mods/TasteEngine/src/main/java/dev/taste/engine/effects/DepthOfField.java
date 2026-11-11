package dev.taste.engine.effects;

public record DepthOfField(float focusDistance, float aperture, float maximumRadius, boolean handAffected) {
    public float circleOfConfusion(float depth) { return Math.min(maximumRadius, Math.abs(depth-focusDistance)/Math.max(depth,0.05F)*aperture); }
}
