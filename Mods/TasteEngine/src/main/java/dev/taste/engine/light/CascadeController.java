package dev.taste.engine.light;

public final class CascadeController {
    public record Cascade(float near, float far, float texelWorldSize) { }
    public Cascade[] splits(float near, float far, int count, float lambda, int resolution) {
        if (near <= 0.0F || far <= near || count < 1 || count > 4 || resolution < 1) throw new IllegalArgumentException("Invalid cascade setup");
        Cascade[] cascades = new Cascade[count];
        float previous = near;
        for (int i = 1; i <= count; i++) {
            float ratio = i / (float) count;
            float logarithmic = near * (float) Math.pow(far / near, ratio);
            float uniform = near + (far - near) * ratio;
            float split = logarithmic * lambda + uniform * (1.0F - lambda);
            cascades[i - 1] = new Cascade(previous, split, (split - previous) * 2.0F / resolution);
            previous = split;
        }
        return cascades;
    }
    public float stabilize(float coordinate, float texelWorldSize) { return Math.round(coordinate / texelWorldSize) * texelWorldSize; }
}
