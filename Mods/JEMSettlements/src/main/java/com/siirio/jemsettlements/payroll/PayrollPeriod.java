package com.siirio.jemsettlements.payroll;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class PayrollPeriod {
    public record Wage(UUID soldierId, ResourceLocation nativeType, long copper, long generation) {
        CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putUUID("Soldier", soldierId); tag.putString("NativeType", nativeType.toString()); tag.putLong("Copper", copper); tag.putLong("Generation", generation); return tag; }
        static Wage load(CompoundTag tag) { ResourceLocation type = ResourceLocation.tryParse(tag.getString("NativeType")); return new Wage(tag.getUUID("Soldier"), type == null ? new ResourceLocation("recruits", "recruit") : type, tag.getLong("Copper"), tag.getLong("Generation")); }
    }
    private final long index;
    private final long dueGameTime;
    private final List<Wage> roster;
    private final long total;
    private boolean coinCurrency = com.siirio.jemsettlements.currency.SettlementCurrency.coins();
    public boolean currentCurrency() { return coinCurrency == com.siirio.jemsettlements.currency.SettlementCurrency.coins(); }
    private boolean paid;
    private UUID receipt;
    public PayrollPeriod(long index, long dueGameTime, List<Wage> roster) { this.index = index; this.dueGameTime = dueGameTime; this.roster = List.copyOf(roster); this.total = roster.stream().filter(wage -> RecruitRank.of(wage.nativeType()) != RecruitRank.UNSUPPORTED).mapToLong(Wage::copper).reduce(0L, Math::addExact); }
    public long index() { return index; } public long dueGameTime() { return dueGameTime; } public List<Wage> roster() { return roster; } public long total() { return total; } public boolean paid() { return paid; } public UUID receipt() { return receipt; }
    public void paid(UUID value) { paid = true; receipt = value; }
    public CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putLong("Index", index); tag.putLong("Due", dueGameTime); tag.putLong("Total", total); tag.putBoolean("CoinCurrency", coinCurrency); tag.putBoolean("Paid", paid); if (receipt != null) tag.putUUID("Receipt", receipt); ListTag rows = new ListTag(); roster.forEach(value -> rows.add(value.save())); tag.put("Roster", rows); return tag; }
    public static PayrollPeriod load(CompoundTag tag) { List<Wage> roster = new ArrayList<>(); ListTag rows = tag.getList("Roster", Tag.TAG_COMPOUND); for (int i = 0; i < rows.size(); i++) roster.add(Wage.load(rows.getCompound(i))); PayrollPeriod period = new PayrollPeriod(tag.getLong("Index"), tag.getLong("Due"), roster); period.coinCurrency = !tag.contains("CoinCurrency") || tag.getBoolean("CoinCurrency"); period.paid = tag.getBoolean("Paid"); if (tag.hasUUID("Receipt")) period.receipt = tag.getUUID("Receipt"); return period; }
}
