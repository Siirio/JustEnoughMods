package com.siirio.jemcompat.gate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CampaignSavedData extends SavedData {
    private static final String FILE_ID = "jemcompat_main_campaign";
    private final Set<ResourceLocation> defeatedBosses = new LinkedHashSet<>();
    private final Set<ResourceLocation> defeatedPrerequisites = new LinkedHashSet<>();
    private final Set<ResourceLocation> unlockedBosses = new LinkedHashSet<>();
    private final Map<UUID, List<ItemStack>> protectedDeathItems = new LinkedHashMap<>();
    private final Map<ResourceLocation, BlockPos> campaignSites = new LinkedHashMap<>();
    private final Map<ResourceLocation, UUID> campaignSiteEntities = new LinkedHashMap<>();
    private final Map<String, BlockPos> locatedStructures = new LinkedHashMap<>();
    private final Set<UUID> secretEndingPlayers = new LinkedHashSet<>();
    private final Set<ResourceLocation> postEndDefeats = new LinkedHashSet<>();
    private boolean postEndRevealed;
    private long gateRevision;

    public static CampaignSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CampaignSavedData::load, CampaignSavedData::new, FILE_ID);
    }

    public boolean recordDefeat(ResourceLocation boss) {
        boolean added = defeatedBosses.add(boss);
        if (added) {
            gateRevision++;
            setDirty();
        }
        return added;
    }

    public boolean defeated(ResourceLocation boss) {
        return defeatedBosses.contains(boss);
    }

    public boolean recordPrerequisite(ResourceLocation boss) {
        boolean added = defeatedPrerequisites.add(boss);
        if (added) {
            gateRevision++;
            setDirty();
        }
        return added;
    }

    public boolean prerequisiteDefeated(CampaignPrerequisite prerequisite) {
        return defeatedPrerequisites.contains(prerequisite.entity());
    }

    public boolean unlock(CampaignBoss boss) {
        boolean added = unlockedBosses.add(boss.entity());
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean unlocked(CampaignBoss boss) {
        return boss.prerequisites().isEmpty() || boss.prerequisites().stream().allMatch(this::prerequisiteDefeated);
    }

    public long gateRevision() {
        return gateRevision;
    }

    public BlockPos campaignSite(CampaignPrerequisite prerequisite) {
        return campaignSites.get(prerequisite.entity());
    }

    public void campaignSite(CampaignPrerequisite prerequisite, BlockPos position) {
        campaignSites.put(prerequisite.entity(), position.immutable());
        setDirty();
    }

    public UUID campaignSiteEntity(CampaignPrerequisite prerequisite) {
        return campaignSiteEntities.get(prerequisite.entity());
    }

    public void campaignSiteEntity(CampaignPrerequisite prerequisite, UUID entity) {
        campaignSiteEntities.put(prerequisite.entity(), entity);
        setDirty();
    }

    public BlockPos locatedStructure(String target) {
        return locatedStructures.get(target);
    }

    public void locatedStructure(String target, BlockPos position) {
        locatedStructures.put(target, position.immutable());
        setDirty();
    }

    public boolean startSecretEnding(UUID player) {
        boolean added = secretEndingPlayers.add(player);
        if (added) setDirty();
        return added;
    }

    public boolean revealPostEnd() {
        if (postEndRevealed) return false;
        postEndRevealed = true;
        setDirty();
        return true;
    }

    public boolean postEndRevealed() { return postEndRevealed; }

    public boolean recordPostEndDefeat(ResourceLocation entity) {
        boolean added = postEndDefeats.add(entity);
        if (added) setDirty();
        return added;
    }

    public boolean postEndDefeated(ResourceLocation entity) { return postEndDefeats.contains(entity); }

    public void protectDeathItems(UUID player, List<ItemStack> items) {
        protectedDeathItems.computeIfAbsent(player, ignored -> new ArrayList<>()).addAll(items);
        setDirty();
    }

    public List<ItemStack> takeProtectedDeathItems(UUID player) {
        List<ItemStack> items = protectedDeathItems.remove(player);
        if (items != null) {
            setDirty();
        }
        return items == null ? List.of() : items;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag defeats = new ListTag();
        defeatedBosses.forEach(id -> defeats.add(StringTag.valueOf(id.toString())));
        tag.put("DefeatedBosses", defeats);

        ListTag prerequisites = new ListTag();
        defeatedPrerequisites.forEach(id -> prerequisites.add(StringTag.valueOf(id.toString())));
        tag.put("DefeatedPrerequisites", prerequisites);

        ListTag unlocked = new ListTag();
        unlockedBosses.forEach(id -> unlocked.add(StringTag.valueOf(id.toString())));
        tag.put("UnlockedBosses", unlocked);

        ListTag protectedItems = new ListTag();
        protectedDeathItems.forEach((player, items) -> {
            CompoundTag value = new CompoundTag();
            value.putUUID("Player", player);
            ListTag stacks = new ListTag();
            items.forEach(stack -> stacks.add(stack.save(new CompoundTag())));
            value.put("Items", stacks);
            protectedItems.add(value);
        });
        tag.put("ProtectedDeathItems", protectedItems);

        ListTag sites = new ListTag();
        campaignSites.forEach((entity, position) -> {
            CompoundTag value = new CompoundTag();
            value.putString("Entity", entity.toString());
            value.putLong("Position", position.asLong());
            UUID spawnedEntity = campaignSiteEntities.get(entity);
            if (spawnedEntity != null) {
                value.putUUID("SpawnedEntity", spawnedEntity);
            }
            sites.add(value);
        });
        tag.put("CampaignSites", sites);

        ListTag locations = new ListTag();
        locatedStructures.forEach((target, position) -> {
            CompoundTag value = new CompoundTag();
            value.putString("Target", target);
            value.putLong("Position", position.asLong());
            locations.add(value);
        });
        tag.put("LocatedStructures", locations);
        ListTag endings = new ListTag();
        secretEndingPlayers.forEach(id -> endings.add(StringTag.valueOf(id.toString())));
        tag.put("SecretEndingPlayers", endings);
        ListTag postEnd = new ListTag();
        postEndDefeats.forEach(id -> postEnd.add(StringTag.valueOf(id.toString())));
        tag.put("PostEndDefeats", postEnd);
        tag.putBoolean("PostEndRevealed", postEndRevealed);
        return tag;
    }

    private static CampaignSavedData load(CompoundTag tag) {
        CampaignSavedData data = new CampaignSavedData();
        for (Tag value : tag.getList("DefeatedBosses", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) {
                data.defeatedBosses.add(id);
            }
        }
        for (Tag value : tag.getList("DefeatedPrerequisites", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) {
                data.defeatedPrerequisites.add(id);
            }
        }
        for (Tag value : tag.getList("UnlockedBosses", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) {
                data.unlockedBosses.add(id);
            }
        }
        for (Tag value : tag.getList("ProtectedDeathItems", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            List<ItemStack> items = new ArrayList<>();
            for (Tag stack : entry.getList("Items", Tag.TAG_COMPOUND)) {
                items.add(ItemStack.of((CompoundTag) stack));
            }
            data.protectedDeathItems.put(entry.getUUID("Player"), items);
        }
        for (Tag value : tag.getList("CampaignSites", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            ResourceLocation entity = ResourceLocation.tryParse(entry.getString("Entity"));
            if (entity == null) {
                continue;
            }
            data.campaignSites.put(entity, BlockPos.of(entry.getLong("Position")));
            if (entry.hasUUID("SpawnedEntity")) {
                data.campaignSiteEntities.put(entity, entry.getUUID("SpawnedEntity"));
            }
        }
        for (Tag value : tag.getList("LocatedStructures", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            data.locatedStructures.put(entry.getString("Target"), BlockPos.of(entry.getLong("Position")));
        }
        for (Tag value : tag.getList("SecretEndingPlayers", Tag.TAG_STRING)) {
            try { data.secretEndingPlayers.add(UUID.fromString(value.getAsString())); } catch (IllegalArgumentException ignored) { }
        }
        for (Tag value : tag.getList("PostEndDefeats", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) data.postEndDefeats.add(id);
        }
        data.postEndRevealed = tag.getBoolean("PostEndRevealed");
        return data;
    }
}
