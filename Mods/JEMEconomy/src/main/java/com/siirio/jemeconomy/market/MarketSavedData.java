package com.siirio.jemeconomy.market;

import com.siirio.jemeconomy.policy.ItemPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class MarketSavedData extends SavedData {
    private static final String NAME = "jem_economy_market";
    public static final long GLOBAL_DAILY_PAYOUT = 1_500L;
    public static final long ACCOUNT_DAILY_PAYOUT = 80L;
    private static final List<String> GROUPS = List.of("crops", "fishing", "cooking", "animal_products", "mining", "forestry", "handcrafts");

    private long activeCycle = Long.MIN_VALUE;
    private long seed;
    private long globalPaid;
    private final Map<UUID, Long> accountPaid = new HashMap<>();
    private final Map<ResourceLocation, Integer> globalUnits = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Integer>> accountUnits = new HashMap<>();
    private final LinkedHashMap<String, Boolean> committed = new LinkedHashMap<>();

    public MarketSavedData() { seed = UUID.randomUUID().getMostSignificantBits(); }
    public static MarketSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MarketSavedData::load, MarketSavedData::new, NAME);
    }

    public synchronized long cycle(MinecraftServer server) {
        long candidate = Math.floorDiv(server.overworld().getDayTime() - 1_000L, 24_000L);
        if (activeCycle == Long.MIN_VALUE || candidate > activeCycle) begin(candidate);
        return activeCycle;
    }

    public synchronized Demand demand(MinecraftServer server) {
        long day = cycle(server);
        int first = Math.floorMod(mix(seed ^ day), GROUPS.size());
        int second = Math.floorMod(mix(seed ^ day ^ 0x9E3779B97F4A7C15L), GROUPS.size() - 1);
        if (second >= first) second++;
        return new Demand(day, GROUPS.get(first), GROUPS.get(second));
    }

    public synchronized QuoteAllowance allowance(MinecraftServer server, UUID account, ItemPolicy policy, int requested, long unitPrice) {
        cycle(server);
        if (account == null || !policy.sellable() || requested <= 0 || unitPrice <= 0L) return new QuoteAllowance(0, 0L, "blocked");
        long accountMoney = Math.max(0L, ACCOUNT_DAILY_PAYOUT - accountPaid.getOrDefault(account, 0L));
        long globalMoney = Math.max(0L, GLOBAL_DAILY_PAYOUT - globalPaid);
        int accountItem = Math.max(0, policy.dailyPlayerUnits() - accountUnits.getOrDefault(account, Map.of()).getOrDefault(policy.item(), 0));
        int globalItem = Math.max(0, policy.dailyGlobalUnits() - globalUnits.getOrDefault(policy.item(), 0));
        long money = Math.min(accountMoney, globalMoney);
        int accepted = (int) Math.min(Math.min((long) requested, Math.min(accountItem, globalItem)), money / unitPrice);
        String reason = accepted > 0 ? "ok" : accountMoney == 0 ? "account_daily_cap" : globalMoney == 0 ? "global_daily_cap" :
                accountItem == 0 ? "item_account_cap" : globalItem == 0 ? "item_global_cap" : "budget_too_small";
        return new QuoteAllowance(accepted, Math.multiplyExact((long) accepted, unitPrice), reason);
    }

    public synchronized boolean commit(MinecraftServer server, String operationKey, UUID account, Map<ResourceLocation, Integer> units, long payout) {
        cycle(server);
        if (committed.containsKey(operationKey)) return true;
        if (operationKey == null || account == null || payout <= 0L || payout > ACCOUNT_DAILY_PAYOUT - accountPaid.getOrDefault(account, 0L)
                || payout > GLOBAL_DAILY_PAYOUT - globalPaid) return false;
        globalPaid += payout;
        accountPaid.merge(account, payout, Long::sum);
        Map<ResourceLocation, Integer> perAccount = accountUnits.computeIfAbsent(account, ignored -> new HashMap<>());
        units.forEach((id, count) -> { globalUnits.merge(id, count, Integer::sum); perAccount.merge(id, count, Integer::sum); });
        committed.put(operationKey, Boolean.TRUE);
        while (committed.size() > 50_000) committed.remove(committed.keySet().iterator().next());
        setDirty();
        return true;
    }

    public synchronized long accountRemaining(MinecraftServer server, UUID account) { cycle(server); return Math.max(0L, ACCOUNT_DAILY_PAYOUT - accountPaid.getOrDefault(account, 0L)); }
    public synchronized long globalRemaining(MinecraftServer server) { cycle(server); return Math.max(0L, GLOBAL_DAILY_PAYOUT - globalPaid); }
    public synchronized int multiplier(MinecraftServer server, String group) {
        Demand demand = demand(server); return demand.primary().equals(group) ? 150 : demand.secondary().equals(group) ? 125 : 100;
    }

    private void begin(long cycle) {
        activeCycle = cycle; globalPaid = 0L; accountPaid.clear(); globalUnits.clear(); accountUnits.clear(); setDirty();
    }
    private static int mix(long value) { value ^= value >>> 33; value *= 0xff51afd7ed558ccdl; value ^= value >>> 33; return (int) (value ^ value >>> 32); }

    @Override public synchronized CompoundTag save(CompoundTag tag) {
        tag.putLong("Cycle", activeCycle); tag.putLong("Seed", seed); tag.putLong("GlobalPaid", globalPaid);
        CompoundTag paid = new CompoundTag(); accountPaid.forEach((id, value) -> paid.putLong(id.toString(), value)); tag.put("AccountPaid", paid);
        CompoundTag gu = new CompoundTag(); globalUnits.forEach((id, value) -> gu.putInt(id.toString(), value)); tag.put("GlobalUnits", gu);
        ListTag au = new ListTag(); accountUnits.forEach((account, rows) -> { CompoundTag row = new CompoundTag(); row.putUUID("Account", account); CompoundTag values = new CompoundTag(); rows.forEach((id, count) -> values.putInt(id.toString(), count)); row.put("Values", values); au.add(row); }); tag.put("AccountUnits", au);
        ListTag done = new ListTag(); committed.keySet().forEach(key -> { CompoundTag row = new CompoundTag(); row.putString("Key", key); done.add(row); }); tag.put("Committed", done);
        return tag;
    }
    private static MarketSavedData load(CompoundTag tag) {
        MarketSavedData data = new MarketSavedData(); data.activeCycle = tag.contains("Cycle") ? tag.getLong("Cycle") : Long.MIN_VALUE; data.seed = tag.contains("Seed") ? tag.getLong("Seed") : data.seed; data.globalPaid = tag.getLong("GlobalPaid");
        CompoundTag paid = tag.getCompound("AccountPaid"); paid.getAllKeys().forEach(key -> { try { data.accountPaid.put(UUID.fromString(key), paid.getLong(key)); } catch (IllegalArgumentException ignored) { } });
        CompoundTag gu = tag.getCompound("GlobalUnits"); gu.getAllKeys().forEach(key -> { ResourceLocation id = ResourceLocation.tryParse(key); if (id != null) data.globalUnits.put(id, gu.getInt(key)); });
        ListTag au = tag.getList("AccountUnits", Tag.TAG_COMPOUND); for (int i = 0; i < au.size(); i++) { CompoundTag row = au.getCompound(i); if (!row.hasUUID("Account")) continue; Map<ResourceLocation, Integer> values = new HashMap<>(); CompoundTag raw = row.getCompound("Values"); raw.getAllKeys().forEach(key -> { ResourceLocation id = ResourceLocation.tryParse(key); if (id != null) values.put(id, raw.getInt(key)); }); data.accountUnits.put(row.getUUID("Account"), values); }
        ListTag done = tag.getList("Committed", Tag.TAG_COMPOUND); for (int i = 0; i < done.size(); i++) data.committed.put(done.getCompound(i).getString("Key"), Boolean.TRUE);
        return data;
    }

    public record Demand(long cycle, String primary, String secondary) { }
    public record QuoteAllowance(int units, long payout, String reason) { }
}
