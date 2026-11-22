package com.siirio.jemsettlements.payroll;

import net.minecraft.server.MinecraftServer;

public final class PayrollService {
    public static final long PERIOD_TICKS = PayrollClock.WEEK;
    private PayrollService() { }
    public static void tick(MinecraftServer server) { }
}
