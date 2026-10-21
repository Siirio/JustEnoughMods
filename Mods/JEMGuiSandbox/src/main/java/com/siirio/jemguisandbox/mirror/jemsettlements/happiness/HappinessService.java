package com.siirio.jemguisandbox.mirror.jemsettlements.happiness;
/** Presentation sample only; never an installed settlement policy. */
public final class HappinessService {
    public static int taxPenalty(int tax) { return -Math.max(0,tax); }
}
