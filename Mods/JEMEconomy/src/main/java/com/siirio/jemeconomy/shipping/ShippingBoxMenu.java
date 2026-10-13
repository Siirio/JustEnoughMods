package com.siirio.jemeconomy.shipping;

import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemeconomy.registry.EconomyMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ShippingBoxMenu extends AbstractContainerMenu {
    private final ShippingBoxBlockEntity box;

    public ShippingBoxMenu(int id, Inventory inventory, ShippingBoxBlockEntity box) {
        super(EconomyMenus.SHIPPING_BOX.get(), id); this.box = box;
        addDataSlots(box.data());
        for (int row = 0; row < 6; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(box, column + row * 9, 8 + column * 18, 18 + row * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return ItemPolicyRegistry.acceptsShipping(stack); }
        });
        for (int column = 0; column < 9; column++) addSlot(new Slot(box, ShippingBoxBlockEntity.PAYOUT_START + column, 8 + column * 18, 132) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 166 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 224));
    }

    public static ShippingBoxMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        var entity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (!(entity instanceof ShippingBoxBlockEntity box)) throw new IllegalStateException("Shipping Box block entity is unavailable");
        return new ShippingBoxMenu(id, inventory, box);
    }

    public ShippingBoxBlockEntity box() { return box; }
    public int statusCode() { return box.data().get(0); }
    public int previewPayout() { return box.data().get(1); }
    public int accountRemaining() { return box.data().get(2); }
    public int globalRemaining() { return box.data().get(3); }
    public int ticksUntilUpdate() { return box.data().get(4); }
    public String primaryDemand() { return group(box.data().get(5)); }
    public String secondaryDemand() { return group(box.data().get(6)); }
    private static String group(int index) { return switch (index) { case 0 -> "crops"; case 1 -> "fishing"; case 2 -> "cooking"; case 3 -> "animal_products"; case 4 -> "mining"; case 5 -> "forestry"; default -> "handcrafts"; }; }
    @Override public boolean stillValid(Player player) { return box.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || !box.isOwner(player)) return false;
        if (id == 0) { box.preview(); return true; }
        if (id == 1) { box.collect(serverPlayer); return true; }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem(), copy = original.copy();
        if (index < ShippingBoxBlockEntity.PAYOUT_START) {
            if (!moveItemStackTo(original, ShippingBoxBlockEntity.TOTAL_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index < ShippingBoxBlockEntity.TOTAL_SLOTS) {
            if (!moveItemStackTo(original, ShippingBoxBlockEntity.TOTAL_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!ItemPolicyRegistry.acceptsShipping(original) || !moveItemStackTo(original, 0, ShippingBoxBlockEntity.INPUT_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
