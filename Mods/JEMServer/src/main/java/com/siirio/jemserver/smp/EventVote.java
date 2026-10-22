package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventVote {
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private Integer wave;
    boolean waveChanged;
    private Long ends;
    boolean endsChanged;
    private Long remindAt;
    boolean remindAtChanged;
    private Boolean resolved;
    boolean resolvedChanged;
    private String decision;
    boolean decisionChanged;
    private Long decisionAt;
    boolean decisionAtChanged;
    private Map<UUID, String> votes;
    boolean votesChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventVote copy() {
        var copy = new EventVote();
        copy.source = source.copy();
        copy.wave = wave;
        copy.waveChanged = waveChanged;
        copy.ends = ends;
        copy.endsChanged = endsChanged;
        copy.remindAt = remindAt;
        copy.remindAtChanged = remindAtChanged;
        copy.resolved = resolved;
        copy.resolvedChanged = resolvedChanged;
        copy.decision = decision;
        copy.decisionChanged = decisionChanged;
        copy.decisionAt = decisionAt;
        copy.decisionAtChanged = decisionAtChanged;
        copy.votes = votes;
        copy.votesChanged = votesChanged;
        return copy;
    }
    public EventVote snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
    }

    public int wave() { return wave == null ? 0 : wave; }
    public void wave(int value) { requireMutable(); waveChanged = true; wave = value; }
    public boolean hasWave() { return wave != null; }
    public void clearWave() { requireMutable(); waveChanged = true; wave = null; }
    public long ends() { return ends == null ? 0L : ends; }
    public void ends(long value) { requireMutable(); endsChanged = true; ends = value; }
    public boolean hasEnds() { return ends != null; }
    public void clearEnds() { requireMutable(); endsChanged = true; ends = null; }
    public long remindAt() { return remindAt == null ? 0L : remindAt; }
    public void remindAt(long value) { requireMutable(); remindAtChanged = true; remindAt = value; }
    public boolean hasRemindAt() { return remindAt != null; }
    public void clearRemindAt() { requireMutable(); remindAtChanged = true; remindAt = null; }
    public boolean resolved() { return resolved == null ? false : resolved; }
    public void resolved(boolean value) { requireMutable(); resolvedChanged = true; resolved = value; }
    public boolean hasResolved() { return resolved != null; }
    public void clearResolved() { requireMutable(); resolvedChanged = true; resolved = null; }
    public String decision() { return decision == null ? "" : decision; }
    public void decision(String value) { requireMutable(); decisionChanged = true; decision = value; }
    public boolean hasDecision() { return decision != null; }
    public void clearDecision() { requireMutable(); decisionChanged = true; decision = null; }
    public long decisionAt() { return decisionAt == null ? 0L : decisionAt; }
    public void decisionAt(long value) { requireMutable(); decisionAtChanged = true; decisionAt = value; }
    public boolean hasDecisionAt() { return decisionAt != null; }
    public void clearDecisionAt() { requireMutable(); decisionAtChanged = true; decisionAt = null; }
    public Map<UUID, String> votes() { return votes == null ? Map.of() : votes; }
    public void votes(Map<UUID, String> value) { requireMutable(); votesChanged = true; votes = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasVotes() { return votes != null; }
    public void clearVotes() { requireMutable(); votesChanged = true; votes = null; }
}
