package com.siirio.jemsettlements.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.simple.SimpleChannel;

@OnlyIn(Dist.CLIENT)
final class ClientConnection {
    private static java.util.UUID session;
    private ClientConnection() { }
    static boolean remote(SimpleChannel channel) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return connection != null && channel.isRemotePresent(connection.getConnection());
    }
    static void bind(net.minecraft.nbt.CompoundTag payload) { if (session != null) payload.putUUID("Session", session); }
    static void accept(net.minecraft.nbt.CompoundTag view) { if (view.hasUUID("Session")) session = view.getUUID("Session"); }
    static void clear() { session = null; }
    static java.util.UUID request() { return session == null ? java.util.UUID.randomUUID() : new java.util.UUID(session.getMostSignificantBits(), java.util.UUID.randomUUID().getLeastSignificantBits()); }
}
