package com.justenoughmods.achievementguide.integration.travel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fml.ModList;

/** Exact optional public JEMCompat read projection. No clock conversion or inferred fuel formula. */
public final class AircraftFuelObservation {
    private static Method projection;
    private static boolean resolved;
    private AircraftFuelObservation() {}

    public static long remaining(Object vehicle) {
        if (!ModList.get().isLoaded("jemcompat")) return -1;
        try {
            if (!resolved) {
                projection = Class.forName("com.siirio.jemcompat.feature.transport.AircraftFuelState")
                        .getMethod("jemcompat$writeFuel", CompoundTag.class);
                resolved = true;
            }
            if (!projection.getDeclaringClass().isInstance(vehicle)) return -1;
            CompoundTag snapshot = new CompoundTag();
            projection.invoke(vehicle, snapshot);
            return snapshot.contains("JemActiveFuelNanos", 4) ? snapshot.getLong("JemActiveFuelNanos") : -1;
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException("Aircraft fuel owner projection failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Aircraft fuel owner API changed", failure);
        }
    }
}
