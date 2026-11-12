package dev.taste.engine.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taste.engine.TasteMod;
import dev.taste.engine.effects.PhotoCapture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PhotoModeEvents {
    private static final KeyMapping MODE = new KeyMapping("key.taste.photoMode", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, "key.categories.taste");
    private static final KeyMapping ACTION = new KeyMapping("key.taste.photoAction", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9, "key.categories.taste");

    private PhotoModeEvents() { }

    @Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }

        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
            event.register(MODE);
            event.register(ACTION);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        var config = ClientState.store().current();
        PhotoCapture capture = PhotoCapture.instance();
        capture.tick(minecraft, config);
        while (MODE.consumeClick()) {
            if (minecraft.screen == null) capture.toggle(minecraft, config);
        }
        if (ACTION.consumeClick()) {
            // Collapse a burst of repeats into one explicit action.
            while (ACTION.consumeClick()) { }
            if (minecraft.screen == null) capture.action(minecraft);
        }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        PhotoCapture.instance().abort();
    }
}
