package dev.taste.engine.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taste.engine.TasteMod;
import dev.taste.engine.render.TasteRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRegistration {
    public static final KeyMapping OPEN_MENU = new KeyMapping("key.taste.menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6, "key.categories.taste");
    public static final KeyMapping TOGGLE_MASTER = new KeyMapping("key.taste.master", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7, "key.categories.taste");
    private ClientRegistration() { }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new TasteConfigScreen(parent, ClientState.store())));
        event.enqueueWork(ClientState::initialize);
    }
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        event.register(TOGGLE_MASTER);
    }

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> TasteRenderer.instance().invalidateResources());
    }
}
