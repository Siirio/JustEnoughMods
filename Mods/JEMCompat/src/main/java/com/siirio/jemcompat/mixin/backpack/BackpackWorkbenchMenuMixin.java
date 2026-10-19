package com.siirio.jemcompat.mixin.backpack;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import com.siirio.jemcompat.feature.backpack.BackpackCraftingGrid;
import com.siirio.jemcompat.feature.backpack.BackpackWorkbench;
import com.siirio.jemcompat.feature.backpack.WorkbenchAugment;
import com.siirio.jemcompat.feature.transport.AircraftPolicy;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BackpackContainerMenu.class, remap = false)
public abstract class BackpackWorkbenchMenuMixin extends AbstractContainerMenu implements BackpackWorkbench {
    @Unique private BackpackCraftingGrid jemcompat$grid;
    @Unique private ResultContainer jemcompat$result;
    @Unique private Player jemcompat$player;
    @Unique private int jemcompat$originalSlots;
    @Unique private int jemcompat$resultIndex;
    @Unique private boolean jemcompat$updating;

    protected BackpackWorkbenchMenuMixin(MenuType<?> type, int id) { super(type, id); }
    @Unique private BackpackContainerMenu jemcompat$menu() { return (BackpackContainerMenu) (Object) this; }

    @Override
    public boolean jemcompat$hasWorkbench() {
        if (jemcompat$player == null || !AircraftPolicy.enabled(jemcompat$player)) return false;
        var menu = jemcompat$menu();
        var position = menu.getAugments().findPosition(WorkbenchAugment.TYPE);
        return menu.isOwner() && position.isPresent() && menu.getAugments().getState(position.get())
                && menu.getAugmentBayController().isSlotUnlocked(position.get().ordinal());
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;IIIIZLcom/mrcrayfish/backpacked/common/backpack/UnlockableSlots;Lcom/mrcrayfish/backpacked/common/Pagination;Lcom/mrcrayfish/backpacked/common/augment/Augments;Lcom/mrcrayfish/backpacked/common/backpack/UnlockableSlots;)V",
            at = @At(value = "INVOKE", target = "Lcom/mrcrayfish/backpacked/inventory/container/BackpackContainerMenu;addPlayerInventorySlots(Lnet/minecraft/world/entity/player/Inventory;II)V"), index = 2)
    private int jemcompat$insertCraftingSpace(Inventory inventory, int x, int y) {
        jemcompat$player = inventory.player;
        return jemcompat$hasWorkbench() ? y + INSERT_HEIGHT : y;
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;IIIIZLcom/mrcrayfish/backpacked/common/backpack/UnlockableSlots;Lcom/mrcrayfish/backpacked/common/Pagination;Lcom/mrcrayfish/backpacked/common/augment/Augments;Lcom/mrcrayfish/backpacked/common/backpack/UnlockableSlots;)V", at = @At("TAIL"))
    private void jemcompat$createCrafting(int id, Inventory inventory, Container backpack, int ownerId, int backpackIndex, int cols, int rows, boolean owner,
                                        com.mrcrayfish.backpacked.common.backpack.UnlockableSlots unlocked, com.mrcrayfish.backpacked.common.Pagination pagination,
                                        Augments augments, com.mrcrayfish.backpacked.common.backpack.UnlockableSlots bays, CallbackInfo callback) {
        if (!jemcompat$hasWorkbench() || !AircraftPolicy.enabled(inventory.player)) return;
        jemcompat$player = inventory.player;
        ItemStack stack = backpack instanceof BackpackInventory nativeInventory ? nativeInventory.getBackpackStack() : ItemStack.EMPTY;
        // Other owners (dock/shelf/pickpocket) have separate native lifecycles and never receive a detached grid.
        if (!inventory.player.level().isClientSide && stack.isEmpty()) return;
        jemcompat$grid = new BackpackCraftingGrid(this, stack, inventory.player);
        jemcompat$result = new ResultContainer();
        jemcompat$originalSlots = slots.size();
        int width = 22 + Math.max(cols, 9) * 18;
        int x = (width - CRAFT_WIDTH) / 2;
        int y = 44 + rows * 18;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) addSlot(new Slot(jemcompat$grid, col + row * 3, x + GRID_X + col * 18, y + GRID_Y + row * 18) {
            @Override public boolean mayPlace(ItemStack incoming) { return jemcompat$grid.canPlaceItem(getContainerSlot(), incoming); }
        });
        jemcompat$resultIndex = slots.size();
        addSlot(new ResultSlot(inventory.player, jemcompat$grid, jemcompat$result, 0, x + RESULT_X, y + RESULT_Y));
        jemcompat$updateRecipe();
    }

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        if (jemcompat$grid != null && changed == jemcompat$grid && !jemcompat$updating) {
            if (!jemcompat$player.level().isClientSide) jemcompat$grid.persist();
            jemcompat$updateRecipe();
        }
    }

    @Unique
    private void jemcompat$updateRecipe() {
        if (jemcompat$grid == null || !(jemcompat$player instanceof ServerPlayer player)) return;
        jemcompat$updating = true;
        try {
            ItemStack output = ItemStack.EMPTY;
            var recipe = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, jemcompat$grid, player.level());
            if (recipe.isPresent() && jemcompat$result.setRecipeUsed(player.level(), player, recipe.get())) {
                var candidate = recipe.get().assemble(jemcompat$grid, player.level().registryAccess());
                if (candidate.isItemEnabled(player.level().enabledFeatures())) output = candidate;
            }
            jemcompat$result.setItem(0, output);
            setRemoteSlot(jemcompat$resultIndex, output);
            player.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), jemcompat$resultIndex, output));
        } finally { jemcompat$updating = false; }
    }

    @Inject(method = "m_7648_(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$craftingTransfer(Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        if (jemcompat$grid == null) return;
        callback.setReturnValue(ItemStack.EMPTY);
        if (index < 0 || index >= slots.size()) return;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int backpackSize = jemcompat$menu().getRows() * jemcompat$menu().getCols();
        boolean moved = index < backpackSize || index >= jemcompat$originalSlots
                ? moveItemStackTo(stack, backpackSize, jemcompat$originalSlots, true)
                : moveItemStackTo(stack, 0, backpackSize, false);
        if (!moved || copy.getCount() == stack.getCount()) return;
        if (index == jemcompat$resultIndex) slot.onQuickCraft(stack, copy);
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        if (index == jemcompat$resultIndex) player.drop(stack, false);
        if (!player.level().isClientSide) jemcompat$grid.persist();
        callback.setReturnValue(copy);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != jemcompat$result && super.canTakeItemForPickAll(stack, slot);
    }

    @Inject(method = "m_6877_(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), remap = false)
    private void jemcompat$persistCrafting(Player player, CallbackInfo callback) {
        if (jemcompat$grid != null && !player.level().isClientSide) jemcompat$grid.persist();
    }
}
