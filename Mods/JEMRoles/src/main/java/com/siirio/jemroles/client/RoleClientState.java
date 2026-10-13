package com.siirio.jemroles.client;

import com.siirio.jemroles.JemRoles;
import com.siirio.jemroles.network.RaceStatusPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemRoles.MOD_ID, value = Dist.CLIENT)
public final class RoleClientState {
    private static RaceStatusPacket status;
    private static final KeyMapping OPEN = new KeyMapping("key.jem_roles.open", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.jem_roles");

    private RoleClientState() {}

    public static void accept(RaceStatusPacket packet) {
        status = packet;
        if (packet.open()) Minecraft.getInstance().setScreen(new RaceSelectionScreen(packet));
    }

    @Mod.EventBusSubscriber(modid = JemRoles.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OPEN.consumeClick()) if (status != null) Minecraft.getInstance().setScreen(new RaceSelectionScreen(status));
    }
}
