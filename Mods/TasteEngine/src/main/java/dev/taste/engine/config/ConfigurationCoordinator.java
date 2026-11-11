package dev.taste.engine.config;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Publishes a validated immutable snapshot only at a world-frame boundary. */
public final class ConfigurationCoordinator {
    public record Candidate(ConfigSnapshot snapshot, EffectiveConfig effective) { }
    private final AtomicReference<Candidate> staged = new AtomicReference<>();
    private volatile Candidate accepted;

    public ConfigurationCoordinator(Candidate initial) { accepted = Objects.requireNonNull(initial); }
    public void stage(Candidate candidate) { staged.set(Objects.requireNonNull(candidate)); }
    public Candidate activateAtFrameBoundary() {
        Candidate candidate = staged.getAndSet(null);
        if (candidate != null) accepted = candidate;
        return accepted;
    }
    public Candidate accepted() { return accepted; }
    public void rejectStaged() { staged.set(null); }
}
