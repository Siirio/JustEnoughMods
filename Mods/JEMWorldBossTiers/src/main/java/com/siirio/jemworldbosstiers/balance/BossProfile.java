package com.siirio.jemworldbosstiers.balance;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record BossProfile(
        ResourceLocation key,
        String displayName,
        List<ResourceLocation> entityIds,
        List<ResourceLocation> victoryAdvancementIds,
        boolean scalesWithWorldTier,
        boolean countsTowardWorldTier,
        double nativeThreat,
        String hpScalingSafety,
        Map<ResourceLocation, TierValues> attributes,
        TierValues specialDamageMultiplier,
        String damageAdapter,
        String healingAdapter,
        String staggerAdapter,
        String arenaStrategy,
        String revivalStrategy,
        String antiCheeseProfile,
        String destructionRules,
        int arenaRadius,
        ResourceLocation revivalOffering,
        int revivalXpLevels
) {
    public BossProfile {
        entityIds = List.copyOf(entityIds);
        victoryAdvancementIds = List.copyOf(victoryAdvancementIds);
        attributes = Map.copyOf(attributes);
    }
}
