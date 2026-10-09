package com.siirio.jemworldbosstiers.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record EncounterData(
        ResourceLocation profileKey,
        int tier,
        EncounterProvenance provenance,
        boolean progressionEligible,
        boolean rematch,
        String arenaId,
        Set<UUID> participants
) {
    public static final String ROOT_KEY = "jem_world_boss_tiers";
    private static final String PROFILE_KEY = "Profile";
    private static final String TIER_KEY = "EncounterTier";
    private static final String PROVENANCE_KEY = "Provenance";
    private static final String ELIGIBLE_KEY = "ProgressionEligible";
    private static final String REMATCH_KEY = "Rematch";
    private static final String ARENA_KEY = "ArenaId";
    private static final String PARTICIPANTS_KEY = "Participants";

    public EncounterData {
        participants = Set.copyOf(participants);
    }

    public boolean canProgressUnique() {
        return provenance != EncounterProvenance.RAID_EVENT && progressionEligible && !rematch && !participants.isEmpty();
    }

    public static EncounterData create(CompoundTag entityData, ResourceLocation profileKey, int tier, EncounterProvenance provenance, boolean progressionEligible, boolean rematch, String arenaId) {
        Optional<EncounterData> existing = read(entityData);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (tier < 1 || tier > 5) {
            throw new IllegalArgumentException("Encounter tier must be between one and five");
        }
        CompoundTag tag = new CompoundTag();
        tag.putString(PROFILE_KEY, profileKey.toString());
        tag.putInt(TIER_KEY, tier);
        tag.putString(PROVENANCE_KEY, provenance.name());
        tag.putBoolean(ELIGIBLE_KEY, progressionEligible);
        tag.putBoolean(REMATCH_KEY, rematch);
        if (arenaId != null && !arenaId.isBlank()) {
            tag.putString(ARENA_KEY, arenaId);
        }
        tag.put(PARTICIPANTS_KEY, new ListTag());
        entityData.put(ROOT_KEY, tag);
        return read(entityData).orElseThrow();
    }

    public static void addParticipant(CompoundTag entityData, UUID participant) {
        CompoundTag tag = entityData.getCompound(ROOT_KEY);
        if (!tag.contains(PROFILE_KEY)) {
            return;
        }
        Set<UUID> participants = readParticipants(tag);
        if (participants.add(participant)) {
            ListTag values = new ListTag();
            participants.forEach(id -> values.add(StringTag.valueOf(id.toString())));
            tag.put(PARTICIPANTS_KEY, values);
            entityData.put(ROOT_KEY, tag);
        }
    }

    public static EncounterData convertToRaid(CompoundTag entityData) {
        CompoundTag tag=entityData.getCompound(ROOT_KEY);
        if(!tag.contains(PROFILE_KEY)||!tag.contains(TIER_KEY)) throw new IllegalStateException("Encounter must be created before raid conversion");
        tag.putString(PROVENANCE_KEY,EncounterProvenance.RAID_EVENT.name());
        tag.putBoolean(ELIGIBLE_KEY,false);
        tag.putBoolean(REMATCH_KEY,true);
        entityData.put(ROOT_KEY,tag);
        return read(entityData).orElseThrow();
    }

    public static EncounterData verify(CompoundTag entityData, EncounterProvenance provenance, String arenaId) {
        EncounterData existing = read(entityData).orElseThrow(() -> new IllegalStateException("Encounter must be created before verification"));
        if (existing.rematch() || existing.provenance() == EncounterProvenance.RAID_EVENT) {
            return existing;
        }
        CompoundTag tag = entityData.getCompound(ROOT_KEY);
        tag.putString(PROVENANCE_KEY, provenance.name());
        tag.putBoolean(ELIGIBLE_KEY, provenance != EncounterProvenance.UNVERIFIED && provenance != EncounterProvenance.REVIVAL);
        if (arenaId != null && !arenaId.isBlank()) {
            tag.putString(ARENA_KEY, arenaId);
        }
        entityData.put(ROOT_KEY, tag);
        return read(entityData).orElseThrow();
    }

    public static EncounterData classify(CompoundTag entityData, EncounterProvenance provenance, boolean progressionEligible, boolean rematch, String arenaId) {
        EncounterData existing = read(entityData).orElseThrow(() -> new IllegalStateException("Encounter must be created before classification"));
        if (existing.provenance() != EncounterProvenance.UNVERIFIED) {
            return existing;
        }
        CompoundTag tag = entityData.getCompound(ROOT_KEY);
        tag.putString(PROVENANCE_KEY, provenance.name());
        tag.putBoolean(ELIGIBLE_KEY, progressionEligible && !rematch);
        tag.putBoolean(REMATCH_KEY, rematch);
        if (arenaId != null && !arenaId.isBlank()) {
            tag.putString(ARENA_KEY, arenaId);
        }
        entityData.put(ROOT_KEY, tag);
        return read(entityData).orElseThrow();
    }

    public static Optional<EncounterData> read(CompoundTag entityData) {
        CompoundTag tag = entityData.getCompound(ROOT_KEY);
        if (!tag.contains(PROFILE_KEY) || !tag.contains(TIER_KEY)) {
            return Optional.empty();
        }
        ResourceLocation profileKey = ResourceLocation.tryParse(tag.getString(PROFILE_KEY));
        if (profileKey == null) {
            return Optional.empty();
        }
        EncounterProvenance provenance;
        try {
            provenance = EncounterProvenance.valueOf(tag.getString(PROVENANCE_KEY));
        } catch (IllegalArgumentException exception) {
            provenance = EncounterProvenance.UNVERIFIED;
        }
        String arenaId = tag.contains(ARENA_KEY) ? tag.getString(ARENA_KEY) : null;
        return Optional.of(new EncounterData(profileKey, tag.getInt(TIER_KEY), provenance, tag.getBoolean(ELIGIBLE_KEY), tag.getBoolean(REMATCH_KEY), arenaId, readParticipants(tag)));
    }

    private static Set<UUID> readParticipants(CompoundTag tag) {
        Set<UUID> participants = new LinkedHashSet<>();
        ListTag values = tag.getList(PARTICIPANTS_KEY, Tag.TAG_STRING);
        for (Tag value : values) {
            try {
                participants.add(UUID.fromString(value.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return participants;
    }
}
