package com.siirio.jemprofessions.sandbox;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import com.siirio.jemeconomy.sandbox.SandboxDurability;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Only interrupted commerce receipts, removed after commit. Puffish remains the live XP owner. */
public final class SandboxCreditJournal extends SavedData {
    private static final String NAME = "jem_profession_xp_journal";
    private final Map<UUID, Pending> pending = new HashMap<>();
    private static SandboxCreditJournal get(ServerPlayer player) {
        return player.getServer().overworld().getDataStorage().computeIfAbsent(SandboxCreditJournal::load, SandboxCreditJournal::new, NAME);
    }
    static void prepare(ServerPlayer player, ProfessionId profession, UUID audit, int amount, String counter, long day) {
        SandboxCreditJournal journal = get(player);
        if (journal.pending.containsKey(player.getUUID())) throw new IllegalStateException("Unrecovered profession credit");
        long target = Math.min(Integer.MAX_VALUE, ProfessionApi.xp(player, profession) + amount);
        String counterKey = counter == null ? "" : counter;
        journal.pending.put(player.getUUID(), new Pending(profession, audit, target, counterKey, day,
                counterKey.isEmpty() ? 0 : SandboxProfessionSupport.record(player).dailyAmount(counterKey, day)));
        journal.setDirty();
        SandboxDurability.save(player.getServer(), journal, NAME);
    }
    public static void recover(ServerPlayer player) {
        if (!SandboxProfessionSupport.allowed(player)) return;
        SandboxCreditJournal journal = get(player);
        Pending receipt = journal.pending.get(player.getUUID());
        if (receipt == null) return;
        var experience = ProfessionApi.category(receipt.profession).flatMap(net.puffish.skillsmod.api.Category::getExperience);
        if (experience.isEmpty()) throw new IllegalStateException("Missing native experience for pending profession credit");
        var nativeXp = experience.get();
        nativeXp.setTotal(player, (int) Math.min(Integer.MAX_VALUE, Math.max(nativeXp.getTotal(player), receipt.target)));
        SandboxProfessionSupport.record(player).reserveAudit(receipt.audit);
        if (!receipt.counter.isEmpty()) SandboxProfessionSupport.record(player).recoverDaily(receipt.counter, receipt.day, receipt.dailyTarget);
        ProfessionSavedData.get(player.getServer()).changed();
        flushOwners(player);
        journal.pending.remove(player.getUUID());
        journal.setDirty();
        SandboxDurability.save(player.getServer(), journal, NAME);
    }
    static void flushOwners(ServerPlayer player) {
        SandboxDurability.save(player.getServer(), net.puffish.skillsmod.server.data.ServerData.getOrCreate(player.getServer()), "puffish_skills");
        SandboxDurability.save(player.getServer(), ProfessionSavedData.get(player.getServer()), "jem_professions");
    }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag rows = new CompoundTag();
        pending.forEach((id, receipt) -> {
            CompoundTag row = new CompoundTag(); row.putString("Profession", receipt.profession.name());
            row.putUUID("Audit", receipt.audit); row.putLong("Target", receipt.target);
            row.putString("Counter", receipt.counter); row.putLong("Day", receipt.day); row.putInt("DailyTarget", receipt.dailyTarget);
            rows.put(id.toString(), row);
        });
        tag.put("Pending", rows); return tag;
    }
    private static SandboxCreditJournal load(CompoundTag tag) {
        SandboxCreditJournal journal = new SandboxCreditJournal(); CompoundTag rows = tag.getCompound("Pending");
        for (String key : rows.getAllKeys()) {
            CompoundTag row = rows.getCompound(key);
            var profession = ProfessionId.parse(row.getString("Profession"));
            if (profession.isEmpty() || !row.hasUUID("Audit")) throw new IllegalStateException("Invalid pending profession credit");
            journal.pending.put(UUID.fromString(key), new Pending(profession.get(), row.getUUID("Audit"), Math.max(0, row.getLong("Target")),
                    row.getString("Counter"), row.getLong("Day"), Math.max(0,row.getInt("DailyTarget"))));
        }
        return journal;
    }
    private record Pending(ProfessionId profession, UUID audit, long target, String counter, long day, int dailyTarget) {}
}
