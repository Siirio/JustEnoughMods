package com.siirio.jemprofessions.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemprofessions.JemProfessions;
import com.siirio.jemprofessions.network.ProfessionStatusPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemProfessions.MOD_ID, value = Dist.CLIENT)
public final class ProfessionClientState {
    private static ProfessionStatusPacket status;
    private static final KeyMapping OPEN = new KeyMapping("key.jem_professions.open", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.jem_professions");
    private ProfessionClientState() {}

    public static void accept(ProfessionStatusPacket packet) {
        status = packet;
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.open()) {
            if (minecraft.screen instanceof ProfessionTreeScreen tree) minecraft.setScreen(new ProfessionTreeScreen(packet, tree.profession()));
            else minecraft.setScreen(new ProfessionsScreen(packet));
        }
    }

    @Mod.EventBusSubscriber(modid = JemProfessions.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OPEN.consumeClick()) if (status != null) Minecraft.getInstance().setScreen(new ProfessionsScreen(status));
    }
}
