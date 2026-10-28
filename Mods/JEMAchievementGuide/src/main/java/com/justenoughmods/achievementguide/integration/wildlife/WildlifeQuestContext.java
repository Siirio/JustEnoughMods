package com.justenoughmods.achievementguide.integration.wildlife;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.world.entity.Entity;

/** Owning native entity tick context avoids access to compiler-generated outer-instance fields. */
public final class WildlifeQuestContext {
    private static final ThreadLocal<Deque<Entity>> TICKS = ThreadLocal.withInitial(ArrayDeque::new);
    private WildlifeQuestContext() {}
    public static void begin(Entity entity) { if (!entity.level().isClientSide) TICKS.get().push(entity); }
    public static Entity current() { return TICKS.get().peek(); }
    public static void end(Entity entity) {
        if (entity.level().isClientSide) return;
        var stack = TICKS.get(); if (stack.peek() == entity) stack.pop(); if (stack.isEmpty()) TICKS.remove();
    }
}
