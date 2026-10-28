package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.mixin.TomsTerminalPlayerQuestAccess;
import com.tom.storagemod.gui.StorageTerminalMenu;
import com.tom.storagemod.util.StoredItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.IItemHandler;

/** A real manual terminal extraction whose sole native connection is the cable-master proxy. */
public final class TomsCableTransferProof {
    public static final class Before {
        final ServerPlayer actor;
        final IItemHandler network;
        final ItemStack item;
        final long stock;
        final int held;
        long pulled;
        Before(ServerPlayer actor, IItemHandler network, ItemStack item, long stock, int held) {
            this.actor = actor; this.network = network; this.item = item; this.stock = stock; this.held = held;
        }
    }
    private TomsCableTransferProof() {}

    public static Before before(StorageTerminalMenu menu, StoredItemStack clicked) {
        if (!(((TomsTerminalPlayerQuestAccess) menu).jem$nativeTerminalPlayerInventory().player instanceof ServerPlayer actor)
                || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || actor.containerMenu != menu || !menu.stillValid(actor) || clicked == null || menu.getTerminal() == null
                || menu.getTerminal().getLevel() != actor.level() || !menu.getTerminal().canInteractWith(actor)) return null;
        IItemHandler network = ((StorageNetworkAccess) menu.getTerminal()).jem$questStorageNetwork();
        if (network == null || network.getSlots() < 1 || network.getSlots() > 4096) return null;
        ItemStack item = clicked.getStack().copyWithCount(1);
        return item.isEmpty() ? null : new Before(actor, network, item, count(network, item), FoodOutputProof.held(actor, menu, item));
    }

    public static void pulled(Before before, StoredItemStack result) {
        if (before == null || result == null || !ItemStack.isSameItemSameTags(before.item, result.getStack()) || result.getQuantity() < 1) return;
        before.pulled += result.getQuantity();
    }

    public static void after(StorageTerminalMenu menu, Before before) {
        if (before == null || before.pulled < 1 || before.actor.containerMenu != menu || !menu.stillValid(before.actor)
                || !menu.getTerminal().canInteractWith(before.actor)
                || ((StorageNetworkAccess) menu.getTerminal()).jem$questStorageNetwork() != before.network
                || before.network.getSlots() > 4096) return;
        long debit = before.stock - count(before.network, before.item);
        long received = (long) FoodOutputProof.held(before.actor, menu, before.item) - before.held;
        if (debit == before.pulled && received == debit) {
            if (before.network instanceof NativeCableStorage) QuestActionTrigger.INSTANCE.emit(before.actor, "v3_store_cables");
            MinecraftForge.EVENT_BUS.post(new NativeTerminalExtractionEvent(before.actor, before.network, before.item, debit, before.stock));
        }
    }

    private static long count(IItemHandler network, ItemStack item) {
        long count = 0;
        for (int slot = 0; slot < network.getSlots(); slot++) {
            ItemStack stack = network.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, item)) count += stack.getCount();
        }
        return count;
    }
}
