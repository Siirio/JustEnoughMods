package com.siirio.jemsettlements.happiness;

public final class HappinessService {
    private HappinessService() {
    }

    public static int calculate(int housing, int food, int safety, int jobs, int community, int taxPercent) {
        return calculate(housing, food, safety, jobs, community, 0, taxPercent);
    }

    public static int calculate(int housing, int food, int safety, int jobs, int community, int decoration, int taxPercent) {
        int penalty = switch (taxPercent) {
            case 0 -> 0;
            case 5 -> 3;
            case 10 -> 8;
            case 15 -> 16;
            default -> throw new IllegalArgumentException("Unsupported tax rate");
        };
        long weighted = Math.round(0.20D * clamp(housing) + 0.20D * clamp(food) + 0.20D * clamp(safety)
                + 0.15D * clamp(jobs) + 0.15D * clamp(community) + 0.10D * clamp(decoration));
        return clamp((int) weighted - penalty);
    }

    /** Personal stimuli and town environment have separate owners; no feedback from aggregate happiness. */
    public static int residentMood(int personal, int environmentBonus, int socialCap) {
        return Math.min(clamp(socialCap), clamp(personal + Math.max(0, environmentBonus)));
    }
    public static int socialCap(int social) {
        int floor = SettlementMetricsConfig.SOCIAL_BASE_CAP.get();
        return floor + clamp(social) * (100 - floor) / 100;
    }
    public static int environmentBonus(int comfort, int animals) {
        return clamp(comfort) * SettlementMetricsConfig.COMFORT_BONUS.get() / 100
                + clamp(animals) * SettlementMetricsConfig.ANIMAL_BONUS.get() / 100;
    }
    public static double productionFactor(int happiness) {
        if (happiness >= 80) return 1.05D;
        if (happiness < 35) return 0.90D;
        return 1.0D;
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
