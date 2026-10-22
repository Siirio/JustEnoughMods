package com.justenoughmods.achievementguide.client.handbook.enchantment;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraftforge.fml.ModList;

/** Exact optional Create 6.0.8 API; its synchronized config owns the resulting value. */
final class NativeCapacity {
    private static Method maximum;
    private static boolean attempted;
    private static boolean failed;
    private static boolean reportedInvocationFailure;

    private NativeCapacity() { }

    static Integer maximum(int level) {
        if (!attempted) {
            attempted = true;
            if (ModList.get().isLoaded("create")) {
                try {
                    maximum = Class.forName("com.simibubi.create.content.equipment.armor.BacktankUtil").getMethod("maxAir", int.class);
                } catch (ReflectiveOperationException exception) {
                    failed = true;
                    LogUtils.getLogger().error("Create backtank capacity help API is unavailable", exception);
                }
            }
        }
        if (maximum == null || failed) return null;
        try {
            return (Integer) maximum.invoke(null, level);
        } catch (ReflectiveOperationException exception) {
            if (!reportedInvocationFailure) {
                reportedInvocationFailure = true;
                LogUtils.getLogger().error("Create backtank capacity help value could not be read", exception);
            }
            return null;
        }
    }
}
