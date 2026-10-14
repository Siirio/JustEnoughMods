package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.merchant.NativeTradeQuote;
import com.siirio.jemeconomy.network.NativeTradePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemEconomy.MOD_ID, value = Dist.CLIENT)
public final class NativeTradeClient {
    private static NativeTradePacket snapshot;
    private NativeTradeClient() {}
    public static void accept(NativeTradePacket packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof MerchantMenu && player.containerMenu.containerId == packet.containerId()) snapshot = packet;
    }
    public static NativeTradePacket snapshot(MerchantMenu menu) {
        return snapshot != null && snapshot.containerId() == menu.containerId ? snapshot : null;
    }
    public static NativeTradeQuote quote(MerchantMenu menu, int index) {
        var current = snapshot(menu);
        return current == null || index < 0 || index >= current.quotes().size() ? null : current.quotes().get(index);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { snapshot = null; }
    @SubscribeEvent public static void closed(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        var player = Minecraft.getInstance().player;
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && (player == null || !(player.containerMenu instanceof MerchantMenu))) snapshot = null;
    }
}
