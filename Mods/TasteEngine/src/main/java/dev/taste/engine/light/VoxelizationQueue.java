package dev.taste.engine.light;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class VoxelizationQueue {
    public record Task(int clipmap, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long generation) { }
    private final Queue<Task> tasks = new ArrayDeque<>();
    private final int maximumQueued;
    public VoxelizationQueue(int maximumQueued) { this.maximumQueued = Math.max(1, maximumQueued); }
    public void submit(Task task) { if (tasks.size() >= maximumQueued) tasks.remove(); tasks.add(task); }
    public List<Task> take(int limit, long generation) {
        List<Task> result = new ArrayList<>();
        while (!tasks.isEmpty() && result.size() < limit) {
            Task task = tasks.remove();
            if (task.generation() == generation) result.add(task);
        }
        return result;
    }
    public void clear() { tasks.clear(); }
}
