package com.siirio.jemserver.smp.settlements;

import com.siirio.jemserver.smp.Profiles;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.town.SettlementSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

public final class SettlementProjection {
    private SettlementProjection() {}

    public static CompoundTag view(ServerPlayer viewer) {
        CompoundTag result = new CompoundTag(); result.putString("tab", "settlements");
        ListTag rows = new ListTag();
        for (var town : SettlementSavedData.get(viewer.getServer()).towns()) {
            CompoundTag row = SettlementNetwork.readOnlyView(viewer, town);
            row.putUUID("id", town.id()); row.putString("name", town.name());
            row.putString("mayor", Profiles.get(viewer.getServer(), town.owner()).getString("name"));
            row.putInt("population", town.members().size() + row.getList("Residents", net.minecraft.nbt.Tag.TAG_COMPOUND).size());
            row.putInt("recruitCount", row.getList("Defense", net.minecraft.nbt.Tag.TAG_COMPOUND).size());
            rows.add(row);
        }
        result.put("rows", rows); result.putInt("total", rows.size()); result.putInt("pageSize", 64);
        return result;
    }
}
