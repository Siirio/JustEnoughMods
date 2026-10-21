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
    public static final long GLOBAL_DAILY_PAYOUT = limits("global_daily_payout",10_000L);
    public static final long ACCOUNT_DAILY_PAYOUT = limits("account_daily_payout",400L);
    private static long limits(String key,long fallback) {
        try (var stream = MarketSavedData.class.getResourceAsStream("/data/jem_economy/economy/market_limits.json")) {
            if (stream == null) return fallback;
            long value = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().get(key).getAsLong();
            if (value <= 0 || value > 1_000_000_000L) throw new IllegalArgumentException("Invalid market limit");
            return value;
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read market limits",failure); }
    }
    private static final List<String> GROUPS = List.of("crops", "fishing", "cooking", "animal_products", "mining", "forestry", "handcrafts");

    private long activeCycle = Long.MIN_VALUE;
    private long seed;
    private long globalPaid;
    private long revision;
    private final Map<UUID, Long> accountPaid = new HashMap<>();
    private final Map<ResourceLocation, Integer> globalUnits = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Integer>> accountUnits = new HashMap<>();
    private final Map<ResourceLocation, Integer> purchasedUnits = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Integer>> accountPurchases = new HashMap<>();
    private final LinkedHashMap<String, Boolean> committed = new LinkedHashMap<>();

    public MarketSavedData() { seed = UUID.randomUUID().getMostSignificantBits(); }
    public static MarketSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MarketSavedData::load, MarketSavedData::new, NAME);
    }

    public static boolean testQuotaBypass(MinecraftServer server) {
        return com.siirio.jemidentity.api.DedicatedTestProfile.binding(server)
                .map(com.siirio.jemidentity.api.DedicatedTestProfile.Binding::bypassQuotas).orElse(false);
    }

    public synchronized long cycle(MinecraftServer server) {
        long candidate = Math.floorDiv(server.overworld().getDayTime() - 1_000L, 210_000L);
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
        long accountMoney = Long.MAX_VALUE;
        long globalMoney = Long.MAX_VALUE;
        int accountItem = accountItemRemaining(server, account, policy);
        int globalItem = globalItemRemaining(server, policy);
        long money = Math.min(accountMoney, globalMoney);
        int accepted = (int) Math.min(Math.min((long) requested, Math.min(accountItem, globalItem)), money / unitPrice);
        String reason = accepted > 0 ? "ok" : accountMoney == 0 ? "account_daily_cap" : globalMoney == 0 ? "global_daily_cap" :
                accountItem == 0 ? "item_account_cap" : globalItem == 0 ? "item_global_cap" : "budget_too_small";
        return new QuoteAllowance(accepted, Math.multiplyExact((long) accepted, unitPrice), reason);
    }

    public synchronized QuoteAllowance allowanceRational(MinecraftServer server, UUID account, ItemPolicy policy, int requested) {
        cycle(server);
        if (account == null || !policy.sellable() || requested <= 0) return new QuoteAllowance(0,0,"blocked");
        int units = Math.min(requested,Math.min(accountItemRemaining(server,account,policy),globalItemRemaining(server,policy)));
        long money = policy.disc() || testQuotaBypass(server) ? Long.MAX_VALUE : Math.min(accountRemaining(server,account),globalRemaining(server));
        int multiplier = multiplier(server,policy.demandGroup());
        while (units > 0) {
            var raw = policy.salePrice(units,multiplier);
            long payout = com.siirio.jemeconomy.ledger.EconomySavedData.get(server).quoteSale(account,raw.numerator(),raw.denominator()).payout();
            if (payout <= money) return new QuoteAllowance(units,payout,payout > 0 ? "ok" : "budget_too_small");
            units--;
        }
        return new QuoteAllowance(0,0,"item_cap_or_budget");
    }

    /** Whole native offer budget, including indivisible bulk prices such as sixteen bricks for one Coin. */
    public synchronized QuoteAllowance allowanceExactTrade(MinecraftServer server, UUID account, ItemPolicy policy, int quantity, long payout) {
        cycle(server);
        if (account == null || !policy.sellable() || quantity <= 0 || payout <= 0) return new QuoteAllowance(0, 0L, "blocked");
        long personal = policy.disc() || testQuotaBypass(server) ? Long.MAX_VALUE : accountRemaining(server, account), global = policy.disc() || testQuotaBypass(server) ? Long.MAX_VALUE : globalRemaining(server);
        boolean fits = quantity <= accountItemRemaining(server, account, policy) && quantity <= globalItemRemaining(server, policy);
        if (personal < payout || global < payout || !fits) return new QuoteAllowance(0, 0L,
                !fits ? "item_cap" : personal < payout ? "account_daily_cap" : "global_daily_cap");
        return new QuoteAllowance(quantity, payout, "ok");
    }

    public synchronized boolean commit(MinecraftServer server, String operationKey, UUID account, Map<ResourceLocation, Integer> units, long payout) {
        return commitPolicies(server,operationKey,account,units,payout,Map.of());
    }
    /** Native merchandise has a distinct exact allowlist; never re-resolve it as global NONE after payment. */
    public synchronized boolean commitNative(MinecraftServer server,String operationKey,UUID account,ItemPolicy policy,int quantity,long payout) {
        if(policy==null||!policy.sellable()||quantity<=0
                ||!com.siirio.jemeconomy.ledger.EconomySavedData.get(server).hasReceipt(operationKey+":payment"))return false;
        return commitPolicies(server,operationKey,account,Map.of(policy.item(),quantity),payout,Map.of(policy.item(),policy));
    }
    private boolean commitPolicies(MinecraftServer server,String operationKey,UUID account,Map<ResourceLocation,Integer> units,long payout,Map<ResourceLocation,ItemPolicy> exact) {
        cycle(server);
        if (committed.containsKey(operationKey)) return true;
        if (operationKey == null || account == null || payout <= 0L) return false;
        long ordinary = payout;
        long discs = 0L;
        Map<ResourceLocation,Integer> families = new HashMap<>();
        for (var entry : units.entrySet()) {
            var policy = exact.getOrDefault(entry.getKey(),com.siirio.jemeconomy.policy.ItemPolicyRegistry.policy(entry.getKey()));
            if (entry.getValue() <= 0) return false;
            if (policy.disc()) discs = Math.addExact(discs, Math.multiplyExact((long) entry.getValue(), 10_000L));
            if (policy.disc()) families.merge(policy.quotaFamily(), Math.multiplyExact(entry.getValue(),policy.quotaUnits()), Math::addExact);
        }
        ordinary = Math.max(0L, ordinary - discs);
        if (!testQuotaBypass(server) && (ordinary > accountRemaining(server,account) || ordinary > globalRemaining(server))) return false;
        Map<ResourceLocation, Integer> perAccount = accountUnits.computeIfAbsent(account, ignored -> new HashMap<>());
        for (var entry : units.entrySet()) {
            var policy = exact.getOrDefault(entry.getKey(),com.siirio.jemeconomy.policy.ItemPolicyRegistry.policy(entry.getKey()));
            int used = families.getOrDefault(policy.quotaFamily(), 0);
            if (policy.disc() && (used > policy.dailyPlayerUnits() - perAccount.getOrDefault(policy.quotaFamily(),0)
                    || used > policy.dailyGlobalUnits() - globalUnits.getOrDefault(policy.quotaFamily(),0))) return false;
        }
        globalPaid = Math.addExact(globalPaid,ordinary);
        accountPaid.merge(account, ordinary, Math::addExact);
        families.forEach((id,count) -> { globalUnits.merge(id,count,Math::addExact); perAccount.merge(id,count,Math::addExact); });
        committed.put(operationKey, Boolean.TRUE);
        revision++;
        while (committed.size() > 50_000) committed.remove(committed.keySet().iterator().next());
        setDirty();
        return true;
    }

    public synchronized long accountRemaining(MinecraftServer server, UUID account) { cycle(server); return Long.MAX_VALUE; }
    public synchronized long globalRemaining(MinecraftServer server) { cycle(server); return Long.MAX_VALUE; }
    public synchronized long accountFamilyRemaining(MinecraftServer server, UUID account, ItemPolicy policy) {
        cycle(server); if(!policy.disc()) return Integer.MAX_VALUE; return Math.max(0,policy.dailyPlayerUnits()-accountUnits.getOrDefault(account,Map.of()).getOrDefault(policy.quotaFamily(),0));
    }
    public synchronized long globalFamilyRemaining(MinecraftServer server, ItemPolicy policy) {
        cycle(server); if(!policy.disc()) return Integer.MAX_VALUE; return Math.max(0,policy.dailyGlobalUnits()-globalUnits.getOrDefault(policy.quotaFamily(),0));
    }
    public synchronized int accountItemRemaining(MinecraftServer server, UUID account, ItemPolicy policy) {
        cycle(server);
        return Math.toIntExact(accountFamilyRemaining(server,account,policy) / policy.quotaUnits());
    }
    public synchronized int globalItemRemaining(MinecraftServer server, ItemPolicy policy) {
        cycle(server);
        return Math.toIntExact(globalFamilyRemaining(server,policy) / policy.quotaUnits());
    }
    public synchronized long revision(MinecraftServer server) { cycle(server); return revision; }
    /** Daily supplier stock is distinct from the NPC's budget and purchases from players. */
    public synchronized int purchaseRemaining(MinecraftServer server, UUID account, ItemPolicy policy) {
        cycle(server);
        if (!policy.disc()) return Integer.MAX_VALUE;
        boolean building = policy.marketCategory() == ItemPolicy.MarketCategory.BLOCKS;
        int personalLimit = building ? Math.max(65536,policy.dailyPlayerUnits()) : policy.dailyPlayerUnits();
        int stockLimit = building ? Math.max(262144,policy.dailyGlobalUnits()) : policy.dailyGlobalUnits();
        int personal = Math.max(0,personalLimit-accountPurchases.getOrDefault(account,Map.of()).getOrDefault(policy.item(),0));
        int stock = Math.max(0,stockLimit-purchasedUnits.getOrDefault(policy.item(),0));
        return Math.min(personal, stock);
    }
    public synchronized void recordPurchase(MinecraftServer server, UUID account, ItemPolicy policy, int quantity) {
        if (quantity <= 0 || purchaseRemaining(server, account, policy) < quantity)
            throw new IllegalStateException("Market supply changed during synchronous purchase");
        if (!policy.disc()) { revision++; setDirty(); return; }
        purchasedUnits.merge(policy.item(), quantity, Math::addExact);
        accountPurchases.computeIfAbsent(account, ignored -> new HashMap<>()).merge(policy.item(), quantity, Math::addExact);
        revision++; setDirty();
    }
    public synchronized int multiplier(MinecraftServer server, String group) {
        Demand demand = demand(server); return demand.primary().equals(group) ? 150 : demand.secondary().equals(group) ? 125 : 100;
    }

    private void begin(long cycle) {
        activeCycle = cycle; globalPaid = 0L; accountPaid.clear(); globalUnits.clear(); accountUnits.clear();
        purchasedUnits.clear(); accountPurchases.clear(); revision++; setDirty();
    }
    private static int mix(long value) { value ^= value >>> 33; value *= 0xff51afd7ed558ccdl; value ^= value >>> 33; return (int) (value ^ value >>> 32); }

    @Override public synchronized CompoundTag save(CompoundTag tag) {
        tag.putInt("QuotaSchema",2);
        tag.putLong("Cycle", activeCycle); tag.putLong("Seed", seed); tag.putLong("GlobalPaid", globalPaid); tag.putLong("Revision", revision);
        CompoundTag supply = new CompoundTag(); purchasedUnits.forEach((id, count) -> supply.putInt(id.toString(), count)); tag.put("PurchasedUnits", supply);
        ListTag buyers = new ListTag(); accountPurchases.forEach((account, values) -> {
            CompoundTag row = new CompoundTag(); row.putUUID("Account", account); CompoundTag counts = new CompoundTag();
            values.forEach((id, count) -> counts.putInt(id.toString(), count)); row.put("Values", counts); buyers.add(row);
        }); tag.put("AccountPurchases", buyers);
        CompoundTag paid = new CompoundTag(); accountPaid.forEach((id, value) -> paid.putLong(id.toString(), value)); tag.put("AccountPaid", paid);
        CompoundTag gu = new CompoundTag(); globalUnits.forEach((id, value) -> gu.putInt(id.toString(), value)); tag.put("GlobalUnits", gu);
        ListTag au = new ListTag(); accountUnits.forEach((account, rows) -> { CompoundTag row = new CompoundTag(); row.putUUID("Account", account); CompoundTag values = new CompoundTag(); rows.forEach((id, count) -> values.putInt(id.toString(), count)); row.put("Values", values); au.add(row); }); tag.put("AccountUnits", au);
        ListTag done = new ListTag(); committed.keySet().forEach(key -> { CompoundTag row = new CompoundTag(); row.putString("Key", key); done.add(row); }); tag.put("Committed", done);
        return tag;
    }
    private static MarketSavedData load(CompoundTag tag) {
        MarketSavedData data = new MarketSavedData(); data.activeCycle = tag.contains("Cycle") ? tag.getLong("Cycle") : Long.MIN_VALUE; data.seed = tag.contains("Seed") ? tag.getLong("Seed") : data.seed; data.globalPaid = tag.getLong("GlobalPaid"); data.revision = tag.getLong("Revision");
        CompoundTag paid = tag.getCompound("AccountPaid"); paid.getAllKeys().forEach(key -> { try { data.accountPaid.put(UUID.fromString(key), paid.getLong(key)); } catch (IllegalArgumentException ignored) { } });
        CompoundTag gu = tag.getCompound("GlobalUnits"); gu.getAllKeys().forEach(key -> { ResourceLocation id = ResourceLocation.tryParse(key); if (id != null) data.globalUnits.put(id, gu.getInt(key)); });
        ListTag au = tag.getList("AccountUnits", Tag.TAG_COMPOUND); for (int i = 0; i < au.size(); i++) { CompoundTag row = au.getCompound(i); if (!row.hasUUID("Account")) continue; Map<ResourceLocation, Integer> values = new HashMap<>(); CompoundTag raw = row.getCompound("Values"); raw.getAllKeys().forEach(key -> { ResourceLocation id = ResourceLocation.tryParse(key); if (id != null) values.put(id, raw.getInt(key)); }); data.accountUnits.put(row.getUUID("Account"), values); }
        ListTag done = tag.getList("Committed", Tag.TAG_COMPOUND); for (int i = 0; i < done.size(); i++) data.committed.put(done.getCompound(i).getString("Key"), Boolean.TRUE);
        CompoundTag supply = tag.getCompound("PurchasedUnits"); supply.getAllKeys().forEach(key -> {
            ResourceLocation id = ResourceLocation.tryParse(key); if (id != null) data.purchasedUnits.put(id, Math.max(0, supply.getInt(key)));
        });
        ListTag buyers = tag.getList("AccountPurchases", Tag.TAG_COMPOUND);
        for (int i = 0; i < buyers.size(); i++) {
            CompoundTag row = buyers.getCompound(i); if (!row.hasUUID("Account")) continue;
            Map<ResourceLocation, Integer> counts = new HashMap<>(); CompoundTag values = row.getCompound("Values");
            values.getAllKeys().forEach(key -> { ResourceLocation id = ResourceLocation.tryParse(key); if (id != null) counts.put(id, Math.max(0, values.getInt(key))); });
            data.accountPurchases.put(row.getUUID("Account"), counts);
        }
        if(tag.getInt("QuotaSchema")<2) {
            normalizeLegacyUnits(data.globalUnits);
            data.accountUnits.values().forEach(MarketSavedData::normalizeLegacyUnits);
            data.setDirty();
        }
        return data;
    }
    /** Legacy counters count exact goods. Convert once without resetting cycle, payouts or supplier stock. */
    private static void normalizeLegacyUnits(Map<ResourceLocation,Integer> counters) {
        Map<ResourceLocation,Integer> converted=new HashMap<>();
        for(var entry:counters.entrySet()) {
            if(entry.getValue()<0)throw new IllegalStateException("Negative saved market quota count");
            var policy=com.siirio.jemeconomy.policy.ItemPolicyRegistry.policy(entry.getKey());
            if(policy.outcome()==ItemPolicy.Outcome.NONE) {
                var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.getKey());
                if(item!=null)policy=com.siirio.jemeconomy.merchant.NativeItemPricing.policy(new net.minecraft.world.item.ItemStack(item)).orElse(policy);
            }
            // Unregistered historical goods retain their original identity and count; no invented alias/reset.
            converted.merge(policy.quotaFamily(),Math.multiplyExact(entry.getValue(),policy.quotaUnits()),Math::addExact);
        }
        counters.clear();counters.putAll(converted);
    }

    public record Demand(long cycle, String primary, String secondary) { }
    public record QuoteAllowance(int units, long payout, String reason) { }
}
