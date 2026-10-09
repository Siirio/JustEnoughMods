package com.siirio.jemworldbosstiers.progression;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ProgressionRules {
    private ProgressionRules() {
    }

    public static int tier(Set<ResourceLocation> historicalDefeats, Set<ResourceLocation> activeQualifyingKeys, List<Integer> thresholds) {
        long activeDefeats = historicalDefeats.stream().filter(activeQualifyingKeys::contains).count();
        return tierForCount(Math.toIntExact(activeDefeats), thresholds);
    }

    public static int tierForCount(int uniqueDefeats, List<Integer> thresholds) {
        validateThresholds(thresholds);
        int tier = 1;
        for (int index = 1; index < thresholds.size(); index++) {
            if (uniqueDefeats < thresholds.get(index)) {
                break;
            }
            tier = index + 1;
        }
        return tier;
    }

    public static int activeDefeatCount(Collection<ResourceLocation> historicalDefeats, Set<ResourceLocation> activeQualifyingKeys) {
        return Math.toIntExact(historicalDefeats.stream().filter(activeQualifyingKeys::contains).distinct().count());
    }

    private static void validateThresholds(List<Integer> thresholds) {
        if (thresholds.isEmpty() || thresholds.size() > 5 || thresholds.get(0) != 0) {
            throw new IllegalArgumentException("Thresholds must define one to five tiers beginning at zero");
        }
        for (int index = 1; index < thresholds.size(); index++) {
            if (thresholds.get(index) <= thresholds.get(index - 1)) {
                throw new IllegalArgumentException("Tier thresholds must be strictly increasing");
            }
        }
    }
}
