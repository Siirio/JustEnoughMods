package com.siirio.jemsettlements.happiness;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** Reads the SMP cooking recipe graph without making Settlements a second progression owner. */
public final class AvailableDishService {
    private AvailableDishService() {}

    public static List<ResourceLocation> moddedDishes(MinecraftServer server) {
        try {
            Class<?> recipes = Class.forName("com.siirio.jemserver.smp.events.CookingRecipes");
            Method available = recipes.getMethod("available", MinecraftServer.class);
            var result = new ArrayList<ResourceLocation>();
            for (Object dish : (List<?>) available.invoke(null, server)) {
                Object value = dish.getClass().getMethod("item").invoke(dish);
                if (value instanceof ResourceLocation id && !id.getNamespace().equals("minecraft")) result.add(id);
            }
            return List.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            return List.of();
        }
    }
}
