package dev.taste.engine.render;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Original frame-boundary transaction. EVERY rendering hook must gate on active(). */
public final class RendererLifecycle {
    public enum State { OFF, ENABLING, ON, DISABLING, FAILED }
    public interface Transaction extends AutoCloseable {
        void prepare() throws Exception;   // Allocate/compile privately; publish nothing yet.
        void commit() throws Exception;    // Attach only after all required resources exist.
        void rollback() throws Exception;  // Detach + restore vanilla state; valid even after partial prepare.
        @Override void close() throws Exception; // Release ALL resources; try every release even if one fails.
    }
    @FunctionalInterface public interface Factory {
        Transaction create() throws Exception; // No GPU allocation until prepare(): rollback handle must exist.
    }
    private final Thread owner = Thread.currentThread();
    private final AtomicBoolean requested = new AtomicBoolean();
    private final Factory factory;
    private final TemporalHistory history;
    private volatile State state = State.OFF;
    private volatile Exception lastFailure;
    private Transaction active;

    public RendererLifecycle(Factory factory, TemporalHistory history) {
        this.factory = Objects.requireNonNull(factory);
        this.history = Objects.requireNonNull(history);
    }
    public void requestEnabled(boolean enabled) { requested.set(enabled); }
    public boolean active() { return state == State.ON; }
    public State state() { return state; }
    public Optional<Exception> lastFailure() { return Optional.ofNullable(lastFailure); }

    /** Call once before starting a world frame, never halfway through draw submission. */
    public State onFrameBoundary() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Render thread required");
        if (requested.get() && !active()) {
            state = State.ENABLING;
            history.invalidate(TemporalHistory.Reason.RENDERER_TOGGLE);
            Transaction candidate = null;
            try {
                candidate = Objects.requireNonNull(factory.create());
                candidate.prepare();
                candidate.commit();
                active = candidate;
                lastFailure = null;
                state = State.ON;
            } catch (Exception failure) {
                lastFailure = cleanup(candidate, failure);
                requested.set(false); // Explicit retry required; never recompile on every frame after failure.
                state = State.FAILED;
            }
        } else if (!requested.get() && active()) {
            state = State.DISABLING; // Gates close BEFORE adapter detaches or cleanup can throw.
            history.invalidate(TemporalHistory.Reason.RENDERER_TOGGLE);
            lastFailure = cleanup(active, null);
            active = null;
            state = lastFailure == null ? State.OFF : State.FAILED;
        }
        return state;
    }
    private static Exception cleanup(Transaction transaction, Exception failure) {
        if (transaction == null) return failure;
        try { transaction.rollback(); } catch (Exception e) { failure = accumulate(failure, e); }
        try { transaction.close(); } catch (Exception e) { failure = accumulate(failure, e); }
        return failure;
    }
    private static Exception accumulate(Exception first, Exception next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }
}
