package com.siirio.jemeconomy.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.simple.SimpleChannel;

@OnlyIn(Dist.CLIENT)
public final class CurrencyClientConnection {
    private CurrencyClientConnection() {}
    public static boolean remote(SimpleChannel channel) {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && channel.isRemotePresent(connection.getConnection());
    }
}
