package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.ArrayDeque;
import java.util.Deque;

/** Routes getMainRenderTarget only inside an explicit owned world scope. */
public final class WorldTargetRouter {
    private static final ThreadLocal<Deque<RenderTarget>> ROUTES = ThreadLocal.withInitial(ArrayDeque::new);
    private WorldTargetRouter() { }

    public static AutoCloseable route(RenderTarget target) {
        ROUTES.get().push(target);
        return () -> {
            RenderTarget removed = ROUTES.get().poll();
            if (removed != target) throw new IllegalStateException("World target routes closed out of order");
            if (ROUTES.get().isEmpty()) ROUTES.remove();
        };
    }

    public static RenderTarget current() { return ROUTES.get().peek(); }
    public static void clear() { ROUTES.remove(); }
}
