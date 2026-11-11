package dev.taste.engine.client;

import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import dev.taste.engine.TasteMod;
import dev.taste.engine.config.ConfigDraft;
import dev.taste.engine.config.ConfigStore;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private ClientEvents() { }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientRegistration.OPEN_MENU.consumeClick()) {
            if (minecraft.screen == null && minecraft.level != null)
                minecraft.setScreen(new EngineSettingsScreen(null, ClientState.store()));
        }
        while (ClientRegistration.TOGGLE_MASTER.consumeClick()) {
            if (minecraft.screen != null || minecraft.level == null) continue;
            ConfigStore store = ClientState.store();
            ConfigDraft draft = store.draft();
            draft.set("general.enabled", new JsonPrimitive(!store.current().requestedEnabled()));
            try {
                store.apply(draft);
                if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("taste.ui.saved"), true);
            } catch (IOException | RuntimeException failure) {
                LogUtils.getLogger().error("Could not save Taste master preference", failure);
                if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("taste.ui.saveFailed"), true);
            }
        }
    }
}
