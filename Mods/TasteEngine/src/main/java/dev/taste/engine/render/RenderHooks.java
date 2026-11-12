package dev.taste.engine.render;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** Narrow Mixin boundary. All policy stays in renderer-owned classes. */
public final class RenderHooks {
    public interface Listener {
        void beginWorld(Camera camera, Matrix4f cameraView, Matrix4f projection, float partialTick);
        void beforeTranslucentTerrain();
        void beforeActors();
        void endWorld();
        void beginLayer(RenderType type);
        void endLayer(RenderType type);
        void beginHand();
        void endHand();
    }

    private static final ThreadLocal<Deque<String>> LAYERS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Integer> WORLD_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> GAME_WORLD_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> WORLD_DEFERRED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> HAND_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static volatile Listener listener;
    private RenderHooks() { }

    public static void setListener(Listener next) { listener = next; }
    public static void beginGameWorld() { GAME_WORLD_DEPTH.set(GAME_WORLD_DEPTH.get() + 1); }
    public static void afterForgeWorld() {
        if (GAME_WORLD_DEPTH.get() == 1 && WORLD_DEFERRED.get() && WORLD_DEPTH.get() == 1) finishWorld();
    }
    public static void endGameWorld() {
        int depth = GAME_WORLD_DEPTH.get();
        if (depth == 1) {
            try { afterForgeWorld(); }
            finally { GAME_WORLD_DEPTH.remove(); }
        } else if (depth > 1) GAME_WORLD_DEPTH.set(depth - 1);
    }
    public static void beginWorld(Camera camera, Matrix4f cameraView, Matrix4f projection, float partialTick) {
        int depth = WORLD_DEPTH.get();
        WORLD_DEPTH.set(depth + 1);
        Listener active = listener;
        if (depth == 0 && active != null) active.beginWorld(camera, cameraView, projection, partialTick);
    }
    public static void endWorld() {
        int depth = WORLD_DEPTH.get();
        if (depth > 1) {
            WORLD_DEPTH.set(depth - 1);
            return;
        }
        if (depth == 0) return;
        if (GAME_WORLD_DEPTH.get() > 0) {
            WORLD_DEFERRED.set(true);
            return;
        }
        finishWorld();
    }
    private static void finishWorld() {
        Listener active = listener;
        try {
            if (active != null) active.endWorld();
        } finally {
            LAYERS.remove();
            WORLD_DEPTH.remove();
            WORLD_DEFERRED.remove();
        }
    }
    public static void beforeTranslucentTerrain() {
        Listener active = listener;
        if (WORLD_DEPTH.get() == 1 && active != null) active.beforeTranslucentTerrain();
    }
    public static void beforeActors() {
        Listener active = listener;
        if (WORLD_DEPTH.get() == 1 && active != null) active.beforeActors();
    }
    public static void beginLayer(RenderType type) {
        LAYERS.get().push(type.toString());
        Listener active = listener;
        if (WORLD_DEPTH.get() == 1 && active != null) active.beginLayer(type);
    }
    public static void endLayer(RenderType type) {
        String expected = type.toString();
        String actual = LAYERS.get().poll();
        if (!expected.equals(actual)) throw new IllegalStateException("Terrain layers closed out of order");
        Listener active = listener;
        if (WORLD_DEPTH.get() == 1 && active != null) active.endLayer(type);
    }
    public static void beginHand() {
        int depth = HAND_DEPTH.get();
        HAND_DEPTH.set(depth + 1);
        Listener active = listener;
        if (depth == 0 && active != null) active.beginHand();
    }
    public static void endHand() {
        int depth = Math.max(0, HAND_DEPTH.get() - 1);
        HAND_DEPTH.set(depth);
        if (depth == 0) {
            Listener active = listener;
            if (active != null) active.endHand();
            HAND_DEPTH.remove();
        }
    }
    public static boolean worldActive() { return WORLD_DEPTH.get() > 0; }
    public static boolean handActive() { return HAND_DEPTH.get() > 0; }
    public static String currentLayer() { return LAYERS.get().peek(); }
}
