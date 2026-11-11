package dev.taste.engine.render;

public final class VelocityResolver {
    public record Velocity(float x, float y, boolean valid) { }
    public Velocity fromClip(float currentX, float currentY, float currentW, float previousX, float previousY, float previousW) {
        if (Math.abs(currentW) < 1.0e-6F || Math.abs(previousW) < 1.0e-6F) return new Velocity(0, 0, false);
        float x = currentX / currentW - previousX / previousW;
        float y = currentY / currentW - previousY / previousW;
        return new Velocity(x * 0.5F, y * 0.5F, Float.isFinite(x) && Float.isFinite(y));
    }
}
