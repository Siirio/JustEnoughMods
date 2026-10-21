package com.siirio.jemeconomy.compat.kubejs;

import java.lang.reflect.Field;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;

/** Exact installed KubeJS reload contract, isolated from common/runtime signatures. */
public final class KubeRecipeReload {
    public record Context(RecipeManager manager, MinecraftServer server) { }
    private static final class Fields {
        static final Field RESOURCES = field("dev.latvian.mods.kubejs.server.KubeJSReloadListener", "resources");
        static final Field SCRIPTS = field("dev.latvian.mods.kubejs.server.ServerScriptManager", "instance");
        static final Field SERVER = field("dev.latvian.mods.kubejs.server.ServerScriptManager", "server");
        static final Field EVENT = field("dev.latvian.mods.kubejs.recipe.RecipesEventJS", "instance");
        private static Field field(String owner, String name) {
            try { return Class.forName(owner).getField(name); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Installed KubeJS recipe reload contract unavailable", failure); }
        }
    }
    private KubeRecipeReload() { }
    public static Context current() {
        try {
            if (Fields.EVENT.get(null) == null) throw new IllegalStateException("recipePolicyEnabled must run inside ServerEvents.recipes");
            Object resources = Fields.RESOURCES.get(null);
            if (!(resources instanceof ReloadableServerResources nativeResources))
                throw new IllegalStateException("KubeJS has no active native recipe reload resources");
            Object scripts = Fields.SCRIPTS.get(null);
            if (scripts == null) throw new IllegalStateException("KubeJS has no active server script manager");
            Object owner = Fields.SERVER.get(scripts);
            return new Context(nativeResources.getRecipeManager(), owner instanceof MinecraftServer server ? server : null);
        } catch (IllegalAccessException failure) { throw new IllegalStateException("Cannot inspect KubeJS native recipe reload owner", failure); }
    }
}
