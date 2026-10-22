package com.siirio.jemserver.compat.professions;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

/** Loaded only after the optional jem_professions presence gate. */
public final class ProfessionHostAdapter {
    private ProfessionHostAdapter() {}

    public static void open(ServerPlayer player) { ProfessionApi.openScreen(player); }

    public static ListTag snapshot(ServerPlayer player) {
        ListTag result = new ListTag();
        for (ProfessionId profession : ProfessionId.values()) {
            long xp = ProfessionApi.xp(player, profession);
            if (xp <= 0L) continue;
            CompoundTag row = new CompoundTag();
            row.putString("id", profession.name());
            row.putLong("xp", xp);
            row.putInt("level", ProfessionApi.level(player, profession));
            result.add(row);
        }
        return result;
    }
}
