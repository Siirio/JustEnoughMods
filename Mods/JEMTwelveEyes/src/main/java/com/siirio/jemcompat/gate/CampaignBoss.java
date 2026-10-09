package com.siirio.jemcompat.gate;

import net.minecraft.resources.ResourceLocation;
import com.siirio.jemtwelveeyes.CampaignDefinition;

import java.util.Arrays;
import java.util.Optional;

public final class CampaignBoss {
    private static final CampaignBoss[] VALUES = load();

    private final String key;
    private final ResourceLocation entity;
    private final ResourceLocation eye;
    private final ResourceLocation dimension;
    private final int cartographerLevel;
    private final java.util.List<CampaignPrerequisite> prerequisites;

    CampaignBoss(String key, String entity, String eye, String dimension, int cartographerLevel, CampaignPrerequisite... prerequisites) {
        this.key = key;
        this.entity = new ResourceLocation(entity);
        this.eye = new ResourceLocation(eye);
        this.dimension = new ResourceLocation(dimension);
        this.cartographerLevel = cartographerLevel;
        this.prerequisites = java.util.List.of(prerequisites);
    }

    public String key() {
        return key;
    }

    public ResourceLocation entity() {
        return entity;
    }

    public ResourceLocation eye() {
        return eye;
    }

    public ResourceLocation structureTag() {
        return new ResourceLocation("jemcompat", "campaign/" + key);
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    public int cartographerLevel() {
        return cartographerLevel;
    }

    public String mapNameKey() {
        return "item.jemcompat.campaign_map." + key;
    }

    public String mapLoreKey() {
        return mapNameKey() + ".lore";
    }

    public java.util.List<CampaignPrerequisite> prerequisites() {
        return prerequisites;
    }

    public ResourceLocation prerequisiteAdvancement(CampaignPrerequisite prerequisite) {
        return new ResourceLocation("jemcompat", "campaign/branches/" + key + "/" + prerequisite.key());
    }

    public ResourceLocation defeatAdvancement() {
        return new ResourceLocation("jemcompat", "defeat/" + key);
    }

    public static Optional<CampaignBoss> byEntity(ResourceLocation entity) {
        return Arrays.stream(values()).filter(boss -> boss.entity.equals(entity)).findFirst();
    }

    public static Optional<CampaignBoss> byEye(ResourceLocation eye) {
        return Arrays.stream(values()).filter(boss -> boss.eye.equals(eye)).findFirst();
    }
    public static CampaignBoss[] values() {
        return VALUES.clone();
    }

    private static CampaignBoss[] load() {
        var bosses = new java.util.ArrayList<CampaignBoss>();
        var keys = new java.util.HashSet<String>();
        var entities = new java.util.HashSet<ResourceLocation>();
        var eyes = new java.util.HashSet<ResourceLocation>();
        for (var entry : CampaignDefinition.entries("bosses")) {
            var json = entry.getAsJsonObject();
            var prerequisites = new java.util.ArrayList<CampaignPrerequisite>();
            json.getAsJsonArray("prerequisites").forEach(value -> prerequisites.add(CampaignPrerequisite.byKey(value.getAsString())));
            var boss = new CampaignBoss(json.get("key").getAsString(), json.get("entity").getAsString(),
                    json.get("eye").getAsString(), json.get("dimension").getAsString(),
                    json.get("cartographer_level").getAsInt(), prerequisites.toArray(CampaignPrerequisite[]::new));
            if (!keys.add(boss.key()) || !entities.add(boss.entity()) || !eyes.add(boss.eye())) {
                throw new IllegalArgumentException("Duplicate campaign key, entity or eye: " + boss.key());
            }
            bosses.add(boss);
        }
        return bosses.toArray(CampaignBoss[]::new);
    }
}
