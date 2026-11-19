package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.network.FamilyViewPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class FamilyClientState {
    private static int target = -1; private static long received; private static CompoundTag view = new CompoundTag();
    private FamilyClientState() { }
    public static void accept(FamilyViewPacket packet) { target = packet.entityId(); received = System.currentTimeMillis(); view = packet.view().copy(); if (packet.open()) Minecraft.getInstance().setScreen(new FamilyHomeScreen(Minecraft.getInstance().screen, view)); }
    public static boolean active() { return target >= 0 && System.currentTimeMillis() - received < 30_000L; }
    public static void open() { if (active()) com.siirio.jemsettlements.network.SettlementNetwork.requestFamilyHome(target); }
    public static void clear() { target = -1; received = 0; view = new CompoundTag(); }
}
