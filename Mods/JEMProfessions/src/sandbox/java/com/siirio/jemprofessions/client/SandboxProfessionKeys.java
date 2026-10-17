package com.siirio.jemprofessions.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.siirio.jemprofessions.JemProfessions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = JemProfessions.MOD_ID, value = Dist.CLIENT)
public final class SandboxProfessionKeys {
    private static final KeyMapping OPEN = new KeyMapping("key.jem_professions.open_sandbox", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7, "key.categories.jem_professions");
    private SandboxProfessionKeys() { }
    @Mod.EventBusSubscriber(modid = JemProfessions.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        private Keys() { }
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN.consumeClick()) if (minecraft.screen == null && minecraft.player != null && minecraft.hasSingleplayerServer())
            minecraft.player.connection.sendCommand("professions");
    }
}
