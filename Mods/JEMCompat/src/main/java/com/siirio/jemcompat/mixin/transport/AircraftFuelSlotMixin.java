package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.AircraftFuelInventory;
import com.siirio.jemcompat.feature.transport.AircraftFuelSlot;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.inventory.slots.FuelSlotDescription;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FuelSlotDescription.class, remap = false)
public abstract class AircraftFuelSlotMixin {
    @Inject(method = "getSlot", at = @At("RETURN"), cancellable = true)
    private void jemcompat$guardedSlot(InventoryVehicleEntity vehicle, Container inventory, CallbackInfoReturnable<Slot> callback) {
        if (!(inventory instanceof AircraftFuelInventory)) return;
        Slot slot = callback.getReturnValue();
        callback.setReturnValue(new AircraftFuelSlot(inventory, slot.getContainerSlot(), slot.x, slot.y));
    }
}
