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
    private static final KeyMapping[] ARCHITECT_SLOTS = new KeyMapping[]{
            new KeyMapping("key.jem_professions.architect_slot_10", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.jem_professions"),
            new KeyMapping("key.jem_professions.architect_slot_11", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.jem_professions"),
            new KeyMapping("key.jem_professions.architect_slot_12", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.jem_professions"),
            new KeyMapping("key.jem_professions.architect_slot_13", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.jem_professions")
    };
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
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); for (KeyMapping key : ARCHITECT_SLOTS) event.register(key); }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OPEN.consumeClick()) if (status != null) Minecraft.getInstance().setScreen(new ProfessionsScreen(status));
        if (Minecraft.getInstance().screen == null) for (int i = 0; i < ARCHITECT_SLOTS.length; i++) {
            while (ARCHITECT_SLOTS[i].consumeClick()) com.siirio.jemprofessions.network.ProfessionsNetwork.selectArchitectSlot(9 + i);
        }
    }
}
