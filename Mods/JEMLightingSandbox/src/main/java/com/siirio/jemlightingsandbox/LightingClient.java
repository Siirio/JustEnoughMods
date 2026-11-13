package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = JemLightingSandbox.ID, value = Dist.CLIENT)
public final class LightingClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static ShaderInstance sceneShader;

    private LightingClient() {
    }

    static ShaderInstance sceneShader() {
        return sceneShader;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jemlighting").executes(context -> {
            Minecraft client = Minecraft.getInstance();
            client.tell(() -> client.setScreen(new LightingScreen()));
            return 1;
        }));
    }

    @Mod.EventBusSubscriber(modid = JemLightingSandbox.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ShaderEvents {
        private ShaderEvents() {
        }

        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) {
            sceneShader = null;
            try {
                var shader = new ShaderInstance(event.getResourceProvider(),
                        new ResourceLocation(JemLightingSandbox.ID, "shadow_preview"), DefaultVertexFormat.POSITION_TEX);
                event.registerShader(shader, loaded -> sceneShader = loaded);
            } catch (IOException exception) {
                LOGGER.error("Could not load the JEM lighting preview shader", exception);
            }
        }
    }
}
