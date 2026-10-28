package com.justenoughmods.achievementguide.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraftforge.items.IItemHandler;

/** Exact bounded inventory observations for a native, explicitly player-operated transfer. */
public final class ItemTransferObservation {
    public record Kind(Item item, CompoundTag tag) {}
    public record Before(IItemHandler source, IItemHandler destination, Map<Kind, Long> from, Map<Kind, Long> to) {}
    private ItemTransferObservation() {}

    public static Before before(IItemHandler source, IItemHandler destination) {
        if (source == destination || source.getSlots() > 4096 || destination.getSlots() > 4096) return null;
        return new Before(source, destination, snapshot(source), snapshot(destination));
    }

    public static boolean committed(Before before) {
        if (before == null || before.source.getSlots() > 4096 || before.destination.getSlots() > 4096) return false;
        var from = snapshot(before.source);
        var to = snapshot(before.destination);
        var kinds = new HashSet<>(before.from.keySet()); kinds.addAll(before.to.keySet());
        kinds.addAll(from.keySet()); kinds.addAll(to.keySet());
        boolean moved = false;
        for (Kind kind : kinds) {
            long debit = before.from.getOrDefault(kind, 0L) - from.getOrDefault(kind, 0L);
            long credit = to.getOrDefault(kind, 0L) - before.to.getOrDefault(kind, 0L);
            if (debit < 0 || debit != credit) return false;
            moved |= debit > 0;
        }
        return moved;
    }

    private static Map<Kind, Long> snapshot(IItemHandler inventory) {
        Map<Kind, Long> counts = new HashMap<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            Kind kind = new Kind(stack.getItem(), stack.hasTag() ? stack.getTag().copy() : new CompoundTag());
            counts.merge(kind, (long) stack.getCount(), Long::sum);
        }
        return counts;
    }
}
