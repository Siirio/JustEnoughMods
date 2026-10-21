package com.siirio.jemguisandbox.mirror.jemserver.smp;
import com.siirio.jemguisandbox.Demo;
/** Local presentation seam. This class deliberately has no network channel. */
public final class SmpNetwork {
    public static void query(SmpQuery query) { Demo.query(query); }
    public static void action(SmpAction action) { Demo.action(action); }
    public static void settlement(SettlementUiAction action) { Demo.notice("Demo: " + action.action()); }
}
