package com.siirio.jemcompat.feature.transport;

import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.inventory.SparseSimpleInventory;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import immersive_aircraft.util.Utils;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The native sparse inventory, with fuel-only admission rules; cargo and upgrades stay native. */
public final class AircraftFuelInventory extends SparseSimpleInventory {
    private final InventoryVehicleEntity owner;
    private final ItemStack[] accepted;
    private boolean restoring;
    private long lastWarning;

    public AircraftFuelInventory(InventoryVehicleEntity owner, int size) {
        super(size);
        this.owner = owner;
        this.accepted = new ItemStack[size];
        java.util.Arrays.fill(accepted, ItemStack.EMPTY);
    }

    public boolean fuelSlot(int index) {
        return owner.getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER).stream()
                .anyMatch(slot -> slot.index() == index);
    }

    public int storedUnits() {
        return owner.getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER).stream()
                .mapToInt(slot -> Utils.getFuelTime(getItem(slot.index())) > 0 ? getItem(slot.index()).getCount() : 0).sum();
    }

    public int freeUnits() {
        int burning = owner instanceof AircraftFuelState state ? state.jemcompat$burningUnits() : 0;
        return Math.max(0, AircraftPolicy.CAPACITY - burning - storedUnits());
    }

    public boolean grounded() {
        return !AircraftPolicy.enabled(owner) || owner.onGround();
    }

    public boolean policyEnabled() { return AircraftPolicy.enabled(owner); }

    public void warnAirborne() {
        if (owner.level().isClientSide || grounded()) return;
        long now = System.nanoTime();
        if (lastWarning != 0 && now - lastWarning < 1_000_000_000L) return;
        lastWarning = now;
        for (Player player : owner.level().players()) {
            if (player.containerMenu instanceof immersive_aircraft.screen.VehicleScreenHandler menu && menu.getVehicle() == owner) {
                player.displayClientMessage(Component.translatable("message.jemcompat.aircraft.ground_only"), true);
            }
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (!AircraftPolicy.enabled(owner) || !fuelSlot(index)) return super.canPlaceItem(index, stack);
        if (!grounded()) warnAirborne();
        return grounded() && freeUnits() > 0 && Utils.getFuelTime(stack) > 0;
    }

    @Override
    public void readNbt(ListTag tag) {
        restoring = true;
        try { super.readNbt(tag); } finally { restoring = false; }
    }

    @Override
    public void setChanged() {
        // SimpleContainer.addItem and some automation mutate a stored stack in place, without setItem.
        // Observe that native mutation seam as well as normal slot/automation admission.
        if (owner != null && accepted != null) {
            for (var slot : owner.getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER)) {
                int index = slot.index();
                ItemStack stack = getItem(index);
                ItemStack previous = accepted[index];
                int before = ItemStack.isSameItemSameTags(stack, previous) ? previous.getCount() : 0;
                int addition = stack.getCount() - before;
                if (!restoring && !owner.level().isClientSide && AircraftPolicy.enabled(owner) && addition > 0) {
                    int burning = owner instanceof AircraftFuelState state ? state.jemcompat$burningUnits() : 0;
                    int space = Math.max(0, AircraftPolicy.CAPACITY - burning - storedUnits() + addition);
                    int allowed = grounded() && Utils.getFuelTime(stack) > 0 ? Math.min(addition, space) : 0;
                    if (allowed < addition) {
                        owner.spawnAtLocation(stack.copyWithCount(addition - allowed));
                        stack.shrink(addition - allowed);
                        if (!grounded()) warnAirborne();
                    }
                }
                accepted[index] = stack.copy();
            }
        }
        super.setChanged();
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (restoring || owner.level().isClientSide || !AircraftPolicy.enabled(owner) || !fuelSlot(index)) {
            super.setItem(index, stack);
            return;
        }
        ItemStack previous = getItem(index);
        int previousCount = ItemStack.isSameItemSameTags(previous, stack) ? previous.getCount() : 0;
        int addition = stack.getCount() - previousCount;
        if (addition > 0) {
            int accepted = grounded() && Utils.getFuelTime(stack) > 0 ? Math.min(addition, freeUnits()) : 0;
            int rejected = addition - accepted;
            if (rejected > 0) {
                // Direct automation writes have no return-value seam. Preserve the rejected items as drops.
                owner.spawnAtLocation(stack.copyWithCount(rejected));
                stack = stack.copyWithCount(previousCount + accepted);
                if (!grounded()) warnAirborne();
            }
        }
        super.setItem(index, stack);
    }

    /** Legacy over-capacity saves keep their items; excess is returned beside the aircraft once. */
    public void reconcileCapacity() {
        int excess = Math.max(0, storedUnits() + (owner instanceof AircraftFuelState state ? state.jemcompat$burningUnits() : 0)
                - AircraftPolicy.CAPACITY);
        if (excess == 0) return;
        for (var slot : owner.getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER)) {
            ItemStack stack = getItem(slot.index());
            if (Utils.getFuelTime(stack) <= 0) continue;
            int amount = Math.min(excess, stack.getCount());
            owner.spawnAtLocation(removeItem(slot.index(), amount));
            excess -= amount;
            if (excess == 0) break;
        }
    }
}
