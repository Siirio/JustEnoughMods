package com.justenoughmods.achievementguide.integration.wildlife;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Exact synchronous native operation scope, recording only accepted inserted entities. */
public final class WildlifeQuestRun {
    public final Entity source;
    public final ServerPlayer actor;
    public final ItemStack input;
    public final List<Entity> created = new ArrayList<>();
    private static final ThreadLocal<Deque<WildlifeQuestRun>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);
    public WildlifeQuestRun(Entity source, ServerPlayer actor, ItemStack input) { this.source = source; this.actor = actor; this.input = input.copy(); }
    public static WildlifeQuestRun begin(Entity source, ServerPlayer actor, ItemStack input) {
        if (actor == null || source.level() != actor.level()) return null;
        var run = new WildlifeQuestRun(source, actor, input); CURRENT.get().push(run); return run;
    }
    public static void inserted(Entity entity, boolean accepted) {
        var run = CURRENT.get().peek();
        if (run != null && accepted && entity.level() == run.actor.level() && run.created.size() < 64) run.created.add(entity);
    }
    public static void end(WildlifeQuestRun run) {
        if (run == null) return;
        var stack = CURRENT.get(); if (stack.peek() == run) stack.pop(); if (stack.isEmpty()) CURRENT.remove();
    }
    public void transformed(ItemStack after, String targetType, String action) {
        if (!source.isRemoved() || !WildlifeQuestProof.debited(input, after, 1)) return;
        for (Entity entity : created) if (WildlifeQuestProof.type(entity).equals(targetType) && actor.serverLevel().getEntity(entity.getUUID()) == entity && !entity.isRemoved()) {
            WildlifeQuestProof.mark(entity, "native_transformation_actor", actor.getUUID());
            WildlifeQuestProof.data(entity).putUUID("native_transformation_source", source.getUUID());
            WildlifeQuestProof.emit(actor, action); return;
        }
    }
}
