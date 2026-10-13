package com.siirio.jemprofessions.progress;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class ProfessionRecord {
    public static final int SCHEMA = 1;
    private static final int MAX_AUDITS = 2048;
    private final Map<ProfessionId, Long> xp = new EnumMap<>(ProfessionId.class);
    private final ArrayDeque<UUID> auditOrder = new ArrayDeque<>();
    private final Set<UUID> audits = new HashSet<>();

    public long xp(ProfessionId id) { return xp.getOrDefault(id, 0L); }
    public int level(ProfessionId id) { return ProfessionFormula.levelFor(xp(id)); }

    public boolean credit(ProfessionId id, int delta, UUID auditId) {
        if (delta <= 0 || auditId == null || !audits.add(auditId)) return false;
        auditOrder.addLast(auditId);
        while (auditOrder.size() > MAX_AUDITS) audits.remove(auditOrder.removeFirst());
        xp.merge(id, (long) delta, Math::addExact);
        return true;
    }

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
        return record;
    }
}
