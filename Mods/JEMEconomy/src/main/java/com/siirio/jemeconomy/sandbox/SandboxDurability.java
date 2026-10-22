package com.siirio.jemeconomy.sandbox;

import com.siirio.jemeconomy.persistence.NativeSavedDataCommit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Sandbox admission; Economy owns the durable native file replacement. */
public final class SandboxDurability {
    private SandboxDurability() { }

    public static void save(MinecraftServer server, SavedData data, String name) {
        if (!SandboxCommerce.active(server)) throw new IllegalStateException("Sandbox financial durability required");
        if (data.isDirty()) NativeSavedDataCommit.save(server, data, name);
    }
}
