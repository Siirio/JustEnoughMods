package com.siirio.jemsettlements.family;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Persistent identity and family state. Entity UUIDs are replaceable transport identities. */
public final class FamilyRecord {
    public enum Sex { MALE, FEMALE }
    public enum Orientation { STRAIGHT, SAME_SEX }

    private final UUID personId;
    private UUID entityId;
    private String name;
    private Sex sex;
    private Orientation orientation;
    private UUID spouse;
    private final Set<UUID> parents = new LinkedHashSet<>();
    private GlobalPos bed, chest, workstation;
    private boolean dead, eternalSingle, tableOrigin;
    private long remarriageAfter, nextBirth;
    private int happiness = 100;

    FamilyRecord(UUID personId, UUID entityId, String name, Sex sex, Orientation orientation) {
        this.personId = personId; this.entityId = entityId; this.name = name;
        this.sex = sex; this.orientation = orientation;
    }
    public UUID personId() { return personId; }
    public UUID entityId() { return entityId; }
    public String name() { return name; }
    public Sex sex() { return sex; }
    public Orientation orientation() { return orientation; }
    public UUID spouse() { return spouse; }
    public Set<UUID> parents() { return Set.copyOf(parents); }
    public GlobalPos bed() { return bed; }
    public GlobalPos chest() { return chest; }
    public GlobalPos workstation() { return workstation; }
    public boolean dead() { return dead; }
    public boolean eternalSingle() { return eternalSingle; }
    public boolean tableOrigin() { return tableOrigin; }
    public long remarriageAfter() { return remarriageAfter; }
    public long nextBirth() { return nextBirth; }
    public int happiness() { return happiness; }

    public void entity(UUID value) { entityId = value; dead = false; }
    public void name(String value) { if (value != null && !value.isBlank()) name = value; }
    public void spouse(UUID value) { spouse = value; }
    public void addParent(UUID value) { if (value != null) parents.add(value); }
    public void bed(GlobalPos value) { bed = value; }
    public void chest(GlobalPos value) { chest = value; }
    public void workstation(GlobalPos value) { workstation = value; }
    public void eternalSingle(boolean value) { eternalSingle = value; if (value) spouse = null; }
    public void tableOrigin(boolean value) { tableOrigin = value; }
    public void happiness(int value) { happiness = Math.max(0, Math.min(100, value)); }
    public void nextBirth(long value) { nextBirth = value; }
    public void died(long now, long cooldown) { dead = true; entityId = null; spouse = null; remarriageAfter = now + cooldown; }
    public void widowed(long now, long cooldown) { spouse = null; remarriageAfter = now + cooldown; }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putUUID("Person", personId);
        if (entityId != null) tag.putUUID("Entity", entityId); tag.putString("Name", name);
        tag.putString("Sex", sex.name()); tag.putString("Orientation", orientation.name());
        if (spouse != null) tag.putUUID("Spouse", spouse);
        ListTag family = new ListTag(); for (UUID parent : parents) { CompoundTag row = new CompoundTag(); row.putUUID("Id", parent); family.add(row); } tag.put("Parents", family);
        savePos(tag, "Bed", bed); savePos(tag, "Chest", chest); savePos(tag, "Work", workstation);
        tag.putBoolean("Dead", dead); tag.putBoolean("EternalSingle", eternalSingle); tag.putBoolean("TableOrigin", tableOrigin);
        tag.putLong("RemarriageAfter", remarriageAfter); tag.putLong("NextBirth", nextBirth); tag.putInt("Happiness", happiness); return tag;
    }
    static FamilyRecord load(CompoundTag tag) {
        FamilyRecord row = new FamilyRecord(tag.getUUID("Person"), tag.hasUUID("Entity") ? tag.getUUID("Entity") : null,
                tag.getString("Name"), parse(Sex.class, tag.getString("Sex"), Sex.MALE),
                parse(Orientation.class, tag.getString("Orientation"), Orientation.STRAIGHT));
        if (tag.hasUUID("Spouse")) row.spouse = tag.getUUID("Spouse");
        ListTag parents = tag.getList("Parents", Tag.TAG_COMPOUND); for (int i = 0; i < parents.size(); i++) if (parents.getCompound(i).hasUUID("Id")) row.parents.add(parents.getCompound(i).getUUID("Id"));
        row.bed = loadPos(tag, "Bed"); row.chest = loadPos(tag, "Chest"); row.workstation = loadPos(tag, "Work");
        row.dead = tag.getBoolean("Dead"); row.eternalSingle = tag.getBoolean("EternalSingle"); row.tableOrigin = tag.getBoolean("TableOrigin");
        row.remarriageAfter = tag.getLong("RemarriageAfter"); row.nextBirth = tag.getLong("NextBirth"); row.happiness = tag.contains("Happiness") ? tag.getInt("Happiness") : 100; return row;
    }
    private static <T extends Enum<T>> T parse(Class<T> type, String value, T fallback) { try { return Enum.valueOf(type, value); } catch (RuntimeException ignored) { return fallback; } }
    private static void savePos(CompoundTag tag, String key, GlobalPos value) { if (value == null) return; CompoundTag pos = new CompoundTag(); pos.putString("Dimension", value.dimension().location().toString()); pos.putLong("Pos", value.pos().asLong()); tag.put(key, pos); }
    private static GlobalPos loadPos(CompoundTag tag, String key) { if (!tag.contains(key, Tag.TAG_COMPOUND)) return null; CompoundTag pos = tag.getCompound(key); ResourceLocation id = ResourceLocation.tryParse(pos.getString("Dimension")); return id == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, id), net.minecraft.core.BlockPos.of(pos.getLong("Pos"))); }
}
