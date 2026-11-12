package dev.taste.engine.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;

/** Shares the active native sprite's frame clock with its companion channels. */
public final class SpriteAnimationClock {
    private static final int MAX_FRAMES = 256;
    private static final Map<SpriteContents, Timeline> TIMELINES = new WeakHashMap<>();
    private static final Map<TextureAtlasSprite, Clock> CLOCKS = new WeakHashMap<>();

    record Frame(int index, int ticks) { }

    record Step(int index, int next, int subFrame, int frameTicks) {
        static final Step FIRST = new Step(0, 0, 0, 1);
    }

    record Timeline(int sheetWidth, int sheetHeight, int frameWidth, int frameHeight,
            List<Frame> frames, int duration, boolean interpolate) {
        Step step(long elapsed) {
            int time = Math.floorMod(elapsed, duration);
            for (int index = 0; index < frames.size(); index++) {
                Frame frame = frames.get(index);
                if (time < frame.ticks()) {
                    int next = frames.get((index + 1) % frames.size()).index();
                    return interpolate && time > 0 && next != frame.index()
                            ? new Step(frame.index(), next, time, frame.ticks())
                            : new Step(frame.index(), frame.index(), 0, 1);
                }
                time -= frame.ticks();
            }
            return Step.FIRST;
        }
    }

    private static final class Clock {
        private volatile long ticks;
    }

    private SpriteAnimationClock() { }

    public static synchronized void register(SpriteContents contents, FrameSize size, int sheetWidth, int sheetHeight,
            AnimationMetadataSection metadata, boolean nativeAnimated) {
        if (!nativeAnimated) return;
        Timeline timeline = describe(metadata, sheetWidth, sheetHeight, size.width(), size.height());
        if (timeline != null) TIMELINES.put(contents, timeline);
    }

    public static synchronized TextureAtlasSprite.Ticker wrap(TextureAtlasSprite sprite, TextureAtlasSprite.Ticker ticker) {
        if (ticker == null || !TIMELINES.containsKey(sprite.contents())) return ticker;
        Clock clock = new Clock();
        CLOCKS.put(sprite, clock);
        return new TextureAtlasSprite.Ticker() {
            @Override public void tickAndUpload() {
                ticker.tickAndUpload();
                clock.ticks++;
            }

            @Override public void close() {
                try {
                    ticker.close();
                } finally {
                    synchronized (SpriteAnimationClock.class) {
                        CLOCKS.remove(sprite, clock);
                    }
                }
            }
        };
    }

    static synchronized Timeline timeline(TextureAtlasSprite sprite) {
        return CLOCKS.containsKey(sprite) ? TIMELINES.get(sprite.contents()) : null;
    }

    static synchronized Step step(TextureAtlasSprite sprite, Timeline timeline) {
        Clock clock = CLOCKS.get(sprite);
        return clock == null ? null : timeline.step(clock.ticks);
    }

    static Timeline describe(AnimationMetadataSection metadata, int width, int height, int frameWidth, int frameHeight) {
        if (frameWidth < 1 || frameHeight < 1 || width % frameWidth != 0 || height % frameHeight != 0) return null;
        long slots = (long) (width / frameWidth) * (height / frameHeight);
        if (slots > MAX_FRAMES) return null;
        List<Frame> frames = new ArrayList<>();
        metadata.forEachFrame((index, ticks) -> {
            if (frames.size() <= MAX_FRAMES) frames.add(new Frame(index, ticks));
        });
        if (frames.isEmpty()) {
            for (int index = 0; index < slots; index++) frames.add(new Frame(index, metadata.getDefaultFrameTime()));
        }
        if (frames.size() > MAX_FRAMES) return null;
        long duration = 0;
        for (Frame frame : frames) {
            if (frame.index() < 0 || frame.index() >= slots || frame.ticks() < 1) return null;
            duration += frame.ticks();
        }
        if (duration <= 0 || duration > Integer.MAX_VALUE) return null;
        return new Timeline(width, height, frameWidth, frameHeight, List.copyOf(frames), (int) duration,
                metadata.isInterpolatedFrames());
    }
}
