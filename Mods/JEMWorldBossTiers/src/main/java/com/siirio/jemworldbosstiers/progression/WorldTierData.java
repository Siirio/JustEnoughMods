package com.siirio.jemworldbosstiers.progression;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.TierValues;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;

public final class WorldTierData extends SavedData {
    private static final String FILE_ID = "jem_world_boss_tiers";
    private static final String SCHEMA_VERSION = "SchemaVersion";
    private static final int CURRENT_SCHEMA_VERSION = 2;
    private static final String DEFEATED_BOSSES = "DefeatedBosses";
    private static final String ARENAS = "Arenas";
    private static final String TIER_OVERRIDE = "TierOverride";
    private final Set<ResourceLocation> defeatedBosses = new LinkedHashSet<>();
    private final Map<String, ArenaRecord> arenas = new LinkedHashMap<>();
    private Integer tierOverride;

    public static WorldTierData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WorldTierData::load, WorldTierData::new, FILE_ID);
    }

    public int tier() {
        return tierOverride == null ? BalanceRegistry.worldTierForDefeats(defeatedBosses) : tierOverride;
    }

    public Optional<Integer> tierOverride() {
        return Optional.ofNullable(tierOverride);
    }

    public void setTierOverride(int tier) {
        if (tier < 1 || tier > TierValues.TIER_COUNT) throw new IllegalArgumentException("Tier must be between one and five");
        if (java.util.Objects.equals(tierOverride,tier)) return;
        tierOverride=tier;
        setDirty();
    }

    public void clearTierOverride() {
        if(tierOverride==null) return;
        tierOverride=null;
        setDirty();
    }

    public boolean recordDefeat(ResourceLocation bossKey) {
        if (!defeatedBosses.add(bossKey)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Set<ResourceLocation> defeatedBosses() {
        return Set.copyOf(defeatedBosses);
    }

    public int activeDefeatCount() {
        return ProgressionRules.activeDefeatCount(defeatedBosses, BalanceRegistry.activeQualifyingKeys());
    }

    public void putArena(ArenaRecord arena) {
        arenas.put(arena.id(), arena);
        setDirty();
    }

    public Optional<ArenaRecord> arena(String id) {
        return Optional.ofNullable(arenas.get(id));
    }

    public Collection<ArenaRecord> arenas() {
        return List.copyOf(arenas.values());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
        if(tierOverride!=null) tag.putInt(TIER_OVERRIDE,tierOverride);
        ListTag values = new ListTag();
        defeatedBosses.forEach(id -> values.add(StringTag.valueOf(id.toString())));
        tag.put(DEFEATED_BOSSES, values);
        ListTag arenaValues = new ListTag();
        arenas.values().forEach(arena -> arenaValues.add(arena.save()));
        tag.put(ARENAS, arenaValues);
        return tag;
    }

    static WorldTierData load(CompoundTag tag) {
        WorldTierData data = new WorldTierData();
        if (tag.getInt(SCHEMA_VERSION) != CURRENT_SCHEMA_VERSION) {
            return data;
        }
        if(tag.contains(TIER_OVERRIDE,Tag.TAG_INT)) data.tierOverride=tag.getInt(TIER_OVERRIDE);
        ListTag values = tag.getList(DEFEATED_BOSSES, Tag.TAG_STRING);
        for (Tag value : values) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) {
                data.defeatedBosses.add(id);
            }
        }
        ListTag arenaValues = tag.getList(ARENAS, Tag.TAG_COMPOUND);
        for (Tag value : arenaValues) {
            ArenaRecord.load((CompoundTag) value).ifPresent(arena -> data.arenas.put(arena.id(), arena));
        }
        return data;
    }
}
