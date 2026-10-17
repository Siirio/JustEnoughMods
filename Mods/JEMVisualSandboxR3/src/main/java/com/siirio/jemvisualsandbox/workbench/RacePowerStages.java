package com.siirio.jemvisualsandbox.workbench;

/** Standalone integrated-owner ability stages: Base, I, II and III. */
public final class RacePowerStages {
    private RacePowerStages() {}
    public static int clamp(int stage) { return Math.max(0,Math.min(3,stage)); }
    public static int chorusRadius(int stage) { return clamp(stage)==0?12:16; }
    public static int riftRange(int stage) { return clamp(stage)==0?24:32; }
    public static int riftSlowTicks(int stage) { return clamp(stage)<2?0:clamp(stage)==2?40:60; }
    public static int riftSlowRadius(int stage) { return clamp(stage)<2?0:clamp(stage)==2?4:8; }
    public static float eruptionDamage(int stage) { return (4+2*clamp(stage))*.85F; }
    public static float slashDamage(int stage) { return eruptionDamage(stage); }
    public static int fireSeconds(int stage) { return clamp(stage)==0?1:2*clamp(stage); }
    public static int guardianTicks(int stage) { return 600+200*clamp(stage); }
    public static int gravityTicks(int stage) { return 100+20*clamp(stage); }
    public static int bridgeTicks(int stage) { return 800+200*clamp(stage); }
    public static int bridgeForward(int stage) { return 2+clamp(stage); }
    public static int echoRadius(int stage) { return 32+8*clamp(stage); }
    public static int recoveryHeal(int stage) { return 6+clamp(stage); }
    public static int absorptionTicks(int stage) { return 400+100*clamp(stage); }
}
