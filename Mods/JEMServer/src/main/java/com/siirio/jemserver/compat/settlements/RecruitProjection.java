package com.siirio.jemserver.compat.settlements;

import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.town.SettlementSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

public final class RecruitProjection {
    private RecruitProjection() {}

    public static CompoundTag view(ServerPlayer viewer) {
        CompoundTag result = new CompoundTag(); result.putString("tab", "recruits");
        ListTag rows = new ListTag();
        for (var town : SettlementSavedData.get(viewer.getServer()).towns()) {
            CompoundTag settlement = SettlementNetwork.readOnlyView(viewer, town);
            for (Tag value : settlement.getList("Defense", Tag.TAG_COMPOUND)) {
                CompoundTag source = (CompoundTag) value; CompoundTag row = source.copy();
                row.putBoolean("CoinCurrency", settlement.getBoolean("CoinCurrency"));
                row.putUUID("id", source.getUUID("Id")); row.putString("name", source.getString("Name"));
                row.putString("settlement", town.name()); row.putString("type", source.getString("NativeType"));
                row.putBoolean("living", source.getBoolean("Living")); row.putLong("nextPayroll", source.getLong("NextPayroll"));
                if (settlement.contains("UnpaidPayroll")) row.putLong("outstandingPayroll", settlement.getLong("UnpaidPayroll"));
                rows.add(row);
            }
        }
        CompoundTag personal = SettlementNetwork.personalRecruitView(viewer);
        for (Tag value : personal.getList("Defense", Tag.TAG_COMPOUND)) {
            CompoundTag source = (CompoundTag) value; CompoundTag row = source.copy();
            row.putBoolean("CoinCurrency", personal.getBoolean("CoinCurrency"));
            row.putUUID("id", source.getUUID("Id")); row.putString("name", source.getString("Name"));
            row.putString("settlement", ""); row.putString("type", source.getString("NativeType"));
            row.putBoolean("living", source.getBoolean("Living")); row.putLong("nextPayroll", source.getLong("NextPayroll"));
            row.putLong("outstandingPayroll", personal.getLong("UnpaidPayroll")); rows.add(row);
        }
        result.put("rows", rows); result.putInt("total", rows.size()); result.putInt("pageSize", 128);
        return result;
    }
}
