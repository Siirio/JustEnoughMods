package dev.taste.engine.render;

import java.util.concurrent.atomic.AtomicLong;

/** Opens one immutable frame and its matching view history ticket. */
public final class FrameCoordinator {
    public record Frame(FrameSnapshot snapshot, WorldViewScope.ViewKey view, TemporalHistory history,
            TemporalHistory.Ticket ticket, long ordinal) { }
    private final AtomicLong ordinals = new AtomicLong();
    private final ViewHistoryRegistry histories = new ViewHistoryRegistry();
    private final DebugAttachments debug = new DebugAttachments();
    public Frame begin(FrameSnapshot snapshot, WorldViewScope.ViewKey view, boolean cameraCut) {
        debug.beginFrame();
        TemporalHistory history = histories.history(view);
        history.observe(snapshot, cameraCut);
        return new Frame(snapshot, view, history, history.beginFrame(), ordinals.incrementAndGet());
    }
    public boolean complete(Frame frame) { return frame.history().completeFrame(frame.ticket()); }
    public void invalidate(TemporalHistory.Reason reason) { histories.invalidateAll(reason); }
    public DebugAttachments debug() { return debug; }
    public void clear() { histories.clear(); debug.beginFrame(); }
}
