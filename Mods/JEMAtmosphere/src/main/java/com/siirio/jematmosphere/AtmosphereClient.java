package com.siirio.jematmosphere;

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
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = JemAtmosphere.ID, value = Dist.CLIENT)
public final class AtmosphereClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean effectsActive;

    private AtmosphereClient() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jematmosphere").executes(context -> {
            Minecraft client = Minecraft.getInstance();
            client.tell(() -> client.setScreen(AtmosphereOptions.open(client.screen)));
            return 1;
        }).then(Commands.literal("settings").executes(context -> {
            Minecraft client = Minecraft.getInstance();
            client.tell(() -> client.setScreen(AtmosphereOptions.open(client.screen)));
            return 1;
        })));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void prepareBiomeFog(ViewportEvent.ComputeFogColor event) {
        BiomeFog.prepare(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void keepTerrainFogAtEdge(ViewportEvent.RenderFog event) {
        BiomeFog.keepTerrainEdge(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderAtmosphere(RenderLevelStageEvent event) {
        AtmosphereRenderer.render(event);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!AtmosphereSettings.atmosphereEnabled()) {
            if (effectsActive) {
                clearAtmosphere();
                effectsActive = false;
            }
            return;
        }
        effectsActive = true;
        AtmosphereWeather.tick(client);
        WindGusts.tick(client);
        CanopyLeaves.tick(client);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        Minecraft client = Minecraft.getInstance();
        if (event.getLevel() == client.level) {
            client.execute(() -> {
                clearAtmosphere();
                effectsActive = false;
            });
        }
    }

    private static void clearAtmosphere() {
        WindGusts.clear();
        CanopyLeaves.clear();
        BiomeFog.clear();
        AtmospherePalette.clear();
        AtmosphereRenderer.clearWorldEffects();
        AtmosphereRenderer.releaseTargets();
    }

    @Mod.EventBusSubscriber(modid = JemAtmosphere.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ShaderEvents {
        private static ShaderInstance atmosphere;
        private static ShaderInstance atmosphereComposite;

        private ShaderEvents() {
        }

        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) {
            atmosphere = null;
            atmosphereComposite = null;
            AtmosphereRenderer.shaders(null, null);
            try {
                var scattering = new ShaderInstance(event.getResourceProvider(),
                        new ResourceLocation(JemAtmosphere.ID, "atmosphere"), DefaultVertexFormat.POSITION_TEX);
                event.registerShader(scattering, loaded -> {
                    atmosphere = loaded;
                    AtmosphereRenderer.shaders(atmosphere, atmosphereComposite);
                });
                var compositing = new ShaderInstance(event.getResourceProvider(),
                        new ResourceLocation(JemAtmosphere.ID, "atmosphere_composite"), DefaultVertexFormat.POSITION_TEX);
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
