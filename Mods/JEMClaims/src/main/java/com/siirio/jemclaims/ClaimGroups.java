package com.siirio.jemclaims;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ClaimGroups {
    private ClaimGroups() {}

    public static List<List<FlanBridge.Territory>> connected(List<FlanBridge.Territory> territories) {
        List<FlanBridge.Territory> remaining = new ArrayList<>(territories.stream()
                .sorted(Comparator.comparing(FlanBridge.Territory::name).thenComparing(FlanBridge.Territory::id)).toList());
        List<List<FlanBridge.Territory>> groups = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<FlanBridge.Territory> group = new ArrayList<>();
            group.add(remaining.remove(0));
            for (int index = 0; index < group.size(); index++) {
                FlanBridge.Territory current = group.get(index);
                remaining.removeIf(candidate -> {
                    if (!adjacent(current, candidate)) return false;
                    group.add(candidate);
                    return true;
                });
            }
            groups.add(List.copyOf(group));
        }
        return List.copyOf(groups);
    }

    public static boolean adjacent(FlanBridge.Territory a, FlanBridge.Territory b) {
        if (!Objects.equals(a.owner(), b.owner()) || !a.dimension().equals(b.dimension())) return false;
        boolean overlapX = a.minX() <= b.maxX() && b.minX() <= a.maxX();
        boolean overlapZ = a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ();
        return overlapX && ((long) a.maxZ() + 1 == b.minZ() || (long) b.maxZ() + 1 == a.minZ())
                || overlapZ && ((long) a.maxX() + 1 == b.minX() || (long) b.maxX() + 1 == a.minX())
                || overlapX && overlapZ;
    }
}
