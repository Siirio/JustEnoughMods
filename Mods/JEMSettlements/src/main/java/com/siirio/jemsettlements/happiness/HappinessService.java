package com.siirio.jemsettlements.happiness;

/** One shared 90-point needs budget, with a separate ten-point personal activity. */
public final class HappinessService {
    private HappinessService() { }
    public static int taxPenalty(int percent) {
        return switch (percent) { case 0 -> 0; case 5 -> 3; case 10 -> 8; case 15 -> 16;
            default -> throw new IllegalArgumentException("Unsupported tax rate"); };
    }
    public static double core(double housing, double food, double safety, double jobs, double community, double comfort) {
        return .20 * bound(housing) + .20 * bound(food) + .10 * bound(safety)
                + .10 * bound(jobs) + .15 * bound(community) + .15 * bound(comfort);
    }
    public static int calculate(int housing, int food, int safety, int jobs, int community, int taxPercent) {
        return calculate(housing, food, safety, jobs, community, 0, taxPercent);
    }
    public static int calculate(int housing, int food, int safety, int jobs, int community, int comfort, int taxPercent) {
        return (int)Math.round(bound(core(housing, food, safety, jobs, community, comfort) - taxPenalty(taxPercent)));
    }
    /** Retained for native recruit adapters; ordinary residents use the shared core directly. */
    public static int residentMood(int personal, int environmentBonus, int socialCap) { return (int)bound(personal); }
    public static int socialCap(int social) { return 100; }
    public static int environmentBonus(int comfort, int animals) { return 0; }
    public static double productionFactor(int happiness) { return happiness >= 80 ? 1.05 : happiness < 35 ? .90 : 1; }
    private static double bound(double value) { return Math.max(0, Math.min(100, value)); }
}
