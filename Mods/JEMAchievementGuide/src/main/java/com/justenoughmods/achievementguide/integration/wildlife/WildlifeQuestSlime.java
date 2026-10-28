package com.justenoughmods.achievementguide.integration.wildlife;
import java.util.*;
import net.minecraft.core.BlockPos;
public final class WildlifeQuestSlime {
    public record Drain(BlockPos root, Set<Long> water) {}
    private static final ThreadLocal<ArrayDeque<Drain>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);
    private WildlifeQuestSlime() {}
    public static void begin(BlockPos root) { CURRENT.get().push(new Drain(root.immutable(), new HashSet<>())); }
    public static void drained(BlockPos pos) { var run = CURRENT.get().peek(); if (run != null && run.water().size() < 256) run.water().add(pos.asLong()); }
    public static boolean water(BlockPos pos) { var run = CURRENT.get().peek(); return run != null && run.water().contains(pos.asLong()); }
    public static void end() { var q = CURRENT.get(); if (!q.isEmpty()) q.pop(); if (q.isEmpty()) CURRENT.remove(); }
}
