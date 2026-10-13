package com.siirio.jemsettlements.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.network.TownViewPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT)
public final class SettlementClientState {
    private static TownViewPacket status;
    private static final KeyMapping OPEN = new KeyMapping("key.jem_settlements.open", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.jem_settlements");
    private SettlementClientState() {}
    public static void accept(TownViewPacket packet) {
        status = packet; Minecraft client = Minecraft.getInstance();
        if (packet.open()) client.setScreen(packet.view().getBoolean("Founding") ? new TownFoundingScreen(packet.view()) : new TownDashboardScreen(packet.view()));
    }
    @Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OPEN.consumeClick()) if (status != null && !status.view().getBoolean("NoTown"))
            Minecraft.getInstance().setScreen(new TownDashboardScreen(status.view()));
    }
}
