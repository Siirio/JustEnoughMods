package com.siirio.jemguisandbox.mirror.jemsettlements.client;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
public final class SettlementClientState {
    public static CompoundTag view() { return com.siirio.jemguisandbox.Demo.settlementView(); }
    public static boolean pending() { return false; }
    public static boolean outcomeUnknown() { return false; }
    public static void open() { com.siirio.jemguisandbox.Demo.open("settlements"); }
    public static void openGuide() { open(); if(net.minecraft.client.Minecraft.getInstance().screen instanceof SettlementScreen s)s.guide(); }
    public static boolean action(String action, UUID town, CompoundTag payload) {
        com.siirio.jemguisandbox.Demo.settlementAction(action,town,payload); return true;
    }
    public static void reconcileNow() { }
    public static void reconcile() { }
    public static void closed() { }
}
