package dev.taste.engine.render.reflection;

import dev.taste.engine.render.TemporalHistory;

public final class ReflectionHistory {
    private final TemporalHistory history = new TemporalHistory();
    private float weight;
    public TemporalHistory history() { return history; }
    public void configure(float nextWeight) { weight = Math.max(0.0F, Math.min(0.99F, nextWeight)); }
    public float effectiveWeight(boolean hitValid, boolean disoccluded) { return hitValid && !disoccluded && history.valid() ? weight : 0.0F; }
}
