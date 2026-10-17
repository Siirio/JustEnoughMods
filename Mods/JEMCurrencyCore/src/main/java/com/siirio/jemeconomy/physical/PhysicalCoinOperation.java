package com.siirio.jemeconomy.physical;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public record PhysicalCoinOperation(UUID id, UUID accountId, Kind kind, Phase phase, long value,
                                    Map<UUID, Integer> lotQuantities, Map<UUID, Integer> outputLots) {
    public enum Kind { WITHDRAW, DEPOSIT, EXCHANGE, CONSOLIDATE, CASH_PAYMENT }
    public enum Phase { PREPARED, INVENTORY_APPLIED, COMMITTED, QUARANTINED }

    public PhysicalCoinOperation withPhase(Phase newPhase) {
        return new PhysicalCoinOperation(id, accountId, kind, newPhase, value, lotQuantities, outputLots);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("Account", accountId);
        tag.putString("Kind", kind.name());
        tag.putString("Phase", phase.name());
        tag.putLong("Value", value);
        ListTag lots = new ListTag();
        lotQuantities.forEach((lot, count) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Lot", lot);
            row.putInt("Count", count);
            lots.add(row);
        });
        tag.put("Lots", lots);
        ListTag outputs = new ListTag();
        outputLots.forEach((lot, count) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Lot", lot);
            row.putInt("Count", count);
            outputs.add(row);
        });
        tag.put("Outputs", outputs);
        return tag;
    }

    public static PhysicalCoinOperation load(CompoundTag tag) {
        Map<UUID, Integer> lots = new LinkedHashMap<>();
        ListTag list = tag.getList("Lots", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag row = list.getCompound(index);
            if (row.hasUUID("Lot") && row.getInt("Count") > 0) lots.put(row.getUUID("Lot"), row.getInt("Count"));
        }
        Map<UUID, Integer> outputs = new LinkedHashMap<>();
        ListTag outputList = tag.getList("Outputs", Tag.TAG_COMPOUND);
        for (int index = 0; index < outputList.size(); index++) {
            CompoundTag row = outputList.getCompound(index);
            if (row.hasUUID("Lot") && row.getInt("Count") > 0) outputs.put(row.getUUID("Lot"), row.getInt("Count"));
        }
        Kind kind;
        Phase phase;
        try { kind = Kind.valueOf(tag.getString("Kind")); } catch (IllegalArgumentException ignored) { kind = Kind.DEPOSIT; }
        try { phase = Phase.valueOf(tag.getString("Phase")); } catch (IllegalArgumentException ignored) { phase = Phase.QUARANTINED; }
        return new PhysicalCoinOperation(tag.getUUID("Id"), tag.getUUID("Account"), kind, phase,
                Math.max(0L, tag.getLong("Value")), Map.copyOf(lots), Map.copyOf(outputs));
    }
}
