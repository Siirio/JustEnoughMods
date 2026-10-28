package com.justenoughmods.achievementguide.integration.events;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Recipient observation of the existing authenticated, durable consent escrow transaction. */
public final class HolidayGiftProof {
    private static final class Call {
        final ServerPlayer player;
        final UUID id;
        ItemStack offered = ItemStack.EMPTY;
        int before;
        boolean removed;
        Call(ServerPlayer player, UUID id) { this.player = player; this.id = id; }
    }
    private static final ThreadLocal<Deque<Call>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);
    public static void begin(ServerPlayer player, UUID id) {
        if (CALLS.get().size() >= 16) CALLS.get().clear();
        CALLS.get().push(new Call(player, id));
    }
    public static void read(UUID id, Optional<CompoundTag> gift) {
        Call call = CALLS.get().peek();
        if (call == null || !java.util.Objects.equals(call.id, id) || HomeInteractionProof.actor(call.player) == null || gift.isEmpty()) return;
        CompoundTag entry = gift.get();
        if (!entry.hasUUID("Id") || !entry.getUUID("Id").equals(id) || !entry.hasUUID("Sender") || !entry.hasUUID("Target")
                || entry.getUUID("Sender").equals(entry.getUUID("Target"))) return;
        ItemStack stack = ItemStack.of(entry.getCompound("Item"));
        if (!stack.isEmpty()) { call.offered = stack.copy(); call.before = count(call.player, stack); }
    }
    public static void removed(UUID id) {
        Call call = CALLS.get().peek();
        if (call != null && java.util.Objects.equals(call.id, id)) call.removed = true;
    }
    public static void end(ServerPlayer player, UUID id, boolean accepted) {
        Call call = CALLS.get().poll();
        if (accepted && call != null && call.player == player && java.util.Objects.equals(call.id, id) && call.removed
                && !call.offered.isEmpty() && HomeInteractionProof.actor(player) != null
                && count(player, call.offered) - call.before == call.offered.getCount())
            HomeInteractionProof.emit(player, "v3_qol_seasonal_gift");
    }
    private static int count(ServerPlayer player, ItemStack variant) {
        int total = 0;
        for (int slot = 0; slot < Math.min(256, player.getInventory().getContainerSize()); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameTags(item, variant)) total += item.getCount();
        }
        return total;
    }
    private HolidayGiftProof() {}
}
