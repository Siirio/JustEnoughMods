package com.siirio.jemcompat.gate;

import net.minecraft.resources.ResourceLocation;
import com.siirio.jemtwelveeyes.CampaignDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CampaignPrerequisite {
    private static final CampaignPrerequisite[] VALUES = load();

    private boolean aquatic;
    private boolean canceledDeathProof;
    private ResourceLocation floor;
    private ResourceLocation marker;
    private final String key;
    private final ResourceLocation entity;
    private final ResourceLocation dimension;
    private final int cartographerLevel;
    private final ResourceLocation nativeStructure;
    private final List<ResourceLocation> proofAdvancements;

    CampaignPrerequisite(String key, String entity, String dimension, int cartographerLevel, String nativeStructure, String... proofAdvancements) {
        this.key = key;
        this.entity = new ResourceLocation(entity);
        this.dimension = new ResourceLocation(dimension);
        this.cartographerLevel = cartographerLevel;
        this.nativeStructure = nativeStructure == null ? null : new ResourceLocation(nativeStructure);
        this.proofAdvancements = Arrays.stream(proofAdvancements).map(ResourceLocation::new).toList();
    }

    public String key() {
        return key;
    }

    public ResourceLocation entity() {
        return entity;
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    public int cartographerLevel() {
        return cartographerLevel;
    }

    public boolean generatedSite() {
        return nativeStructure == null;
    }

    public ResourceLocation structureTag() {
        return generatedSite() ? null : new ResourceLocation("jemcompat", "campaign/prerequisite/" + key);
    }

    public String hintKey() {
        return "message.jemcompat.campaign.hint." + key;
    }

    public List<ResourceLocation> proofAdvancements() {
        return proofAdvancements;
    }

    public static Optional<CampaignPrerequisite> byEntity(ResourceLocation entity) {
        return Arrays.stream(values()).filter(requirement -> requirement.entity.equals(entity)).findFirst();
    }

    public static Optional<CampaignPrerequisite> byProofAdvancement(ResourceLocation advancement) {
        return Arrays.stream(values()).filter(requirement -> requirement.proofAdvancements.contains(advancement)).findFirst();
    }
    public static CampaignPrerequisite[] values() {
        return VALUES.clone();
    }

    public static CampaignPrerequisite byKey(String key) {
        return Arrays.stream(VALUES).filter(value -> value.key.equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown prerequisite " + key));
    }

    public boolean aquatic() { return aquatic; }
    public boolean canceledDeathProof() { return canceledDeathProof; }
    public ResourceLocation floor() { return floor; }
    public ResourceLocation marker() { return marker; }

    private static CampaignPrerequisite[] load() {
        var values = new java.util.ArrayList<CampaignPrerequisite>();
        var keys = new java.util.HashSet<String>();
        var entities = new java.util.HashSet<ResourceLocation>();
        for (var entry : CampaignDefinition.entries("prerequisites")) {
            var json = entry.getAsJsonObject();
            var proofs = new java.util.ArrayList<String>();
            json.getAsJsonArray("proof_advancements").forEach(value -> proofs.add(value.getAsString()));
            var structure = json.get("native_structure");
            var value = new CampaignPrerequisite(json.get("key").getAsString(), json.get("entity").getAsString(),
                    json.get("dimension").getAsString(), json.get("cartographer_level").getAsInt(),
                    structure == null || structure.isJsonNull() ? null : structure.getAsString(), proofs.toArray(String[]::new));
            value.aquatic = json.get("aquatic").getAsBoolean();
            value.canceledDeathProof = json.get("canceled_death_proof").getAsBoolean();
            value.floor = new ResourceLocation(json.get("floor").getAsString());
            value.marker = new ResourceLocation(json.get("marker").getAsString());
            if (!keys.add(value.key()) || !entities.add(value.entity())) {
                throw new IllegalArgumentException("Duplicate prerequisite key or entity: " + value.key());
            }
            values.add(value);
        }
        return values.toArray(CampaignPrerequisite[]::new);
    }
}
