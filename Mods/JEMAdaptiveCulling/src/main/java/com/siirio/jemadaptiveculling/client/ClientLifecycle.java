package com.siirio.jemadaptiveculling.client;

import com.siirio.jemadaptiveculling.JEMAdaptiveCulling;
import com.siirio.jemadaptiveculling.config.JEMConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = JEMAdaptiveCulling.MOD_ID, value = Dist.CLIENT)
public final class ClientLifecycle {
    private ClientLifecycle() {
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) VisibilityCache.endFrame();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VisibilityCache.clear();
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) VisibilityCache.unload(level);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) VisibilityCache.remove(event.getEntity());
    }

    @Mod.EventBusSubscriber(modid = JEMAdaptiveCulling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Resources {
        private Resources() {
        }

        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) resources -> {
                VisibilityCache.clear();
                CompatibilityPolicy.reload();
            });
        }

        @SubscribeEvent
        public static void config(ModConfigEvent event) {
            if (event.getConfig().getSpec() == JEMConfig.SPEC && event instanceof ModConfigEvent.Reloading) {
                Minecraft.getInstance().execute(() -> {
                    VisibilityCache.clear();
                    CompatibilityPolicy.reload();
                });
            }
        }
    }
}
