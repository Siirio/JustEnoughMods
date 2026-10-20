package com.siirio.jemworldbosstiers.compat.irons;

public final class SiphonHealingTest {
    private static void check(String scenario, float expected, float actual) {
        if (Math.abs(expected - actual) > 0.0001F) throw new AssertionError(scenario + ": " + actual);
    }

    public static void main(String[] args) {
        check("armor-mitigated hit heals final loss", 2, SiphonHealing.acceptedHealing(true, 20, 18, 1));
        check("canceled hit never heals", 0, SiphonHealing.acceptedHealing(false, 20, 18, 1));
        check("invulnerable hit never heals", 0, SiphonHealing.acceptedHealing(false, 20, 20, 1));
        check("zero effective loss never heals", 0, SiphonHealing.acceptedHealing(true, 20, 20, 1));
        check("absorbed damage counts resource loss", 3, SiphonHealing.acceptedHealing(true, 24, 21, 1));
        check("overkill uses remaining target health", 1, SiphonHealing.acceptedHealing(true, 1, 0, 1));
        check("native share remains authoritative", 1, SiphonHealing.acceptedHealing(true, 20, 16, 0.25F));
        check("healed target grants no negative lifesteal", 0, SiphonHealing.acceptedHealing(true, 20, 24, 1));
        System.out.println("SiphonHealingTest: 8 contracts passed");
    }
}
