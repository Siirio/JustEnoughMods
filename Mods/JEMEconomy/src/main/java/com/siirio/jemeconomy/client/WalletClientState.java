package com.siirio.jemeconomy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.network.WalletStatusPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
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

    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var policy = com.siirio.jemeconomy.policy.ItemPolicyRegistry.policy(event.getItemStack());
        if (policy.isPresent() && policy.get().sellable()) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("jem.economy.tooltip.shipping_price",
                    com.siirio.jemeconomy.physical.CoinFormatter.format(policy.get().sellCopper()),
                    com.siirio.jemeconomy.physical.CoinFormatter.format(policy.get().buyCopper())));
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("jem.economy.tooltip.shipping_quota",
                    policy.get().dailyPlayerUnits(), policy.get().dailyGlobalUnits()));
            if (status != null) event.getToolTip().add(net.minecraft.network.chat.Component.translatable("jem.economy.tooltip.shipping_next",
                    status.accountRemaining(), status.ticksUntilUpdate()));
        } else if (net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()) != null) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("jem.economy.tooltip.shipping_blocked"));
        }
    }
}
