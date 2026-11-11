package dev.taste.engine.effects;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class InteractionEvents {
    public enum Type { FOOTSTEP, IMPACT, EXPLOSION, WATER_ENTRY, BLOCK_PLACED }
    public record Event(Type type, double x, double y, double z, float strength, int color, long tick) { }
    private final Queue<Event> events = new ArrayDeque<>();
    private final int capacity;
    public InteractionEvents(int capacity) { this.capacity = Math.max(1, capacity); }
    public void submit(Event event) { if (events.size() >= capacity) events.remove(); events.add(event); }
    public List<Event> drain() { List<Event> result = new ArrayList<>(events); events.clear(); return result; }
}
