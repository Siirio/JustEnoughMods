package dev.taste.engine.render.reflection;

import java.util.ArrayList;
import java.util.List;

public final class DepthPyramid {
    public record Level(int index, int width, int height) { }
    public List<Level> layout(int width, int height) {
        List<Level> levels = new ArrayList<>();
        int index = 0;
        while (true) {
            levels.add(new Level(index++, width, height));
            if (width == 1 && height == 1) break;
            width = Math.max(1, width / 2); height = Math.max(1, height / 2);
        }
        return levels;
    }
}
