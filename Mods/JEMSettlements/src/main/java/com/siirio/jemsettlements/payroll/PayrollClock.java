package com.siirio.jemsettlements.payroll;

/** Monotonic game time, independent of /time, daylight, chunks and dimensions. */
public final class PayrollClock {
    public static final long WEEK = 7L * 24_000L;
    private PayrollClock() { }
    public static long firstDue(long enlisted) { return Math.addExact(Math.max(0L, enlisted), WEEK); }
    public static long nextDue(long previous) { return Math.addExact(previous, WEEK); }
    public static boolean due(long next, long now) { return next > 0L && now >= next; }
}
