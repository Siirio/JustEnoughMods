package com.siirio.jemprofessions.progress;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class ProfessionRecord {
    public static final int SCHEMA = 2;
    private static final int MAX_AUDITS = 2048;
    private final Map<ProfessionId, Long> xp = new EnumMap<>(ProfessionId.class);
    private final ArrayDeque<UUID> auditOrder = new ArrayDeque<>();
    private final Set<UUID> audits = new HashSet<>();
    private final Set<String> discoveries = new LinkedHashSet<>();
    private final Set<String> toggles = new HashSet<>();
    private final Set<ProfessionId> puffishMigrations = java.util.EnumSet.noneOf(ProfessionId.class);
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, DailyCounter> daily = new HashMap<>();

    public long xp(ProfessionId id) { return xp.getOrDefault(id, 0L); }
    public int level(ProfessionId id) { return ProfessionFormula.levelFor(xp(id)); }
    public boolean claimDiscovery(String key) { return key != null && key.length() <= 256 && discoveries.size() < 8192 && discoveries.add(key); }
    public boolean discovered(String key) { return discoveries.contains(key); }
    public boolean toggle(String key) { if (!toggles.add(key)) { toggles.remove(key); return false; } return true; }
    public boolean enabled(String key) { return toggles.contains(key); }
    public Set<String> enabledKeys() { return Set.copyOf(toggles); }
    public long cooldown(String key) { return cooldowns.getOrDefault(key, 0L); }
    public void cooldown(String key, long until) { cooldowns.put(key, Math.max(0L, until)); }
    public int reserveDaily(String key, long day, int requested, int cap) {
        DailyCounter counter = daily.get(key);
        if (counter == null || counter.day != day) counter = new DailyCounter(day, 0);
        int grant = Math.max(0, Math.min(requested, Math.max(0, cap - counter.amount)));
        daily.put(key, new DailyCounter(day, counter.amount + grant));
        return grant;
    }
    public int dailyAmount(String key, long day) {
        DailyCounter counter = daily.get(key);
        return counter != null && counter.day == day ? counter.amount : 0;
    }
    public void recoverDaily(String key, long day, int amount) {
        DailyCounter counter = daily.get(key);
        if (counter == null || counter.day < day) daily.put(key, new DailyCounter(day, amount));
        else if (counter.day == day && counter.amount < amount) daily.put(key, new DailyCounter(day, amount));
    }

    public boolean reserveAudit(UUID auditId) {
        if (auditId == null || !audits.add(auditId)) return false;
        auditOrder.addLast(auditId);
        while (auditOrder.size() > MAX_AUDITS) audits.remove(auditOrder.removeFirst());
        return true;
    }

    public boolean hasAudit(UUID auditId) { return audits.contains(auditId); }

    public boolean markPuffishMigrated(ProfessionId id) { return puffishMigrations.add(id); }

    public void administrativeCredit(ProfessionId id, int delta) {
        if (delta > 0) xp.merge(id, (long) delta, Math::addExact);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Schema", SCHEMA);
        CompoundTag values = new CompoundTag();
        xp.forEach((id, value) -> values.putLong(id.name(), value));
        tag.put("Xp", values);
        ListTag ids = new ListTag();
        auditOrder.forEach(id -> ids.add(StringTag.valueOf(id.toString())));
        tag.put("Audits", ids);
        ListTag found = new ListTag();
        discoveries.forEach(value -> found.add(StringTag.valueOf(value)));
        tag.put("Discoveries", found);
        ListTag active = new ListTag();
        toggles.forEach(value -> active.add(StringTag.valueOf(value)));
        tag.put("Toggles", active);
        ListTag migrated = new ListTag();
        puffishMigrations.forEach(id -> migrated.add(StringTag.valueOf(id.name())));
        tag.put("PuffishMigrations", migrated);
        CompoundTag savedCooldowns = new CompoundTag();
        cooldowns.forEach(savedCooldowns::putLong);
        tag.put("Cooldowns", savedCooldowns);
        CompoundTag savedDaily = new CompoundTag();
        daily.forEach((key, value) -> { CompoundTag row = new CompoundTag(); row.putLong("Day", value.day); row.putInt("Amount", value.amount); savedDaily.put(key, row); });
        tag.put("Daily", savedDaily);
        return tag;
    }

    static ProfessionRecord load(CompoundTag tag) {
        ProfessionRecord record = new ProfessionRecord();
        CompoundTag values = tag.getCompound("Xp");
        for (String key : values.getAllKeys()) ProfessionId.parse(key).ifPresent(id -> record.xp.put(id, Math.max(0L, values.getLong(key))));
        ListTag ids = tag.getList("Audits", Tag.TAG_STRING);
        for (int i = Math.max(0, ids.size() - MAX_AUDITS); i < ids.size(); i++) {
            try {
                UUID id = UUID.fromString(ids.getString(i));
                if (record.audits.add(id)) record.auditOrder.addLast(id);
            } catch (IllegalArgumentException ignored) {
            }
        }
        ListTag found = tag.getList("Discoveries", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(8192, found.size()); i++) record.discoveries.add(found.getString(i));
        ListTag active = tag.getList("Toggles", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(256, active.size()); i++) record.toggles.add(active.getString(i));
        ListTag migrated = tag.getList("PuffishMigrations", Tag.TAG_STRING);
        for (int i = 0; i < migrated.size(); i++) ProfessionId.parse(migrated.getString(i)).ifPresent(record.puffishMigrations::add);
        CompoundTag savedCooldowns = tag.getCompound("Cooldowns");
        savedCooldowns.getAllKeys().forEach(key -> record.cooldowns.put(key, Math.max(0L, savedCooldowns.getLong(key))));
        CompoundTag savedDaily = tag.getCompound("Daily");
        savedDaily.getAllKeys().forEach(key -> { CompoundTag row = savedDaily.getCompound(key); record.daily.put(key, new DailyCounter(row.getLong("Day"), Math.max(0, row.getInt("Amount")))); });
        return record;
    }

    private record DailyCounter(long day, int amount) {}
}
