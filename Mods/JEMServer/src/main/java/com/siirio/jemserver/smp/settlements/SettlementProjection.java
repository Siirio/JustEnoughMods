package com.siirio.jemserver.smp.settlements;

import com.siirio.jemserver.smp.Profiles;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.town.SettlementSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

public final class SettlementProjection {
    private SettlementProjection() {}

    public static CompoundTag view(ServerPlayer viewer, java.util.UUID selected) {
        CompoundTag result = new CompoundTag(); result.putString("tab", "settlements");
        ListTag rows = new ListTag();
        var principal = com.siirio.jemidentity.api.IdentityPolicy.resolve(viewer);
        var towns = principal.isEmpty() ? java.util.List.<com.siirio.jemsettlements.town.TownRecord>of()
                : SettlementSavedData.get(viewer.getServer()).ownedTowns(principal.get().accountId());
        for (var town : towns) {
            CompoundTag full = SettlementNetwork.readOnlyView(viewer, town);
            CompoundTag row = new CompoundTag();
            row.putUUID("id", town.id()); row.putString("name", town.name());
            row.putString("dimension", town.dimension().toString()); row.putLong("position", town.center().asLong());
            row.putInt("happiness", town.happiness());
            row.putInt("population", full.getList("Residents", net.minecraft.nbt.Tag.TAG_COMPOUND).size() + SettlementSavedData.get(viewer.getServer()).workforce(town.id()).size());
            rows.add(row);
        }
        result.put("rows", rows); result.putInt("total", rows.size()); result.putInt("pageSize", 64);
        var chosen = selected == null && towns.size() == 1 ? towns.get(0) : towns.stream().filter(town -> town.id().equals(selected)).findFirst().orElse(null);
        if (chosen != null) {
            CompoundTag detail = SettlementNetwork.readOnlyView(viewer, chosen);
            detail.putUUID("id", chosen.id()); detail.putString("name", chosen.name()); detail.putString("title", chosen.name());
            detail.putString("dimension", chosen.dimension().toString()); detail.putLong("position", chosen.center().asLong());
            detail.putInt("revision", java.util.Objects.hash(chosen.territoryMinX(), chosen.territoryMinZ(), chosen.territoryMaxX(), chosen.territoryMaxZ(), chosen.name()));
            result.put("detail", detail);
        }
        return result;
    }
}
