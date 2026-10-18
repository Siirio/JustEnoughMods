package com.siirio.jemcompat.feature.transport;

import immersive_aircraft.screen.slot.FuelSlot;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class AircraftFuelSlot extends FuelSlot {
    private final AircraftFuelInventory inventory;

    public AircraftFuelSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.inventory = (AircraftFuelInventory) inventory;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return super.mayPlace(stack) && inventory.canPlaceItem(getContainerSlot(), stack);
    }

    @Override
    public int getMaxStackSize() {
        if (!inventory.policyEnabled()) return super.getMaxStackSize();
        return getMaxStackSize(getItem());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (!inventory.policyEnabled()) return Math.min(super.getMaxStackSize(), stack.getMaxStackSize());
        if (!inventory.grounded()) {
            inventory.warnAirborne();
            return getItem().getCount();
        }
        return Math.min(AircraftPolicy.CAPACITY, getItem().getCount() + inventory.freeUnits());
    }
}
