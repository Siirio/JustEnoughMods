package com.siirio.jemsettlements.payroll;

import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemsettlements.currency.SettlementCurrency;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.HashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.npc.Villager;

/** Integrates existing resident values, never housing, blocks, paths or territory scans.
 * A day is 24,000 elapsed gameTime ticks: sleep and /time changes cannot fabricate income.
 * Missing resident observations earn nothing; partial presence scales the daily ceiling.
 * Daily averages are averaged separately before multiplication, never averaged products.
 */
public final class DailyTaxService {
    private static final long DAY = 24_000L;
    private DailyTaxService() { }

    /** Call every server tick after existing resident and comfort updates. */
    public static void tick(MinecraftServer server) {
        if (!SettlementAuthority.active(server)) return;
        var data = SettlementSavedData.get(server);
        long now = server.overworld().getGameTime();
        for (TownRecord town : data.towns()) observe(server, data, town, now);
    }

    public static void observe(MinecraftServer server, SettlementSavedData data, TownRecord town, long now) {
        CompoundTag state = town.dailyTax();
        if (!state.contains("LastTime")) {
            state.putLong("Day", Math.floorDiv(now, DAY)); state.putLong("LastTime", now);
            state.putBoolean("Coins", SettlementCurrency.coins()); state.put("Residents", new CompoundTag());
            state.put("Pending", new ListTag()); sample(server, data, town, state); data.changed(); return;
        }
        long previous = state.getLong("LastTime");
        if (now <= previous) return;
        // Only consecutive observed server ticks are evidence. A stopped server adds no gameTime;
        // unknown time jumps or missed callbacks are deliberately not extrapolated.
        if (now - previous == 1L) accumulate(state);
        long day = Math.floorDiv(now, DAY);
        if (day > state.getLong("Day")) {
            closeDay(town, state);
            state.putLong("Day", day); state.putBoolean("Coins", SettlementCurrency.coins());
            resetDay(state);
            state.putLong("LastTime", now); data.changed();
            // Intent contains fixed terms and the advanced day before the first currency mutation.
            TaxDurability.save(server, data, "jem_settlements");
            settlePending(server, data, town, state);
            publishMetrics(town, state);
        } else if (now % 1_200L == 0L && !state.getList("Pending", Tag.TAG_COMPOUND).isEmpty()) {
            state.putLong("LastTime", now); data.changed();
            TaxDurability.save(server, data, "jem_settlements");
            settlePending(server, data, town, state);
            publishMetrics(town, state);
        }
        sample(server, data, town, state);
        state.putLong("LastTime", now); data.changed();
    }

    private static void accumulate(CompoundTag state) {
        CompoundTag rows = state.getCompound("Residents");
        for (String id : rows.getAllKeys()) {
            CompoundTag row = rows.getCompound(id);
            if (!row.getBoolean("Active")) continue;
            row.putLong("Ticks", row.getLong("Ticks") + 1L);
            row.putDouble("HappinessIntegral", row.getDouble("HappinessIntegral") + row.getDouble("Happiness"));
            row.putDouble("SatietyIntegral", row.getDouble("SatietyIntegral") + row.getDouble("Satiety"));
            row.putDouble("TaxIntegral", row.getDouble("TaxIntegral") + state.getInt("Rate") / 15.0);
        }
        if (state.getBoolean("ComfortKnown")) {
            state.putLong("ComfortTicks", state.getLong("ComfortTicks") + 1L);
            state.putDouble("ComfortIntegral", state.getDouble("ComfortIntegral") + state.getDouble("Comfort"));
        }
    }

    private static void sample(MinecraftServer server, SettlementSavedData data, TownRecord town, CompoundTag state) {
        CompoundTag rows = state.getCompound("Residents");
        var current = new HashSet<String>();
        var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, town.dimension()));
        for (ResidentMood resident : data.residents(town.id())) {
            resident.advanceTime(server.overworld().getGameTime());
            String id = resident.villagerId().toString(); current.add(id);
            CompoundTag row = rows.getCompound(id);
            var entity = level == null ? null : level.getEntity(resident.villagerId());
            // UUID lookup sees only already loaded entities and never loads their chunk.
            boolean active = resident.living() && entity instanceof Villager villager && villager.isAlive();
            row.putBoolean("Active", active);
            if (active) { resident.advanceTime(server.overworld().getGameTime()); row.putDouble("Happiness", resident.exactMood()); row.putDouble("Satiety", resident.satiety()); }
            rows.put(id, row);
        }
        for (String id : rows.getAllKeys()) if (!current.contains(id)) rows.getCompound(id).putBoolean("Active", false);
        state.put("Residents", rows); state.putInt("Rate", town.taxPercent());
        // The comfort owner keeps its last trustworthy score across unavailable chunks.
        var comfort = town.observedComfort();
        state.putBoolean("ComfortKnown", comfort.isPresent());
        state.putDouble("Comfort", Math.max(0, Math.min(100, comfort.orElse(0))));
    }

    private static void closeDay(TownRecord town, CompoundTag state) {
        boolean coins = state.getBoolean("Coins");
        double residents = 0;
        CompoundTag rows = state.getCompound("Residents");
        for (String id : rows.getAllKeys()) {
            CompoundTag row = rows.getCompound(id); long ticks = row.getLong("Ticks");
            if (ticks <= 0) continue;
            double happiness = row.getDouble("HappinessIntegral") / ticks / 100.0;
            double satiety = row.getDouble("SatietyIntegral") / ticks / 100.0;
            double rate = row.getDouble("TaxIntegral") / ticks;
            residents += SettlementEconomyConfig.dailyResidentTax(coins) * happiness * satiety * rate * ticks / DAY;
        }
        double comfort = SettlementEconomyConfig.dailyComfortTax(coins) * state.getDouble("ComfortIntegral") / (100.0 * DAY);
        String suffix = coins ? "Copper" : "Emeralds";
        double residentTotal = residents + state.getDouble("ResidentRemainder" + suffix);
        double comfortTotal = comfort + state.getDouble("ComfortRemainder" + suffix);
        long residentPay = (long) Math.floor(residentTotal + 1.0e-10);
        long comfortPay = (long) Math.floor(comfortTotal + 1.0e-10);
        state.putDouble("ResidentRemainder" + suffix, Math.max(0, residentTotal - residentPay));
        state.putDouble("ComfortRemainder" + suffix, Math.max(0, comfortTotal - comfortPay));
        state.putLong("LastResidentIncome", 0L); state.putLong("LastComfortIncome", 0L); state.putLong("LastDailyTax", 0L);
        state.putLong("LastTaxDay", state.getLong("Day"));
        state.putLong("LastComfortObservedTicks", state.getLong("ComfortTicks"));
        if (residentPay + comfortPay > 0) {
            var pending = state.getList("Pending", Tag.TAG_COMPOUND);
            CompoundTag intent = new CompoundTag(); intent.putLong("Day", state.getLong("Day")); intent.putBoolean("Coins", coins);
            intent.putLong("Residents", residentPay); intent.putLong("Comfort", comfortPay); pending.add(intent); state.put("Pending", pending);
        }
    }

    private static void resetDay(CompoundTag state) {
        CompoundTag old = state.getCompound("Residents"), rows = new CompoundTag();
        for (String id : old.getAllKeys()) {
            CompoundTag row = old.getCompound(id);
            if (!row.getBoolean("Active")) continue;
            row.putLong("Ticks", 0L); row.putDouble("HappinessIntegral", 0); row.putDouble("SatietyIntegral", 0); row.putDouble("TaxIntegral", 0);
            rows.put(id, row);
        }
        state.put("Residents", rows); state.putLong("ComfortTicks", 0L); state.putDouble("ComfortIntegral", 0);
    }

    private static void settlePending(MinecraftServer server, SettlementSavedData data, TownRecord town, CompoundTag state) {
        ListTag pending = state.getList("Pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < pending.size();) {
            CompoundTag intent = pending.getCompound(i);
            if (intent.getBoolean("Coins") != SettlementCurrency.coins()) { i++; continue; }
            long residents = intent.getLong("Residents"), comfort = intent.getLong("Comfort");
            var result = SettlementCurrency.creditDailyTax(server, town.id(), Math.addExact(residents, comfort), intent.getLong("Day"));
            if (result.status() != TransferResult.Status.COMMITTED && result.status() != TransferResult.Status.REPLAYED) break;
            // The currency owner has durably stored both transfer and receipt before acknowledgement.
            state.putLong("LastResidentIncome", residents); state.putLong("LastComfortIncome", comfort); state.putLong("LastDailyTax", residents + comfort);
            state.putLong("LastPaidTaxDay", intent.getLong("Day")); pending.remove(i);
            state.put("Pending", pending); publishMetrics(town, state); data.changed();
            TaxDurability.save(server, data, "jem_settlements");
        }
    }

    private static void publishMetrics(TownRecord town, CompoundTag state) {
        CompoundTag metrics = town.metrics();
        metrics.putLong("LastResidentIncome", state.getLong("LastResidentIncome"));
        metrics.putLong("LastComfortIncome", state.getLong("LastComfortIncome"));
        metrics.putLong("LastDailyTax", state.getLong("LastDailyTax"));
        long pendingAmount = 0L;
        for (Tag tag : state.getList("Pending", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) tag;
            if (row.getBoolean("Coins") == SettlementCurrency.coins()) pendingAmount = Math.addExact(pendingAmount, row.getLong("Residents") + row.getLong("Comfort"));
        }
        metrics.putLong("TaxPending", pendingAmount); metrics.putLong("LastTaxDay", state.getLong("LastTaxDay"));
        metrics.putLong("LastPaidTaxDay", state.getLong("LastPaidTaxDay"));
        town.metrics(metrics);
    }
}
