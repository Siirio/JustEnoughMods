package com.siirio.jemsettlements.happiness;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Bounded incremental route policy; the world seam supplies real collision and support observations. */
public final class ThreatRouteModel {
    public record Cell(int x, int y, int z) {
        public Cell offset(int dx, int dy, int dz) { return new Cell(x + dx, y + dy, z + dz); }
    }
    public enum Access { OPEN, BLOCKED, UNKNOWN }
    public enum Result { PENDING, REACHABLE, BLOCKED, UNKNOWN }
    public enum Sight { CLEAR, GLASS, BLOCKED, UNKNOWN }
    public enum Threat { NONE, GLASS, NORMAL }
    public record Evidence(Threat threat, boolean complete) { }
    public static Evidence assess(Result route, Sight sight) {
        if (sight == Sight.CLEAR || route == Result.REACHABLE) return new Evidence(Threat.NORMAL, true);
        return new Evidence(sight == Sight.GLASS ? Threat.GLASS : Threat.NONE,
                route == Result.BLOCKED && sight != Sight.UNKNOWN);
    }
    public interface Grid {
        Access stand(Cell cell);
        Access step(Cell from, Cell to);
    }
    public static final class Search {
        private final Grid grid;
        private final Cell start, goal;
        private final int radius, maximum;
        private final ArrayDeque<Cell> pending = new ArrayDeque<>();
        private final Set<Cell> visited = new HashSet<>();
        private boolean unknown, initialized;
        private Result result = Result.PENDING;
        public Search(Grid grid, Cell start, Cell goal, int radius, int maximum) {
            this.grid = grid; this.start = start; this.goal = goal;
            this.radius = Math.max(1, radius); this.maximum = Math.max(1, maximum);
        }
        public Result result() { return result; }
        public int visited() { return visited.size(); }
        public Result advance(int budget) {
            if (result != Result.PENDING || budget <= 0) return result;
            if (!initialized) {
                initialized = true;
                Access origin = grid.stand(start);
                if (origin != Access.OPEN) return result = origin == Access.UNKNOWN ? Result.UNKNOWN : Result.BLOCKED;
                pending.add(start); visited.add(start);
            }
            while (budget-- > 0 && !pending.isEmpty()) {
                Cell current = pending.removeFirst();
                if (current.equals(goal)) return result = Result.REACHABLE;
                for (int direction = 0; direction < 4; direction++) {
                    int dx = direction == 0 ? 1 : direction == 1 ? -1 : 0;
                    int dz = direction == 2 ? 1 : direction == 3 ? -1 : 0;
                    for (int dy = 0; dy <= 2; dy++) {
                        Cell next = current.offset(dx, dy == 2 ? -1 : dy, dz);
                        if (visited.contains(next)) continue;
                        boolean outside = Math.abs(next.x() - start.x()) > radius || Math.abs(next.z() - start.z()) > radius
                                || Math.abs(next.y() - start.y()) > radius;
                        Access access = grid.stand(next);
                        if (access == Access.UNKNOWN) { unknown = true; continue; }
                        if (access == Access.BLOCKED) continue;
                        access = grid.step(current, next);
                        if (access == Access.UNKNOWN) { unknown = true; continue; }
                        if (access == Access.BLOCKED) continue;
                        if (outside) { unknown = true; continue; }
                        if (visited.size() >= maximum) return result = Result.UNKNOWN;
                        visited.add(next); pending.addLast(next);
                    }
                }
            }
            if (pending.isEmpty()) result = unknown ? Result.UNKNOWN : Result.BLOCKED;
            return result;
        }
    }
    /** Shared state projection: acute attack fear fades only after a completed safe observation. */
    public static int fear(int current, int observed, boolean complete, int maximum, int recovery) {
        int next = observed > 0 ? Math.max(current, observed) : complete ? Math.max(0, current - recovery) : current;
        return Math.max(0, Math.min(maximum, next));
    }
    private ThreatRouteModel() { }
}
