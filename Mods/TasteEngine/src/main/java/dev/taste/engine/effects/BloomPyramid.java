package dev.taste.engine.effects;

import java.util.ArrayList;
import java.util.List;

public final class BloomPyramid {
    public record Level(int index, int width, int height) { }
    public List<Level> layout(int width, int height, int requestedLevels) {
        List<Level> levels = new ArrayList<>();
        width = Math.max(1, width / 2); height = Math.max(1, height / 2);
        for (int i = 0; i < requestedLevels && (width > 1 || height > 1); i++) {
            levels.add(new Level(i, width, height)); width = Math.max(1, width / 2); height = Math.max(1, height / 2);
        }
        return levels;
    }
    public float softThreshold(float luminance, float threshold, float knee) {
        float soft = Math.max(0.0F, luminance - threshold + knee);
        soft = soft * soft / Math.max(4.0F * knee + 1.0e-5F, 1.0e-5F);
        return Math.max(luminance - threshold, soft) / Math.max(luminance, 1.0e-5F);
    }
}
