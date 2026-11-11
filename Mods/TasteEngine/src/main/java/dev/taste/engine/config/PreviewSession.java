package dev.taste.engine.config;

public final class PreviewSession {
    public enum State { IDLE, STAGED, VISIBLE, ACCEPTED, REVERTED, FAILED }
    private final ConfigSnapshot original;
    private final ConfigSnapshot candidate;
    private State state=State.STAGED;
    private long visibleAtNanos;
    public PreviewSession(ConfigSnapshot original,ConfigSnapshot candidate){this.original=original;this.candidate=candidate;}
    public void visible(){if(state!=State.STAGED)throw new IllegalStateException("Preview not staged");state=State.VISIBLE;visibleAtNanos=System.nanoTime();}
    public long remainingSeconds(int duration){if(state!=State.VISIBLE)return 0;long elapsed=(System.nanoTime()-visibleAtNanos)/1_000_000_000L;return Math.max(0,duration-elapsed);}
    public ConfigSnapshot accept(){if(state!=State.VISIBLE)throw new IllegalStateException("Preview not visible");state=State.ACCEPTED;return candidate;}
    public ConfigSnapshot revert(){state=State.REVERTED;return original;}
    public void fail(){state=State.FAILED;}
    public State state(){return state;}
}
