package com.siirio.jemvisualsandbox.workbench;

/** Shared live/preview contract for Born in Bloom's main power. */
public final class BloomMechanics {
    public static final int GRIP_TICKS=60;
    public static final int BOSS_GRIP_TICKS=GRIP_TICKS*2/5;
    public static final int POISON_TICKS=90;
    public static final float INITIAL_DAMAGE=5.4F;
    public static final float SQUEEZE_DAMAGE=2.7F;
    public static final float POISON_DAMAGE=1F;
    private BloomMechanics() {}
    /** Existing authored grow/hold/release poses, with release at grip expiry. */
    public static double rootPoseTick(double age) {
        return rootPoseTick(age,GRIP_TICKS);
    }
    public static double rootPoseTick(double age,int gripTicks) {
        return age<16?age:age<gripTicks?16:16+(age-gripTicks)*8/12;
    }
}
