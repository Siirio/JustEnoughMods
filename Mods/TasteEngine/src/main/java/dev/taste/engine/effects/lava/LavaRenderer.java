package dev.taste.engine.effects.lava;

public record LavaRenderer(float waveHeight, float crust, float heatDistortion, float emission, float visibility) {
    public LavaRenderer { if (waveHeight < 0 || crust < 0 || crust > 1 || heatDistortion < 0 || emission < 0 || visibility < 0) throw new IllegalArgumentException("Invalid lava settings"); }
    public float distortion(float x, float z, float time) { return (float) (Math.sin(x * 1.7 + time) * Math.cos(z * 1.3 - time * 0.8)) * waveHeight * heatDistortion; }
}
