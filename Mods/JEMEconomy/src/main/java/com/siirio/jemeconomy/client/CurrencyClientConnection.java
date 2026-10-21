package com.siirio.jemeconomy.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.simple.SimpleChannel;

@OnlyIn(Dist.CLIENT)
public final class CurrencyClientConnection {
    private static boolean enabled;
    private CurrencyClientConnection() {}
    public static void enabled(boolean value) {
        enabled = value;
        if (!value) {
            var client = Minecraft.getInstance();
            MarketClientState.clear();
            if (client.screen instanceof MarketScreen || client.screen instanceof CurrencyContainerScreen)
                client.setScreen(null);
        }
    }

    public static boolean remote(SimpleChannel channel) {
        var connection = Minecraft.getInstance().getConnection();
        return enabled && connection != null && channel.isRemotePresent(connection.getConnection());
    }
}
