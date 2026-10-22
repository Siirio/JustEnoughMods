package com.siirio.jemserver.compat.professions;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.server.level.ServerPlayer;

/** Loaded only after the optional jem_professions presence gate. */
public final class ProfessionHostAdapter {
    private ProfessionHostAdapter() {}

    public static void open(ServerPlayer player) { ProfessionApi.openScreen(player); }

    public static java.util.List<com.siirio.jemserver.smp.ProfileState.ProfessionSnapshot> snapshot(ServerPlayer player) {
        var result = new java.util.ArrayList<com.siirio.jemserver.smp.ProfileState.ProfessionSnapshot>();
        for (ProfessionId profession : ProfessionId.values()) {
            long xp = ProfessionApi.xp(player, profession);
            if (xp > 0L) result.add(new com.siirio.jemserver.smp.ProfileState.ProfessionSnapshot(profession.name(), xp, ProfessionApi.level(player, profession)));
        }
        return java.util.List.copyOf(result);
    }
}
