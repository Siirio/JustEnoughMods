package com.justenoughmods.achievementguide.integration.home;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Defers native placement evidence until Forge has finished cancellation and rollback. */
public final class HomeItemUseProof {
    private record Pending(BooleanSupplier retained, Runnable committed) {}
    private record Use(ItemStack owner, ItemStack before, UseOnContext context, List<Pending> pending) {}
    private static final ThreadLocal<Deque<Use>> USES = ThreadLocal.withInitial(ArrayDeque::new);

    public static void begin(ItemStack stack, UseOnContext context) {
        Deque<Use> calls = USES.get();
        if (calls.size() >= 16) calls.clear();
        calls.push(new Use(stack, stack.copy(), context, new ArrayList<>()));
    }

    public static ServerPlayer actor() {
        Use use = USES.get().peek();
        return use == null ? null : HomeInteractionProof.actor(use.context.getPlayer());
    }

    public static boolean used(net.minecraft.world.item.Item item) {
        Use use = USES.get().peek();
        return use != null && use.before.is(item);
    }

    public static void defer(ServerPlayer player, BooleanSupplier retained, Runnable committed) {
        Use use = USES.get().peek();
        if (use != null && player != null && player == use.context.getPlayer() && use.pending.size() < 16)
            use.pending.add(new Pending(retained, committed));
    }

    public static void finish(ItemStack owner, UseOnContext context, InteractionResult result) {
        Use use = USES.get().poll();
        if (use == null || use.owner != owner || use.context != context || !result.consumesAction()
                || HomeInteractionProof.actor(context.getPlayer()) == null || context.getPlayer().level() != context.getLevel()) return;
        boolean debit = owner.isEmpty() ? use.before.getCount() == 1
                : ItemStack.isSameItemSameTags(use.before, owner) && use.before.getCount() == owner.getCount() + 1;
        if (debit) for (Pending pending : use.pending) if (pending.retained.getAsBoolean()) pending.committed.run();
        if (USES.get().isEmpty()) USES.remove();
    }

    private HomeItemUseProof() {}
}
