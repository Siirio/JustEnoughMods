package dev.taste.engine.render;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Render-thread owned. A failed or invalidated frame must never publish valid history. */
public final class TemporalHistory {
    public enum Reason { STARTUP, WORLD_CHANGE, DIMENSION_CHANGE, CAMERA_CUT,
        PROJECTION_CHANGE, VIEWPORT_RESIZE, RENDERER_TOGGLE, RESOURCE_RELOAD,
        PIPELINE_CHANGE, MATERIAL_CHANGE, TIME_DISCONTINUITY }
    public record Ticket(long generation, Set<Reason> reasons) {
        public Ticket { reasons = Set.copyOf(reasons); }
    }
    private final EnumSet<Reason> pending = EnumSet.of(Reason.STARTUP);
    private long generation;
    private boolean written;
    private FrameSnapshot previous;

    public void invalidate(Reason reason) {
        pending.add(Objects.requireNonNull(reason));
        generation++;
    }
    public boolean valid() { return written && pending.isEmpty(); }
    public Ticket beginFrame() { return new Ticket(generation, pending); }
    public boolean completeFrame(Ticket ticket) {
        if (ticket.generation != generation) return false;
        pending.clear();
        written = true;
        return true;
    }
    /** Compare UNJITTERED projection; ordinary TAA jitter or origin rebasing is not a camera cut. */
    public void observe(FrameSnapshot next, boolean cameraCut) {
        Objects.requireNonNull(next);
        if (previous != null) {
            if (previous.worldEpoch() != next.worldEpoch()) invalidate(Reason.WORLD_CHANGE);
            if (!previous.dimension().equals(next.dimension())) invalidate(Reason.DIMENSION_CHANGE);
            if (previous.width() != next.width() || previous.height() != next.height()) invalidate(Reason.VIEWPORT_RESIZE);
            if (!previous.unjitteredProjection().equals(next.unjitteredProjection())) invalidate(Reason.PROJECTION_CHANGE);
            if (next.timeSeconds() < previous.timeSeconds()) invalidate(Reason.TIME_DISCONTINUITY);
        }
        if (cameraCut) invalidate(Reason.CAMERA_CUT);
        previous = next;
    }
}
