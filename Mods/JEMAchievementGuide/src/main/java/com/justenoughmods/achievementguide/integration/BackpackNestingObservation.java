package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IInventoryWrapperUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.inception.InceptionUpgradeWrapper;

/** Native accepted manual nesting, with actual input debit and native capacity/recursion validation. */
public final class BackpackNestingObservation {
    public record Before(List<ItemStack> slots, Map<Item, Long> nested, Map<Item, Long> player) {}
    private BackpackNestingObservation() {}

    public static Before before(BackpackContainer menu, ServerPlayer player) {
        if (player instanceof FakePlayer || player.isSpectator() || player.getAbilities().instabuild || !menu.stillValid(player)) return null;
        var wrapper = menu.getStorageWrapper();
        var inventory = wrapper.getInventoryHandler();
        if (inventory.getSlots() > 4096 || wrapper.getUpgradeHandler().getWrappersThatImplement(IInventoryWrapperUpgrade.class)
                .stream().noneMatch(upgrade -> upgrade instanceof InceptionUpgradeWrapper inception && inception.isEnabled())) return null;
        List<ItemStack> slots = new ArrayList<>(inventory.getSlots());
        Map<Item, Long> nested = new HashMap<>();
        for (int index = 0; index < inventory.getSlots(); index++) {
            ItemStack stack = inventory.getStackInSlot(index);
            slots.add(stack.copy()); add(nested, stack);
        }
        return new Before(List.copyOf(slots), Map.copyOf(nested), player(player, menu));
    }

    public static void after(BackpackContainer menu, ServerPlayer actor, Before before) {
        if (before == null || actor.containerMenu != menu || !menu.stillValid(actor)) return;
        var inventory = menu.getStorageWrapper().getInventoryHandler();
        if (inventory.getSlots() != before.slots.size()) return;
        Map<Item, Long> nested = new HashMap<>();
        for (int index = 0; index < inventory.getSlots(); index++) add(nested, inventory.getStackInSlot(index));
        Map<Item, Long> player = player(actor, menu);
        for (int index = 0; index < inventory.getSlots(); index++) {
            ItemStack stack = inventory.getStackInSlot(index);
            if (!(stack.getItem() instanceof BackpackItem) || ItemStack.matches(stack, before.slots.get(index))
                    || !inventory.isItemValid(index, stack, actor)) continue;
            long received = nested.getOrDefault(stack.getItem(), 0L) - before.nested.getOrDefault(stack.getItem(), 0L);
            long debited = before.player.getOrDefault(stack.getItem(), 0L) - player.getOrDefault(stack.getItem(), 0L);
            if (received > 0 && received == debited) {
                QuestActionTrigger.INSTANCE.emit(actor, "v3_store_nested");
                return;
            }
        }
    }

    private static Map<Item, Long> player(ServerPlayer player, BackpackContainer menu) {
        Map<Item, Long> counts = new HashMap<>();
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) add(counts, player.getInventory().getItem(index));
        add(counts, menu.getCarried());
        return counts;
    }
    private static void add(Map<Item, Long> counts, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof BackpackItem) counts.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
    }
}
