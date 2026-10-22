package com.siirio.jemeconomy.policy;

import com.siirio.jemeconomy.config.EconomyConfig;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Recipe projection belongs to its exact native reload generation, never a global last-world flag. */
public final class EconomyRecipePolicy {
    private static final Map<RecipeManager, Boolean> PROJECTED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<MinecraftServer> REQUIRED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Set<MinecraftServer> RELOADING = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private EconomyRecipePolicy() { }

    /** Called only by ServerEvents.recipes. Initial resources have no world config yet. */
    public static boolean query() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("kubejs"))
            throw new IllegalStateException("Recipe projection query requires the installed KubeJS reload owner");
        var context = com.siirio.jemeconomy.compat.kubejs.KubeRecipeReload.current();
        RecipeManager manager = context.manager();
        boolean enabled = configured(context.server());
        synchronized (PROJECTED) {
            Boolean previous = PROJECTED.putIfAbsent(manager, enabled);
            if (previous != null && previous != enabled)
                throw new IllegalStateException("Economy setting changed during one recipe application; refusing mixed projection");
        }
        return enabled;
    }

    private static boolean configured(MinecraftServer server) {
        return server != null && server == ServerLifecycleHooks.getCurrentServer()
                && EconomyConfig.SPEC.isLoaded() && EconomyConfig.ENABLED.get();
    }

    /** A config enable cannot admit currency until its corresponding recipes are installed. */
    public static boolean applied(MinecraftServer server) {
        if (server == null) return false;
        if (RELOADING.contains(server)) return false;
        Boolean projected = PROJECTED.get(server.getRecipeManager());
        return projected == null ? !REQUIRED.contains(server) : projected == configured(server);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void starting(ServerStartingEvent event) { reconcile(event.getServer()); }

    /** At most one native reload for a mismatching generation; no retry loop or invented config defaults. */
    public static void reconcile(MinecraftServer server) {
        if (server == null || server != ServerLifecycleHooks.getCurrentServer() || !EconomyConfig.SPEC.isLoaded()) return;
        if (!server.isSameThread()) { server.execute(() -> reconcile(server)); return; }
        if (RELOADING.contains(server)) return;
        RecipeManager manager = server.getRecipeManager();
        Boolean projected = PROJECTED.get(manager);
        if (projected == null) {
            if (REQUIRED.contains(server)) throw new IllegalStateException("Economy recipe policy was omitted by this resource generation");
            return;
        }
        REQUIRED.add(server);
        boolean enabled = configured(server);
        if (projected == enabled) return;
        RELOADING.add(server);
        try {
            // Vanilla managedBlock pumps the server executor when called on its own thread.
            // ServerStarting runs after loadLevel, before ServerStarted permits player logins.
            server.reloadResources(server.getPackRepository().getSelectedIds()).join();
            RecipeManager replacement = server.getRecipeManager();
            Boolean applied = PROJECTED.get(replacement);
            if (replacement == manager || applied == null || applied != enabled && configured(server) == enabled)
                throw new IllegalStateException("Economy world recipe projection was not applied by the native resource reload");
        } finally { RELOADING.remove(server); }
        // Only an externally changed world setting may require another generation.
        if (configured(server) != enabled) server.execute(() -> reconcile(server));
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        REQUIRED.remove(event.getServer()); RELOADING.remove(event.getServer());
        PROJECTED.remove(event.getServer().getRecipeManager());
    }
}
