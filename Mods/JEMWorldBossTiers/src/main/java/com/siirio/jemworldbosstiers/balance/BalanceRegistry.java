package com.siirio.jemworldbosstiers.balance;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.siirio.jemworldbosstiers.progression.ProgressionRules;

public final class BalanceRegistry {
    private static volatile Map<ResourceLocation, BossProfile> bossesByEntity = Map.of();
    private static volatile Map<ResourceLocation, BossProfile> bossesByKey = Map.of();
    private static volatile Map<ResourceLocation, BossProfile> bossesByVictoryAdvancement = Map.of();
    private static volatile Map<ResourceLocation, RewardProfile> rewardsByItem = Map.of();
    private static volatile List<Integer> tierThresholds = List.of(0);

    private BalanceRegistry() {
    }

    public static Optional<BossProfile> boss(ResourceLocation entityId) {
        return Optional.ofNullable(bossesByEntity.get(entityId));
    }

    public static Optional<BossProfile> bossByVictoryAdvancement(ResourceLocation advancementId) {
        return Optional.ofNullable(bossesByVictoryAdvancement.get(advancementId));
    }

    public static Collection<ResourceLocation> victoryAdvancementIds() {
        return bossesByVictoryAdvancement.keySet();
    }

    public static Optional<BossProfile> bossByKey(ResourceLocation key) {
        return Optional.ofNullable(bossesByKey.get(key));
    }

    public static Set<ResourceLocation> activeQualifyingKeys() {
        return bossesByKey.values().stream().filter(BossProfile::countsTowardWorldTier).map(BossProfile::key).collect(Collectors.toUnmodifiableSet());
    }

    public static List<Integer> tierThresholds() {
        return tierThresholds;
    }

    public static Optional<RewardProfile> reward(ResourceLocation itemId) {
        return Optional.ofNullable(rewardsByItem.get(itemId));
    }

    public static Collection<RewardProfile> rewards() {
        return rewardsByItem.values();
    }

    public static int worldTierForDefeats(int uniqueDefeats) {
        return ProgressionRules.tierForCount(uniqueDefeats, tierThresholds);
    }

    public static int worldTierForDefeats(Set<ResourceLocation> historicalDefeats) {
        return ProgressionRules.tier(historicalDefeats, activeQualifyingKeys(), tierThresholds);
    }

    public static void replaceBosses(Collection<BossProfile> profiles, List<Integer> thresholds) {
        Map<ResourceLocation, BossProfile> indexed = new HashMap<>();
        Map<ResourceLocation, BossProfile> keyed = new HashMap<>();
        Map<ResourceLocation, BossProfile> indexedAdvancements = new HashMap<>();
        for (BossProfile profile : profiles) {
            if (keyed.put(profile.key(), profile) != null) {
                throw new IllegalArgumentException("Duplicate boss key: " + profile.key());
            }
            for (ResourceLocation entityId : profile.entityIds()) {
                BossProfile previous = indexed.put(entityId, profile);
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate boss entity mapping: " + entityId);
                }
            }
            for (ResourceLocation advancementId : profile.victoryAdvancementIds()) {
                BossProfile previous = indexedAdvancements.put(advancementId, profile);
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate boss victory advancement mapping: " + advancementId);
                }
            }
        }
        validateThresholds(thresholds);
        bossesByEntity = Map.copyOf(indexed);
        bossesByKey = Map.copyOf(keyed);
        bossesByVictoryAdvancement = Map.copyOf(indexedAdvancements);
        tierThresholds = List.copyOf(thresholds);
    }

    public static void replaceRewards(Collection<RewardProfile> profiles) {
        Map<ResourceLocation, RewardProfile> indexed = new HashMap<>();
        for (RewardProfile profile : profiles) {
            RewardProfile previous = indexed.put(profile.itemId(), profile);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate reward item mapping: " + profile.itemId());
            }
        }
        rewardsByItem = Map.copyOf(indexed);
    }

    private static void validateThresholds(List<Integer> thresholds) {
        if (thresholds.isEmpty() || thresholds.get(0) != 0) {
            throw new IllegalArgumentException("The first boss tier threshold must be zero");
        }
        for (int index = 1; index < thresholds.size(); index++) {
            if (thresholds.get(index) <= thresholds.get(index - 1)) {
                throw new IllegalArgumentException("Boss tier thresholds must be strictly increasing");
            }
        }
    }
}
