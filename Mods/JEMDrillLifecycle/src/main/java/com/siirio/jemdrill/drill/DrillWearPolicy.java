package com.siirio.jemdrill.drill;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class DrillWearPolicy {
    public static final String WEAR_KEY = "JemDrillWear";
    public static final String REPAIRS_KEY = "JemDrillRepairs";
    public static final int MAX_WEAR = 1000;
    public static final int DULL_WEAR = 500;
    public static final int MAX_REPAIRS = 3;
    public static final float DULL_SPEED_MULTIPLIER = 0.5F;
    private static final int NORMAL_WEAR = 1;
    private static final int OBSIDIAN_WEAR = 500;

    private DrillWearPolicy() {
    }

    public static int afterBreak(int wear, BlockState state) {
        return Math.min(MAX_WEAR, wear + (state.is(Blocks.OBSIDIAN) ? OBSIDIAN_WEAR : NORMAL_WEAR));
    }
}
