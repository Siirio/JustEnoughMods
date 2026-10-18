package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.AircraftFuelInventory;
import com.siirio.jemcompat.feature.transport.AircraftFuelState;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.inventory.SparseSimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InventoryVehicleEntity.class, remap = false)
public abstract class AircraftInventoryMixin {
    @Shadow private SparseSimpleInventory inventory;

    @Inject(method = "initInventory()V", at = @At("TAIL"))
    private void jemcompat$boundInventory(CallbackInfo callback) {
        var owner = (InventoryVehicleEntity) (Object) this;
        if (!(owner instanceof EngineVehicle)) return;
        inventory.removeListener(owner);
        inventory = new AircraftFuelInventory(owner, inventory.getContainerSize());
        inventory.addListener(owner);
    }

    @Inject(method = "addItemTag", at = @At("TAIL"))
    private void jemcompat$savePackedFuel(net.minecraft.nbt.CompoundTag tag, CallbackInfo callback) {
        if ((Object) this instanceof AircraftFuelState state) state.jemcompat$writeFuel(tag);
    }

    @Inject(method = "readItemTag", at = @At("TAIL"))
    private void jemcompat$readPackedFuel(net.minecraft.nbt.CompoundTag tag, CallbackInfo callback) {
        if ((Object) this instanceof AircraftFuelState state) state.jemcompat$readFuel(tag);
    }
}
