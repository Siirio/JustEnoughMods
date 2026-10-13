package com.siirio.jemeconomy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.network.WalletStatusPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemEconomy.MOD_ID, value = Dist.CLIENT)
public final class WalletClientState {
    private static WalletStatusPacket status;
    private static final KeyMapping OPEN = new KeyMapping("key.jem_economy.wallet", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.jem_economy");
    private WalletClientState() {}

    public static void accept(WalletStatusPacket packet) {
        status = packet;
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof WalletScreen screen) screen.accept(packet);
        else if (packet.open()) client.setScreen(new WalletScreen(packet));
    }

    @Mod.EventBusSubscriber(modid = JemEconomy.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OPEN.consumeClick()) if (status != null) Minecraft.getInstance().setScreen(new WalletScreen(status));
    }
}
