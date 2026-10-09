package com.siirio.jemcompat.discovery;

import com.siirio.jemcompat.gate.CampaignBoss;
import com.siirio.jemcompat.gate.CampaignPrerequisite;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record CampaignTarget(
        String selectionKey,
        String key,
        ResourceLocation entity,
        ResourceLocation dimension,
        int cartographerLevel,
        ResourceLocation structureTag,
        CampaignBoss mainBoss,
        CampaignPrerequisite prerequisite
) {
    private static final List<CampaignTarget> VALUES = createValues();

    public boolean generatedSite() {
        return prerequisite != null && prerequisite.generatedSite();
    }

    public String mapNameKey() {
        return "item.jemcompat.campaign_map." + key;
    }

    public static CampaignTarget main(CampaignBoss boss) {
        return new CampaignTarget(
                "main/" + boss.key(),
                boss.key(),
                boss.entity(),
                boss.dimension(),
                boss.cartographerLevel(),
                boss.structureTag(),
                boss,
                null
        );
    }

    public static CampaignTarget prerequisite(CampaignPrerequisite prerequisite) {
        return new CampaignTarget(
                "sub/" + prerequisite.key(),
                prerequisite.key(),
                prerequisite.entity(),
                prerequisite.dimension(),
                prerequisite.cartographerLevel(),
                prerequisite.structureTag(),
                null,
                prerequisite
        );
    }

    public static List<CampaignTarget> values() {
        return VALUES;
    }

    public static Optional<CampaignTarget> bySelectionKey(String key) {
        return VALUES.stream().filter(target -> target.selectionKey.equals(key)).findFirst();
    }

    public static CampaignTarget forBoss(CampaignBoss boss) {
        return VALUES.stream().filter(target -> target.mainBoss == boss).findFirst().orElseThrow();
    }

    public static CampaignTarget forPrerequisite(CampaignPrerequisite prerequisite) {
        return VALUES.stream().filter(target -> target.prerequisite == prerequisite).findFirst().orElseThrow();
    }

    private static List<CampaignTarget> createValues() {
        List<CampaignTarget> targets = new ArrayList<>();
        Arrays.stream(CampaignBoss.values()).map(CampaignTarget::main).forEach(targets::add);
        Arrays.stream(CampaignPrerequisite.values()).map(CampaignTarget::prerequisite).forEach(targets::add);
        return List.copyOf(targets);
    }
}
