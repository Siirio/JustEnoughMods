package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
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
        }).then(Commands.literal("terrain")
                .executes(context -> reportTerrainStatus())
                .then(Commands.literal("status").executes(context -> reportTerrainStatus()))
                .then(Commands.literal("on").executes(context -> {
                    TerrainLighting.setEnabled(true);
                    return reportTerrainStatus();
                }))
                .then(Commands.literal("off").executes(context -> {
                    TerrainLighting.setEnabled(false);
                    return reportTerrainStatus();
                }))));
    }

    private static int reportTerrainStatus() {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("JEM terrain lighting: " + TerrainLighting.status()));
        return 1;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TerrainLighting.tick(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        updateChunk(event);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        updateChunk(event);
    }

    private static void updateChunk(ChunkEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (event.getLevel() == client.level) {
            var position = event.getChunk().getPos();
            client.execute(() -> {
                if (event.getLevel() == client.level) {
                    TerrainLighting.invalidateChunk(position);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        Minecraft client = Minecraft.getInstance();
        if (event.getLevel() == client.level) {
            client.execute(TerrainLighting::clearWorld);
        }
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
