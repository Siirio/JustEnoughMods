package dev.taste.engine.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Nested view identity. GUI frames never enter this scope. */
public final class WorldViewScope implements AutoCloseable {
    public record ViewKey(String dimension, int ordinal) {
        public ViewKey { Objects.requireNonNull(dimension); }
    }
    private static final ThreadLocal<Deque<ViewKey>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private final ViewKey key;
    private boolean closed;

    private WorldViewScope(ViewKey key) {
        this.key = key;
        STACK.get().push(key);
    }

    public static WorldViewScope enter(ViewKey key) { return new WorldViewScope(Objects.requireNonNull(key)); }
    public static ViewKey current() { return STACK.get().peek(); }
    public ViewKey key() { return key; }

    @Override public void close() {
        if (closed) return;
        ViewKey removed = STACK.get().poll();
        if (!key.equals(removed)) throw new IllegalStateException("World view scopes closed out of order");
        if (STACK.get().isEmpty()) STACK.remove();
        closed = true;
    }
}
