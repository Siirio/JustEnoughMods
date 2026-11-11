package dev.taste.engine.material;

import java.util.concurrent.atomic.AtomicLong;

/** Separates render-active material state from worker mesh layout state. */
public final class MaterialGeneration {
    public record Snapshot(long render, long layout) { }
    private final AtomicLong render = new AtomicLong();
    private final AtomicLong layout = new AtomicLong();
    public Snapshot snapshot() { return new Snapshot(render.get(), layout.get()); }
    public long advanceRender() { return render.incrementAndGet(); }
    public long advanceLayout() { return layout.incrementAndGet(); }
    public boolean acceptsLayout(long candidate) { return layout.get() == candidate; }
}
