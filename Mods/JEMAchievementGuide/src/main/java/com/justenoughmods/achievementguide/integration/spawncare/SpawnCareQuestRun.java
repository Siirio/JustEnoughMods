package com.justenoughmods.achievementguide.integration.spawncare;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
public final class SpawnCareQuestRun {
    public final Entity source; public final ServerPlayer actor; public final ItemStack input; public final List<Entity> created = new ArrayList<>();
    private static final ThreadLocal<ArrayDeque<SpawnCareQuestRun>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);
    private SpawnCareQuestRun(Entity source, ServerPlayer actor, ItemStack input) { this.source = source; this.actor = actor; this.input = input.copy(); }
    public static SpawnCareQuestRun begin(Entity source, ServerPlayer actor, ItemStack input) { if (actor == null || source.level() != actor.level()) return null; var run = new SpawnCareQuestRun(source, actor, input); CURRENT.get().push(run); return run; }
    public static void inserted(Entity entity, boolean accepted) { if (accepted) for (var run : CURRENT.get()) if (entity.level() == run.actor.level() && run.created.size() < 64) run.created.add(entity); }
    public static void end(SpawnCareQuestRun run) { if (run == null) return; var q = CURRENT.get(); q.removeFirstOccurrence(run); if (q.isEmpty()) CURRENT.remove(); }
}
