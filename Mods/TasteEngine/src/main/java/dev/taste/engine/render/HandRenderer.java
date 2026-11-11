package dev.taste.engine.render;

/** Tracks the separate native-resolution first-person layer. */
public final class HandRenderer {
    private boolean capturing;
    private long frame;
    public void begin(long nextFrame) { if (capturing) throw new IllegalStateException("Hand capture already active"); capturing = true; frame = nextFrame; }
    public void end(long expectedFrame) { if (!capturing || frame != expectedFrame) throw new IllegalStateException("Hand capture mismatch"); capturing = false; }
    public boolean capturing() { return capturing; }
}
