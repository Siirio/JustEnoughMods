package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import com.siirio.jemlightingsandbox.terrain.WaterSurface;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
                })))
                .then(Commands.literal("settings").executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.tell(() -> client.setScreen(new LightingSettingsScreen(client.screen)));
                    return 1;
                })));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderAtmosphere(RenderLevelStageEvent event) {
        AtmosphereRenderer.render(event);
    }

    private static int reportTerrainStatus() {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("JEM terrain lighting: " + TerrainLighting.status()));
        return 1;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft client = Minecraft.getInstance();
            TerrainLighting.tick(client);
            SandboxWeather.tick(client);
            PrecipitationRenderer.tick(client);
            WindGusts.tick(client);
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
            client.execute(() -> {
                TerrainLighting.clearWorld();
                PrecipitationRenderer.clear();
                WindGusts.clear();
                AtmosphereRenderer.releaseTargets();
                WaterSurface.release();
            });
        }
    }

    @Mod.EventBusSubscriber(modid = JemLightingSandbox.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ShaderEvents {
        private static ShaderInstance atmosphere;
        private static ShaderInstance atmosphereComposite;
        private ShaderEvents() {
        }

        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) {
            sceneShader = null;
            atmosphere = null;
            atmosphereComposite = null;
            AtmosphereRenderer.shaders(null, null);
            try {
                var shader = new ShaderInstance(event.getResourceProvider(),
                        new ResourceLocation(JemLightingSandbox.ID, "shadow_preview"), DefaultVertexFormat.POSITION_TEX);
                event.registerShader(shader, loaded -> sceneShader = loaded);
            } catch (IOException exception) {
                LOGGER.error("Could not load the JEM lighting preview shader", exception);
            }
            try {
                var scattering = new ShaderInstance(event.getResourceProvider(),
                        new ResourceLocation(JemLightingSandbox.ID, "atmosphere"), DefaultVertexFormat.POSITION_TEX);
                event.registerShader(scattering, loaded -> {
                    atmosphere = loaded;
                    AtmosphereRenderer.shaders(atmosphere, atmosphereComposite);
                });
                var compositing = new ShaderInstance(event.getResourceProvider(),
                        new ResourceLocation(JemLightingSandbox.ID, "atmosphere_composite"), DefaultVertexFormat.POSITION_TEX);
                event.registerShader(compositing, loaded -> {
                    atmosphereComposite = loaded;
                    AtmosphereRenderer.shaders(atmosphere, atmosphereComposite);
                });
            } catch (IOException exception) {
                LOGGER.error("Could not load the JEM atmosphere shaders", exception);
            }
        }
    }
}
