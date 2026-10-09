package com.siirio.jemserver.smp.events;

import java.util.*;

public final class BloodMoonWaves {
    public enum Category { HORDE, ARACHNID, RANGED, HUNTER, FLYING, TELEPORTER, CREEPER, SUPPORT }
    public static final int WAVES = 5;
    private static final double EARLY_WAVE_DAMAGE_MULTIPLIER = 1.15;
    private static final double WAVE_TWO_DAMAGE_MULTIPLIER = 1.30;
    private static final double WAVE_THREE_STRENGTH_MULTIPLIER = 1.15;
    private static final int MOBS_PER_BATCH = 20;
    private static final int[] BATCHES = {2,2,0,3,0};
    private static final double[] REWARD_FACTORS = {.5,.65,.85,1,1};
    private static final double[] WAVE_PRESSURE = {1,1.15,1.30,1.45,1.60};
    private static final double[] ATTACK_PRESSURE = {1,1.40,1.50,2.20,2.15};
    private static final double[] MOVEMENT_SPEED = {1.06,1.12,1,1.15,1};
    private static final double[] ARCHETYPE_CHANCE = {.15,.25,.35,.45,.50};
    private static final double[] ELITE_CHANCE = {.06,.09,.12,.16,.20};
    private static final int[][] SHARES = {
            {45,25,15,5,0,0,10,0},
            {30,20,20,15,5,0,10,0},
            {0,0,0,0,0,0,0,0},
            {20,10,20,25,5,5,10,5},
            {0,0,0,0,0,0,0,0}
    };
    public static int total(int tier, int wave, int players) {
        return batchSize(players)*batchCount(wave);
    }
    public static int batchSize(int players) { return MOBS_PER_BATCH*Math.max(1,Math.min(5,players)); }
    public static int batchCount(int wave) { return BATCHES[Math.max(1,Math.min(WAVES,wave))-1]; }
    public static int ordinaryTotal(int tier,int wave,int players,int setPieces) {
        return total(tier,wave,players);
    }
    public static int batchTarget(int wave,int players,int ordinaryTotal) {
        return Math.min(batchSize(players),ordinaryTotal);
    }
    public static double factor(int wave) { return REWARD_FACTORS[Math.max(1,Math.min(WAVES,wave))-1]; }
    public static double pressure(int wave) { return WAVE_PRESSURE[Math.max(1,Math.min(WAVES,wave))-1]; }
    public static double healthStrength(int wave) { return wave==3?WAVE_THREE_STRENGTH_MULTIPLIER:1; }
    public static double damagePressure(int wave) {
        return switch(Math.max(1,Math.min(WAVES,wave))) {
            case 1 -> EARLY_WAVE_DAMAGE_MULTIPLIER;
            case 2 -> WAVE_TWO_DAMAGE_MULTIPLIER;
            case 3 -> pressure(wave)*WAVE_THREE_STRENGTH_MULTIPLIER;
            default -> pressure(wave);
        };
    }
    public static double attackPressure(int tier) { return ATTACK_PRESSURE[Math.max(1,Math.min(ATTACK_PRESSURE.length,tier))-1]; }
    public static double movementSpeed(int wave) { return MOVEMENT_SPEED[Math.max(1,Math.min(WAVES,wave))-1]; }
    public static double archetypeChance(int wave) { return ARCHETYPE_CHANCE[Math.max(1,Math.min(WAVES,wave))-1]; }
    public static double eliteChance(int tier,int wave) {
        return Math.min(1,ELITE_CHANCE[Math.max(1,Math.min(5,tier))-1]+(wave==WAVES?.05:0));
    }
    public static Map<Category,Integer> composition(int wave,int total) {
        var counts = new EnumMap<Category,Integer>(Category.class);
        int remaining = Math.max(0,total);
        var shares = SHARES[wave-1];
        int assigned = 0;
        var indices = new ArrayList<Integer>();
        for (int i=0;i<shares.length;i++) {
            int count=remaining*shares[i]/100;
            counts.put(Category.values()[i], count);
            assigned+=count;
            indices.add(i);
        }
        indices.sort(Comparator.<Integer>comparingInt(i -> remaining*shares[i]%100).reversed().thenComparingInt(i -> i));
        for(int i=0;i<remaining-assigned;i++) counts.merge(Category.values()[indices.get(i)],1,Integer::sum);
        return counts;
    }
    private BloodMoonWaves() {}
}
