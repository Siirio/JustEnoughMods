package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;

/** Native stack-upgrade installation followed by a conserved insertion beyond the former limit. */
public final class BackpackCapacityProof {
    private static final String KEY = "jemQuestStackCapacityV1";
    public record Probe(int slot, int oldLimit, ItemStack item, int held) {}
    public record Before(UUID backpack, int baseLimit, List<ItemStack> inventory, Map<Item, Integer> upgrades,
                         Map<Item, Integer> heldUpgrades, List<Probe> probes) {}
    private BackpackCapacityProof() {}

    public static Before before(BackpackContainer menu, ServerPlayer actor) {
        if (!eligible(actor) || !menu.stillValid(actor)) return null;
        var wrapper = menu.getStorageWrapper();
        var identity = wrapper.getContentsUuid();
        var inventory = wrapper.getInventoryHandler();
        if (identity.isEmpty() || inventory.getSlots() > 4096 || wrapper.getUpgradeHandler().getSlotWrappers().size() > 128) return null;
        List<ItemStack> stacks = new ArrayList<>();
        List<Probe> probes = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            stacks.add(stack.copy());
            int limit = inventory.getStackLimit(slot, stack);
            if (!stack.isEmpty() && stack.getMaxStackSize() == 64 && limit > 0 && limit < Integer.MAX_VALUE && stack.getCount() <= limit)
                probes.add(new Probe(slot, limit, stack.copy(), FoodOutputProof.held(actor, menu, stack)));
        }
        Map<Item, Integer> upgrades = new HashMap<>();
        for (IUpgradeWrapper upgrade : wrapper.getUpgradeHandler().getSlotWrappers().values()) add(upgrades, upgrade.getUpgradeStack());
        Map<Item, Integer> held = new HashMap<>();
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) add(held, actor.getInventory().getItem(slot));
        add(held, menu.getCarried());
        return new Before(identity.get(), inventory.getBaseSlotLimit(), List.copyOf(stacks), Map.copyOf(upgrades), Map.copyOf(held), List.copyOf(probes));
    }

    public static void after(BackpackContainer menu, ServerPlayer actor, Before before) {
        if (before == null || actor.containerMenu != menu || !eligible(actor) || !menu.stillValid(actor)) return;
        var wrapper = menu.getStorageWrapper();
        if (!wrapper.getContentsUuid().filter(before.backpack::equals).isPresent()) return;
        var inventory = wrapper.getInventoryHandler();
        if (inventory.getSlots() != before.inventory.size()) return;
        var upgrades = wrapper.getUpgradeHandler().getSlotWrappers().values();
        for (IUpgradeWrapper upgrade : upgrades) {
            ItemStack installed = upgrade.getUpgradeStack();
            if (!upgrade.isEnabled() || !normal(installed) || !installed.hasTag()) continue;
            CompoundTag proof = installed.getTag().getCompound(KEY);
            if (!proof.hasUUID("actor") || !proof.getUUID("actor").equals(actor.getUUID()) || !proof.hasUUID("backpack")
                    || !proof.getUUID("backpack").equals(before.backpack)) continue;
            int slot = proof.getInt("slot");
            int limit = proof.getInt("oldLimit");
            if (slot < 0 || slot >= inventory.getSlots() || limit < 1 || limit == Integer.MAX_VALUE) continue;
            ItemStack kind = ItemStack.of(proof.getCompound("item"));
            ItemStack prior = before.inventory.get(slot);
            ItemStack current = inventory.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameTags(kind, prior) || !ItemStack.isSameItemSameTags(kind, current)
                    || prior.getCount() > limit || current.getCount() <= limit || inventory.getStackLimit(slot, current) <= limit) continue;
            Probe probe = before.probes.stream().filter(row -> row.slot == slot && ItemStack.isSameItemSameTags(row.item, kind)).findFirst().orElse(null);
            int received = current.getCount() - prior.getCount();
            if (probe != null && received > 0 && probe.held - FoodOutputProof.held(actor, menu, current) == received) {
                installed.getTag().remove(KEY);
                wrapper.getUpgradeHandler().saveInventory();
                QuestActionTrigger.INSTANCE.emit(actor, "storage_stack_capacity");
            }
        }
        if (inventory.getBaseSlotLimit() <= before.baseLimit || before.probes.isEmpty()) return;
        // Installing the upgrade must preserve every stored item and its count.
        for (int slot = 0; slot < inventory.getSlots(); slot++) if (!ItemStack.matches(before.inventory.get(slot), inventory.getStackInSlot(slot))) return;
        Map<Item, Integer> currentUpgrades = new HashMap<>();
        for (IUpgradeWrapper upgrade : upgrades) add(currentUpgrades, upgrade.getUpgradeStack());
        for (IUpgradeWrapper upgrade : upgrades) {
            ItemStack installed = upgrade.getUpgradeStack();
            if (!upgrade.isEnabled() || !normal(installed) || currentUpgrades.getOrDefault(installed.getItem(), 0) <= before.upgrades.getOrDefault(installed.getItem(), 0)) continue;
            int held = 0;
            for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
                ItemStack stack = actor.getInventory().getItem(slot);
                if (stack.is(installed.getItem())) held += stack.getCount();
            }
            if (menu.getCarried().is(installed.getItem())) held += menu.getCarried().getCount();
            if (before.heldUpgrades.getOrDefault(installed.getItem(), 0) - held != 1) continue;
            Probe probe = before.probes.stream().filter(row -> inventory.getStackLimit(row.slot, row.item) > row.oldLimit).findFirst().orElse(null);
            if (probe == null) continue;
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", actor.getUUID());
            proof.putUUID("backpack", before.backpack);
            proof.putInt("slot", probe.slot);
            proof.putInt("oldLimit", probe.oldLimit);
            proof.put("item", probe.item.copyWithCount(1).save(new CompoundTag()));
            installed.getOrCreateTag().put(KEY, proof);
            wrapper.getUpgradeHandler().saveInventory();
            break;
        }
    }

    private static boolean normal(ItemStack stack) {
        if (!(stack.getItem() instanceof StackUpgradeItem)) return false;
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        return id.equals("sophisticatedbackpacks:stack_upgrade_starter_tier") || id.matches("sophisticatedbackpacks:stack_upgrade_tier_[1-4]");
    }
    private static void add(Map<Item, Integer> counts, ItemStack stack) { if (normal(stack)) counts.merge(stack.getItem(), stack.getCount(), Integer::sum); }
    private static boolean eligible(ServerPlayer actor) { return !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild; }
}
