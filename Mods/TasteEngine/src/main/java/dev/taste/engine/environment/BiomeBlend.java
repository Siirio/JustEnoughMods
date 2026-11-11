package dev.taste.engine.environment;

import java.util.List;

public final class BiomeBlend {
    public record Sample(float red, float green, float blue, float fog, float weight) { }
    public Sample blend(List<Sample> samples) {
        float r = 0, g = 0, b = 0, fog = 0, weight = 0;
        for (Sample sample : samples) {
            float w = Math.max(0.0F, sample.weight());
            r += sample.red() * w; g += sample.green() * w; b += sample.blue() * w; fog += sample.fog() * w; weight += w;
        }
        if (weight <= 1.0e-6F) return new Sample(1, 1, 1, 0, 1);
        return new Sample(r / weight, g / weight, b / weight, fog / weight, 1);
    }
}
