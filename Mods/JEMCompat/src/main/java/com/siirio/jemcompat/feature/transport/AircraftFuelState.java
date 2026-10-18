package com.siirio.jemcompat.feature.transport;

/** Implemented by the native engine; inventories consult the same persisted owner. */
public interface AircraftFuelState {
    int jemcompat$burningUnits();
    void jemcompat$pauseFuelClock();
    void jemcompat$writeFuel(net.minecraft.nbt.CompoundTag tag);
    void jemcompat$readFuel(net.minecraft.nbt.CompoundTag tag);
}
