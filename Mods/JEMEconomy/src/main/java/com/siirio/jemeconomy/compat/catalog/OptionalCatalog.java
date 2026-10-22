package com.siirio.jemeconomy.compat.catalog;

import java.lang.reflect.Method;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Optional shared classifier. Its absence hides classifier-owned stock, never invents prices/categories. */
public final class OptionalCatalog {
    private static final class NativeMethods {
        static final Method BUILDING = method("isMarketBuilding", ItemStack.class);
        static final Method CLASSIFY = method("classify", ItemStack.class,
                net.minecraft.world.entity.LivingEntity.class, java.util.function.Predicate.class);
        private static Method method(String name, Class<?>... arguments) {
            try { return Class.forName("com.siirio.jemcompat.catalog.JemCatalogCategories").getMethod(name, arguments); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Installed JEMCompat catalog contract unavailable", failure); }
        }
    }
    private OptionalCatalog() { }
    public static boolean isBuilding(ItemStack stack) {
        if (!ModList.get().isLoaded("jemcompat")) return false;
        try { return (boolean) NativeMethods.BUILDING.invoke(null, stack); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("JEM building classifier failed", failure); }
    }
    public static boolean isNature(ItemStack stack) {
        if (!ModList.get().isLoaded("jemcompat")) return false;
        try {
            Object kind = NativeMethods.CLASSIFY.invoke(null, stack, null,
                    (java.util.function.Predicate<ItemStack>) ignored -> false);
            return ((Enum<?>) kind).name().equals("NATURE");
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("JEM nature classifier failed", failure); }
    }
}
