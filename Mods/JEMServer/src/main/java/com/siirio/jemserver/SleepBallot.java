package com.siirio.jemserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SleepBallot {
    private final Map<UUID, Boolean> choices = new HashMap<>();

    public boolean cast(UUID player, boolean agree, Set<UUID> eligible) {
        return eligible.contains(player) && choices.putIfAbsent(player, agree) == null;
    }

    public Boolean choice(UUID player) { return choices.get(player); }

    public long agreements(Set<UUID> eligible) {
        return eligible.stream().filter(id -> Boolean.TRUE.equals(choices.get(id))).count();
    }

    public long disagreements(Set<UUID> eligible) {
        return eligible.stream().filter(id -> Boolean.FALSE.equals(choices.get(id))).count();
    }

    public boolean approved(Set<UUID> eligible) { return agreements(eligible) > disagreements(eligible); }
}
