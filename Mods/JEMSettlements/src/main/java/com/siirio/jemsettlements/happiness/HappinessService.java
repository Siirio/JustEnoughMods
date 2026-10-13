package com.siirio.jemsettlements.happiness;

public final class HappinessService {
    private HappinessService() {
    }

    public static int calculate(int housing, int food, int safety, int jobs, int community, int taxPercent) {
        int penalty = switch (taxPercent) {
            case 0 -> 0;
            case 5 -> 3;
            case 10 -> 8;
            case 15 -> 16;
            default -> throw new IllegalArgumentException("Unsupported tax rate");
        };
        long weighted = Math.round(0.25D * clamp(housing) + 0.25D * clamp(food) + 0.20D * clamp(safety)
                + 0.15D * clamp(jobs) + 0.15D * clamp(community));
        return clamp((int) weighted - penalty);
    }

    public static double productionFactor(int happiness) {
        if (happiness >= 80) return 1.05D;
        if (happiness < 35) return 0.90D;
        return 1.0D;
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
