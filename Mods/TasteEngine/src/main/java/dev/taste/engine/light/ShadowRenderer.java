package dev.taste.engine.light;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/** Bounded shadow update scheduler. Geometry submission stays with the backend adapter. */
public final class ShadowRenderer {
    public record Update(String id, int faceCount, float influence, Runnable render) { }
    private final Queue<Update> queue = new ArrayDeque<>();
    public void submit(Update update) { queue.add(update); }
    public List<String> renderBudget(int maximumFaces) {
        int used = 0;
        List<String> rendered = new ArrayList<>();
        while (!queue.isEmpty()) {
            Update update = queue.peek();
            if (used + update.faceCount() > maximumFaces) break;
            queue.remove().render().run();
            used += update.faceCount();
            rendered.add(update.id());
        }
        return rendered;
    }
    public void clear() { queue.clear(); }
}
