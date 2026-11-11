package dev.taste.engine.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalDouble;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/** Non-blocking GPU timer query ring. It never waits for an unavailable result. */
public final class PassTimerRing implements AutoCloseable {
    private record Pending(int query, long frame) { }
    private final Deque<Integer> free = new ArrayDeque<>();
    private final Deque<Pending> pending = new ArrayDeque<>();
    private boolean active;
    private int activeQuery;
    public PassTimerRing(int size) { RenderSystem.assertOnRenderThread(); for(int i=0;i<Math.max(2,size);i++)free.add(GL15.glGenQueries()); }
    public void begin() { if(active||free.isEmpty())return;activeQuery=free.remove();GL33.glBeginQuery(GL33.GL_TIME_ELAPSED,activeQuery);active=true; }
    public void end(long frame) { if(!active)return;GL33.glEndQuery(GL33.GL_TIME_ELAPSED);pending.add(new Pending(activeQuery,frame));active=false; }
    public OptionalDouble poll() {
        Pending first=pending.peek();if(first==null||GL15.glGetQueryObjecti(first.query(),GL15.GL_QUERY_RESULT_AVAILABLE)==0)return OptionalDouble.empty();
        pending.remove();long nanos=GL33.glGetQueryObjectui64(first.query(),GL15.GL_QUERY_RESULT);free.add(first.query());return OptionalDouble.of(nanos/1_000_000.0);
    }
    @Override public void close(){if(active){GL33.glEndQuery(GL33.GL_TIME_ELAPSED);active=false;}free.forEach(GL15::glDeleteQueries);pending.forEach(value->GL15.glDeleteQueries(value.query()));free.clear();pending.clear();}
}
