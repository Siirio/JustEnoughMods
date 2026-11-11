package dev.taste.engine.render.reflection;

public final class PlanarCapture implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private boolean entered;
    public boolean enter() {
        int depth = DEPTH.get();
        if (depth > 0) return false;
        DEPTH.set(depth + 1); entered = true; return true;
    }
    @Override public void close() { if (entered) { DEPTH.set(Math.max(0, DEPTH.get() - 1)); if (DEPTH.get() == 0) DEPTH.remove(); entered = false; } }
}
