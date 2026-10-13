package com.siirio.jemprofessions.progress;

public final class ProfessionFormula {
    public static final int MAX_LEVEL = 50;

    private ProfessionFormula() {
    }

    public static int stepCost(int level) {
        int bounded = Math.max(1, Math.min(MAX_LEVEL, level));
        return (int) Math.round(100.0D * Math.pow(1.0D + 0.08D * bounded, 1.4D));
    }

    public static int levelFor(long totalXp) {
        long remaining = Math.max(0L, totalXp);
        int level = 1;
        while (level < MAX_LEVEL) {
            int cost = stepCost(level);
            if (remaining < cost) break;
            remaining -= cost;
            level++;
        }
        return level;
    }
}
