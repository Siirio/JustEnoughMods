package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.mixin.CompressionDefinitionQuestAccess;
import com.justenoughmods.achievementguide.mixin.CompressionPartQuestAccess;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionInventoryPart;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionUpgradeItem;

/** Material conservation using the installed native compression chain, never a parallel recipe table. */
public final class StorageCompressionProof {
    public record Slot(int index, ItemStack item, int previousMultiplier, BigInteger weight) {}
    public record Chain(CompressionInventoryPart part, List<Slot> slots, List<ItemStack> physical, BigInteger stored, BigInteger held) {}
    public record Before(InventoryHandler inventory, List<Chain> chains) {}
    private StorageCompressionProof() {}

    public static Before before(StorageContainerMenu menu, ServerPlayer actor) {
        if (!eligible(menu, actor)) return null;
        var inventory = menu.getStorageWrapper().getInventoryHandler();
        if (inventory.getSlots() > 16) return null;
        Set<CompressionInventoryPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Chain> chains = new ArrayList<>();
        for (int index = 0; index < inventory.getSlots(); index++) {
            if (!(inventory.getInventoryPartitioner().getPartBySlot(index) instanceof CompressionInventoryPart part) || !seen.add(part)) continue;
            List<Slot> slots = chain(part, inventory);
            if (slots.isEmpty()) continue;
            List<ItemStack> physical = physical(inventory, slots);
            BigInteger stored = quantity(physical, slots);
            if (stored != null) chains.add(new Chain(part, slots, physical, stored, held(actor, menu, slots)));
        }
        return new Before(inventory, List.copyOf(chains));
    }

    public static void after(StorageContainerMenu menu, ServerPlayer actor, Before before) {
        if (before == null || actor.containerMenu != menu || !eligible(menu, actor)
                || menu.getStorageWrapper().getInventoryHandler() != before.inventory) return;
        for (Chain previous : before.chains) {
            List<Slot> slots = chain(previous.part, before.inventory);
            if (!same(previous.slots, slots)) continue;
            BigInteger stored = quantity(physical(before.inventory, slots), slots);
            if (stored == null) continue;
            BigInteger delta = stored.subtract(previous.stored);
            if (delta.signum() != 0 && held(actor, menu, slots).subtract(previous.held).equals(delta.negate())) {
                QuestActionTrigger.INSTANCE.emit(actor, "v3_store_compression");
                return;
            }
        }
    }

    private static List<Slot> chain(CompressionInventoryPart part, InventoryHandler inventory) {
        var access = (CompressionPartQuestAccess) part;
        var range = access.jem$nativeCompressionRange();
        if (range.numberOfSlots() < 2 || range.numberOfSlots() > 16 || range.firstSlot() < 0
                || range.firstSlot() + range.numberOfSlots() > inventory.getSlots()) return List.of();
        var definitions = access.jem$nativeCompressionDefinitions();
        List<Slot> reverse = new ArrayList<>();
        BigInteger weight = BigInteger.ONE;
        int expected = -1;
        for (int index = range.firstSlot() + range.numberOfSlots() - 1; index >= range.firstSlot(); index--) {
            if (inventory.getInventoryPartitioner().getPartBySlot(index) != part) return List.of();
            Object definition = definitions.get(index);
            if (!(definition instanceof CompressionDefinitionQuestAccess slot) || !slot.jem$nativeCompressionAccessible()
                    || !slot.jem$nativeCompressionCompressible()) continue;
            ItemStack item = slot.jem$nativeCompressionItem();
            int multiplier = slot.jem$nativeCompressionMultiplier();
            if (item.isEmpty() || expected >= 0 && expected != index || multiplier < 1
                    || reverse.stream().anyMatch(row -> ItemStack.isSameItemSameTags(row.item, item))) return List.of();
            reverse.add(new Slot(index, item.copyWithCount(1), multiplier, weight));
            weight = weight.multiply(BigInteger.valueOf(multiplier));
            expected = index - 1;
        }
        if (reverse.size() < 2) return List.of();
        Collections.reverse(reverse);
        for (int index = 1; index < reverse.size(); index++) if (reverse.get(index).previousMultiplier < 2) return List.of();
        return List.copyOf(reverse);
    }

    private static List<ItemStack> physical(InventoryHandler inventory, List<Slot> slots) {
        return slots.stream().map(slot -> inventory.getSlotStack(slot.index).copy()).toList();
    }
    private static BigInteger quantity(List<ItemStack> physical, List<Slot> slots) {
        BigInteger total = BigInteger.ZERO;
        for (int index = 0; index < slots.size(); index++) {
            ItemStack stack = physical.get(index);
            if (stack.isEmpty()) continue;
            Slot slot = slots.get(index);
            if (!ItemStack.isSameItemSameTags(stack, slot.item)) return null;
            total = total.add(slot.weight.multiply(BigInteger.valueOf(stack.getCount())));
        }
        return total;
    }
    private static BigInteger held(ServerPlayer actor, StorageContainerMenu menu, List<Slot> slots) {
        BigInteger total = BigInteger.ZERO;
        for (Slot slot : slots) total = total.add(slot.weight.multiply(BigInteger.valueOf(FoodOutputProof.held(actor, menu, slot.item))));
        return total;
    }
    private static boolean same(List<Slot> before, List<Slot> after) {
        if (before.size() != after.size()) return false;
        for (int index = 0; index < before.size(); index++) {
            Slot a = before.get(index), b = after.get(index);
            if (a.index != b.index || a.previousMultiplier != b.previousMultiplier || !a.weight.equals(b.weight)
                    || !ItemStack.isSameItemSameTags(a.item, b.item)) return false;
        }
        return true;
    }
    private static boolean eligible(StorageContainerMenu menu, ServerPlayer actor) {
        return !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild && menu.stillValid(actor)
                && menu.getStorageBlockEntity() instanceof LimitedBarrelBlockEntity barrel && barrel.getLevel() == actor.level()
                && menu.getStorageWrapper().getUpgradeHandler().getSlotWrappers().values().stream()
                .anyMatch(upgrade -> upgrade.isEnabled() && upgrade.getUpgradeStack().getItem() instanceof CompressionUpgradeItem);
    }
}
