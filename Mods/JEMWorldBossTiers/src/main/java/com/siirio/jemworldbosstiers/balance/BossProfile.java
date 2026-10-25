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
        boolean hostedEncounter,
        List<ResourceLocation> allyTags,
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
        allyTags = List.copyOf(allyTags);
        victoryAdvancementIds = List.copyOf(victoryAdvancementIds);
        attributes = Map.copyOf(attributes);
    }

    /** Progression enrollment alone never gives a mob exclusive arena ownership. */
    public boolean ownsExclusiveArena() {
        return hostedEncounter || !"none".equals(arenaStrategy)
                || com.siirio.jemworldbosstiers.revival.RespawnOwnership.usesJemScheduler(revivalStrategy);
    }
}
