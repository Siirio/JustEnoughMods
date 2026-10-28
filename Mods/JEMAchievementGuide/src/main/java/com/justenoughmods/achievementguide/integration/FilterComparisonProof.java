package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;

/** Ephemeral observations of native operations, never a second inventory or filter owner. */
public final class FilterComparisonProof {
    public static final int SLOT_LIMIT = 4096;
    public static final class Configuration {
        public final WeakReference<ServerPlayer> actor;
        public final CompoundTag settings;
        private final long configuredAt;
        private WeakReference<Object> endpoint = new WeakReference<>(null);
        private boolean bound;
        private Item accepted;
        private Item rejected;
        public Configuration(ServerPlayer actor, CompoundTag settings) {
            this.actor = new WeakReference<>(actor);
            this.settings = settings.copy();
            configuredAt = actor.level().getGameTime();
        }
        public boolean valid(ServerPlayer player, CompoundTag current) {
            long age = player.level().getGameTime() - configuredAt;
            return real(player) && actor.get() == player && age >= 0 && age <= 12000 && settings.equals(current);
        }
        public void observed(ServerPlayer player, CompoundTag current, Object nativeEndpoint, ItemStack item, boolean permitted, String action) {
            if (item.isEmpty() || !valid(player, current)) return;
            Object previous = endpoint.get();
            if (previous == null) {
                if (bound) { accepted = null; rejected = null; }
                endpoint = new WeakReference<>(nativeEndpoint);
                bound = true;
            }
            else if (previous != nativeEndpoint) return;
            if (permitted) accepted = item.getItem(); else rejected = item.getItem();
            if (accepted != null && rejected != null && accepted != rejected) QuestActionTrigger.INSTANCE.emit(player, action);
        }
    }
    private FilterComparisonProof() {}
    public static boolean real(ServerPlayer player) {
        return !(player instanceof FakePlayer) && !player.isSpectator() && !player.getAbilities().instabuild
                && player.isAlive() && player.getServer() != null && player.getServer().isSameThread()
                && player.getServer().getPlayerList().getPlayer(player.getUUID()) == player;
    }
    public static ItemTransferObservation.Kind kind(ItemStack stack) {
        return new ItemTransferObservation.Kind(stack.getItem(), stack.hasTag() ? stack.getTag().copy() : new CompoundTag());
    }
    public static Map<ItemTransferObservation.Kind, Long> snapshot(IItemHandler inventory) {
        if (inventory == null || inventory.getSlots() < 1 || inventory.getSlots() > SLOT_LIMIT) return null;
        Map<ItemTransferObservation.Kind, Long> result = new HashMap<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) result.merge(kind(stack), (long)stack.getCount(), Long::sum);
        }
        return result;
    }
    public static long count(IItemHandler inventory, ItemStack item) {
        if (inventory == null || inventory.getSlots() < 1 || inventory.getSlots() > SLOT_LIMIT) return -1;
        long count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, item)) count += stack.getCount();
        }
        return count;
    }
}
