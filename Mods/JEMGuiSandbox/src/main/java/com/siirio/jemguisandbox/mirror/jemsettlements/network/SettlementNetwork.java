package com.siirio.jemguisandbox.mirror.jemsettlements.network;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
public final class SettlementNetwork {
    public static boolean supported() { return true; }
    public static void action(String action, UUID town, CompoundTag payload, UUID request) {
        com.siirio.jemguisandbox.Demo.settlementAction(action,town,payload);
    }
}
