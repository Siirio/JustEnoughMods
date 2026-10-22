package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

/** Host-authored availability; optional domains retain their own state and authority. */
public final class DomainFeatures {
    private DomainFeatures() { }
    public static boolean loaded(String id) { return ModList.get().isLoaded(id); }
    public static boolean economy(MinecraftServer server) { return loaded("jem_economy") && Economy.enabled(server); }
    private static final class Economy {
        static boolean enabled(MinecraftServer server) { return com.siirio.jemeconomy.api.EconomyApi.isEnabled(server); }
    }
    public static String economyAvailability(MinecraftServer server) {
        return !loaded("jem_economy") ? "UNAVAILABLE_MISSING_CONTENT" : economy(server) ? "AVAILABLE" : "DISABLED_BY_WORLD";
    }
    public static CompoundTag snapshot(MinecraftServer server) {
        var result = new CompoundTag();
        result.putBoolean("economy", economy(server));
        result.putString("economyAvailability", economyAvailability(server));
        result.putBoolean("roles", loaded("jem_roles"));
        result.putBoolean("professions", loaded("jem_professions"));
        result.putBoolean("settlements", loaded("jem_settlements"));
        result.putBoolean("recruits", loaded("jem_settlements") && loaded("recruits"));
        result.putBoolean("events", com.siirio.jemserver.smp.events.EventRuntime.enabled(server));
        result.putBoolean("prizes", loaded("jem_world_boss_tiers"));
        return result;
    }
}
