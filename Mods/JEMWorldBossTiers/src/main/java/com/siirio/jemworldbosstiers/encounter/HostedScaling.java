package com.siirio.jemworldbosstiers.encounter;

public record HostedScaling(double health, double damage) {
    public static HostedScaling calculate(int participants, boolean raid, double raidHealth, double raidDamage) {
        if (participants < 1) throw new IllegalArgumentException("A hosted encounter needs an active participant");
        double health = switch (participants) {
            case 1 -> 1.0;
            case 2 -> 1.45;
            case 3 -> 1.80;
            case 4 -> 2.10;
            default -> 2.10 + 0.20 * (participants - 4);
        };
        double damage = switch (participants) {
            case 1 -> 1.0;
            case 2 -> 1.10;
            case 3 -> 1.18;
            case 4 -> 1.25;
            default -> Math.min(1.45, 1.25 + 0.05 * (participants - 4));
        };
        return new HostedScaling(health * (raid ? raidHealth : 1), damage * (raid ? raidDamage : 1));
    }
}
