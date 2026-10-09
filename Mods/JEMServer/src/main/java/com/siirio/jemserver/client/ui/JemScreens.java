package com.siirio.jemserver.client.ui;

import com.siirio.jemserver.JemServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemServer.MOD_ID, value = Dist.CLIENT)
public final class JemScreens {
    private static final String PREFIX = "jem.ui.";

    private JemScreens() {}

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof AbstractContainerScreen<?> original)
                || original instanceof JemMenuScreen
                || !(original.getTitle().getContents() instanceof TranslatableContents title)
                || !title.getKey().startsWith(PREFIX)) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !(original.getMenu() instanceof ChestMenu || original.getMenu() instanceof AnvilMenu)) return;
        event.setNewScreen(new JemMenuScreen(original.getMenu(), player.getInventory(), original.getTitle(), title.getKey().substring(PREFIX.length())));
    }
}
