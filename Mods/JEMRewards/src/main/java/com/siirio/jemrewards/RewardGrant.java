package com.siirio.jemrewards;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** One recipient's already-rolled groups inside a single source-owned batch. */
public record RewardGrant(UUID recipient, String titleKey, List<RewardGroup> groups) {
    public RewardGrant {
        Objects.requireNonNull(recipient);
        Objects.requireNonNull(titleKey);
        groups = List.copyOf(groups);
    }
}
