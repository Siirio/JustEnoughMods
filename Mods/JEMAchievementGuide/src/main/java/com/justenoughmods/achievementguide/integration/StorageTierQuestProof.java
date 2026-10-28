package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

/** One native paid tier conversion, observed only after its final returned success. */
public final class StorageTierQuestProof {
    public record Before(ServerPlayer actor, BlockPos position, BlockState state, ItemStack payment, int paidCount,
                         List<ItemStack> stored, List<ItemStack> upgrades, int baseLimit) {}
    private StorageTierQuestProof() {}

    public static Before before(ItemStack payment, Level level, BlockPos position, BlockState state, Player player) {
        if (!(player instanceof ServerPlayer actor) || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || actor.level() != level || payment.isEmpty() || !(payment.getItem() instanceof StorageTierUpgradeItem)
                || actor.getMainHandItem() != payment && actor.getOffhandItem() != payment
                || !(level.getBlockEntity(position) instanceof StorageBlockEntity storage) || storage.isLinkedStorage()
                || state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) return null;
        var inventory = storage.getStorageWrapper().getInventoryHandler();
        var upgrades = storage.getStorageWrapper().getUpgradeHandler();
        if (inventory.getSlots() > 4096 || upgrades.getSlots() > 128) return null;
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) stacks.add(inventory.getSlotStack(slot).copy());
        return new Before(actor, position.immutable(), state, payment, payment.getCount(), List.copyOf(stacks), stacks(upgrades), inventory.getBaseSlotLimit());
    }

    public static void after(ItemStack payment, Level level, Player player, InteractionResult result, Before before) {
        if (before == null || before.actor != player || before.payment != payment || result != InteractionResult.SUCCESS
                || before.paidCount - payment.getCount() != 1 || before.actor.level() != level
                || level.getBlockState(before.position).equals(before.state)
                || !(level.getBlockEntity(before.position) instanceof StorageBlockEntity storage) || storage.isLinkedStorage()) return;
        var inventory = storage.getStorageWrapper().getInventoryHandler();
        var upgrades = storage.getStorageWrapper().getUpgradeHandler();
        if (inventory.getSlots() > 4096 || upgrades.getSlots() > 128 || inventory.getSlots() < before.stored.size()
                || upgrades.getSlots() < before.upgrades.size()) return;
        boolean increased = inventory.getSlots() > before.stored.size() || upgrades.getSlots() > before.upgrades.size()
                || inventory.getBaseSlotLimit() > before.baseLimit;
        if (!increased) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack expected = slot < before.stored.size() ? before.stored.get(slot) : ItemStack.EMPTY;
            if (!ItemStack.matches(expected, inventory.getSlotStack(slot))) return;
        }
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            ItemStack expected = slot < before.upgrades.size() ? before.upgrades.get(slot) : ItemStack.EMPTY;
            if (!ItemStack.matches(expected, upgrades.getStackInSlot(slot))) return;
        }
        QuestActionTrigger.INSTANCE.emit(before.actor, "v3_store_fixed_upgrades");
    }
    private static List<ItemStack> stacks(IItemHandler inventory) {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) result.add(inventory.getStackInSlot(slot).copy());
        return List.copyOf(result);
    }
}
