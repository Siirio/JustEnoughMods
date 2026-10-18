package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.backpacked.Config;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.augment.SavedAugments;
import com.mrcrayfish.backpacked.item.BackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Comparator;
import java.util.List;

public final class AugmentBayMenu extends AbstractContainerMenu {
    public static final String WORKBENCH_PURCHASED = "JemWorkbenchPurchased";
    public static final int LAPIS_COST = 4;
    private final SimpleContainer ingredients = new SimpleContainer(3);
    private final SimpleContainerData selection = new SimpleContainerData(2);
    private final Inventory inventory;
    private final BlockPos position;
    private final List<AugmentType<?>> types;

    public AugmentBayMenu(int id, Inventory inventory, BlockPos position) {
        super(BackpackContent.AUGMENT_BAY_MENU.get(), id);
        this.inventory = inventory;
        this.position = position.immutable();
        this.types = AugmentType.stream().filter(type -> !type.isEmpty() && !Config.getDisabledAugments().contains(type.id()))
                .sorted(Comparator.comparing(type -> type.id().toString())).<AugmentType<?>>map(type -> type).toList();
        addSlot(new Slot(ingredients, 0, 35, 39) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BackpackItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(ingredients, 1, 80, 39) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.LAPIS_LAZULI); }
        });
        addSlot(new Slot(ingredients, 2, 125, 39) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.CRAFTING_TABLE); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 135 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 193));
        addDataSlots(selection);
    }

    public AugmentType<?> selectedType() { return types.isEmpty() ? null : types.get(Math.floorMod(selection.get(0), types.size())); }
    public int selectedBay() { return selection.get(1); }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || player != inventory.player || !stillValid(player)) return false;
        if (button == 0 || button == 1) selection.set(0, types.isEmpty() ? 0 : Math.floorMod(selection.get(0) + (button == 0 ? -1 : 1), types.size()));
        else if (button == 2 || button == 3) selection.set(1, Math.floorMod(selection.get(1) + (button == 2 ? -1 : 1), 4));
        else if (button == 4) install(player);
        else return false;
        broadcastChanges();
        return true;
    }

    private void install(Player player) {
        ItemStack stack = ingredients.getItem(0);
        AugmentType<?> type = selectedType();
        if (!(stack.getItem() instanceof BackpackItem item) || type == null || Config.getDisabledAugments().contains(type.id())) { fail(player, "backpack_required"); return; }
        var bay = Augments.Position.values()[selectedBay()];
        if (bay.ordinal() >= item.getMaxAugmentBays(stack) || !item.getUnlockableAugmentBays(stack).isUnlocked(bay.ordinal())) { fail(player, "bay_locked"); return; }
        var current = Augments.cached(stack);
        if (current.findPosition(type).isPresent()) { fail(player, "already_installed"); return; }
        var required = type.requires().get();
        if (required != null && current.findPosition(required).isEmpty()) { fail(player, "requires_augment"); return; }
        var old = current.getAugment(bay);
        for (var other : Augments.Position.values()) if (other != bay && current.getAugment(other).type().requires().get() == old.type()) {
            fail(player, "required_by_other"); return;
        }
        boolean buyWorkbench = type == WorkbenchAugment.TYPE && !stack.getOrCreateTag().getBoolean(WORKBENCH_PURCHASED);
        if (!ingredients.getItem(1).is(Items.LAPIS_LAZULI) || ingredients.getItem(1).getCount() < LAPIS_COST
                || (buyWorkbench && !ingredients.getItem(2).is(Items.CRAFTING_TABLE))) { fail(player, "missing_resources"); return; }
        var saved = SavedAugments.get(stack);
        var updated = saved.getSavedOrCreateDefault(type);
        ingredients.removeItem(1, LAPIS_COST);
        if (buyWorkbench) { ingredients.removeItem(2, 1); stack.getOrCreateTag().putBoolean(WORKBENCH_PURCHASED, true); }
        saved.add(old);
        saved.remove(updated);
        current.setAugment(bay, updated);
        current.setState(bay, true);
        ingredients.setChanged();
        player.displayClientMessage(Component.translatable("message.jemcompat.augment_bay.installed", type.name()), true);
    }

    private void fail(Player player, String reason) { player.displayClientMessage(Component.translatable("message.jemcompat.augment_bay." + reason), true); }

    @Override
    public boolean stillValid(Player player) {
        return player == inventory.player && stillValid(ContainerLevelAccess.create(player.level(), position), player, BackpackContent.AUGMENT_BAY.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3 ? !moveItemStackTo(stack, 3, slots.size(), true) : !moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, ingredients);
    }
}
