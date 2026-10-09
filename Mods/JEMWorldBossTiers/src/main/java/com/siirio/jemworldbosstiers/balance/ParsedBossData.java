package com.siirio.jemworldbosstiers.balance;

import java.util.List;

public record ParsedBossData(List<BossProfile> profiles, List<Integer> thresholds) {
    public ParsedBossData {
        profiles = List.copyOf(profiles);
        thresholds = List.copyOf(thresholds);
    }
}
