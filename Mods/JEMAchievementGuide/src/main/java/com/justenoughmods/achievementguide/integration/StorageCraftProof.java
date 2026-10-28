package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.tom.storagemod.tile.CraftingTerminalBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Transient scope around one native result take, without storing a second network inventory. */
public final class StorageCraftProof {
    private static final ThreadLocal<StorageCraftProof> CURRENT = new ThreadLocal<>();
    private final CraftingTerminalBlockEntity terminal;
    private int consumed;
    private long pulled;

    private StorageCraftProof(CraftingTerminalBlockEntity terminal) { this.terminal = terminal; }

    public static void craft(CraftingTerminalBlockEntity terminal, ServerPlayer actor, ItemStack taken) {
        StorageCraftProof previous = CURRENT.get();
        StorageCraftProof run = new StorageCraftProof(terminal);
        CURRENT.set(run);
        boolean committed = false;
        try {
            terminal.craft(actor);
            committed = true;
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
        if (committed && !taken.isEmpty() && run.consumed > 0 && run.pulled > 0) {
            QuestActionTrigger.INSTANCE.emit(actor, "storage_craft");
        }
    }

    public static void consumed(CraftingTerminalBlockEntity terminal, ItemStack removed) {
        StorageCraftProof run = CURRENT.get();
        if (run != null && run.terminal == terminal && !removed.isEmpty()) run.consumed += removed.getCount();
    }

    public static void pulled(CraftingTerminalBlockEntity terminal, long count) {
        StorageCraftProof run = CURRENT.get();
        if (run != null && run.terminal == terminal && count > 0) run.pulled += count;
    }
}
