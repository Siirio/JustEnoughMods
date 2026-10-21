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

public final class ShippingBoxMenu extends AbstractContainerMenu implements com.siirio.jemeconomy.api.CurrencyContainerAccess {
    private final ShippingBoxBlockEntity box;
    private final net.minecraft.world.inventory.ContainerData values;
    private int tab;

    public ShippingBoxMenu(int id, Inventory inventory, ShippingBoxBlockEntity box) {
        this(id, inventory, box, box, box.data());
    }

    private ShippingBoxMenu(int id, Inventory inventory, ShippingBoxBlockEntity box,
                            net.minecraft.world.Container contents, net.minecraft.world.inventory.ContainerData values) {
        super(EconomyMenus.SHIPPING_BOX.get(), id); this.box = box; this.values = values;
        addDataSlots(values);
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return tab; }
            @Override public void set(int value) { tab = value == 1 ? 1 : 0; }
        });
        for (int row = 0; row < 6; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(contents, column + row * 9, ShippingBoxLayout.INPUT_X + column * 18, ShippingBoxLayout.INPUT_Y + row * 18) {
            @Override public boolean isActive() { return tab == 0; }
            @Override public boolean mayPickup(Player player) { return tab == 0 && inventoryUnlocked() && stillValid(player); }
            @Override public boolean mayPlace(ItemStack stack) { return tab == 0 && inventoryUnlocked() && (box == null || ItemPolicyRegistry.acceptsShipping(stack)); }
        });
        for (int column = 0; column < 9; column++) addSlot(new Slot(contents, ShippingBoxBlockEntity.PAYOUT_START + column, ShippingBoxLayout.INPUT_X + column * 18, ShippingBoxLayout.PAYOUT_Y) {
            @Override public boolean isActive() { return tab == 0; }
            @Override public boolean mayPickup(Player player) { return tab == 0 && inventoryUnlocked() && stillValid(player); }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, ShippingBoxLayout.INPUT_X + column * 18, ShippingBoxLayout.PLAYER_Y + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, ShippingBoxLayout.INPUT_X + column * 18, ShippingBoxLayout.HOTBAR_Y));
    }

    public static ShippingBoxMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        buffer.readBlockPos();
        return new ShippingBoxMenu(id, inventory, null, new net.minecraft.world.SimpleContainer(ShippingBoxBlockEntity.TOTAL_SLOTS),
                new net.minecraft.world.inventory.SimpleContainerData(32));
    }

    public ShippingBoxBlockEntity box() { return box; }
    public boolean hasPayment() {
        for (int slot = ShippingBoxBlockEntity.PAYOUT_START; slot < ShippingBoxBlockEntity.TOTAL_SLOTS; slot++)
            if (slots.get(slot).hasItem()) return true;
        return false;
    }
    public boolean paymentLocked() { return hasPayment() || !inventoryUnlocked(); }
    @Override public void removed(Player player) { super.removed(player); if (box != null && player instanceof ServerPlayer serverPlayer) box.release(serverPlayer); }
    private boolean inventoryUnlocked() { return box != null ? box.inventoryUnlocked() : value(7) == 0 || value(7) == 5; }
    private long value(int field) {
        long result = 0L;
        for (int word = 0; word < 4; word++) result |= ((long) values.get(field * 4 + word) & 0xffffL) << (word * 16);
        return result;
    }
    @Override public void broadcastChanges() { if (box != null) box.refresh(); super.broadcastChanges(); }
    public int tab() { return tab; }
    public void clientTab(int value) { tab = value == 1 ? 1 : 0; }
    @Override public boolean permitsCurrencyAction(ServerPlayer player) { return tab == 1 && player.containerMenu == this && stillValid(player); }
    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.isActive() && super.canTakeItemForPickAll(stack, slot); }
    @Override public void clicked(int slot, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if ((tab == 1 || !inventoryUnlocked()) && slot >= 0 && slot < ShippingBoxBlockEntity.TOTAL_SLOTS) return;
        super.clicked(slot, button, type, player);
    }
    public int statusCode() { return (int) value(0); }
    public long previewPayout() { return value(1); }
    public long accountRemaining() { return value(2); }
    public long globalRemaining() { return value(3); }
    public int ticksUntilUpdate() { return (int) value(4); }
    public String primaryDemand() { return group((int) value(5)); }
    public String secondaryDemand() { return group((int) value(6)); }
    private static String group(int index) { return switch (index) { case 0 -> "crops"; case 1 -> "fishing"; case 2 -> "cooking"; case 3 -> "animal_products"; case 4 -> "mining"; case 5 -> "forestry"; default -> "handcrafts"; }; }
    @Override public boolean stillValid(Player player) { return box == null || box.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.containerMenu != this || !stillValid(player)) return false;
        if (id == 2 || id == 3) { tab = id - 2; resetQuickCraft(); broadcastChanges(); return true; }
        if (id == 1 && tab == 0) { box.sellAndCollect(serverPlayer); return true; }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).isActive() || !stillValid(player) || !inventoryUnlocked()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem(), copy = original.copy();
        if (index < ShippingBoxBlockEntity.PAYOUT_START) {
            if (!moveItemStackTo(original, ShippingBoxBlockEntity.TOTAL_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index < ShippingBoxBlockEntity.TOTAL_SLOTS) {
            if (!moveItemStackTo(original, ShippingBoxBlockEntity.TOTAL_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (tab != 0) return ItemStack.EMPTY;
            if (box != null && !ItemPolicyRegistry.acceptsShipping(original) || !moveItemStackTo(original, 0, ShippingBoxBlockEntity.INPUT_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
