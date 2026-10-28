package com.siirio.jemguisandbox;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = "jem_gui_sandbox", value = Dist.CLIENT)
public final class QuickSkinTitleScreenDefaults {
    private QuickSkinTitleScreenDefaults() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void initializeQuickSkinPose(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || !ModList.get().isLoaded("quickskin")) {
            return;
        }

        try {
            Class<?> clientEvents = Class.forName("com.quickskin.mod.event.ClientEvents");
            Field playerWidget = clientEvents.getDeclaredField("playerWidget");
            playerWidget.setAccessible(true);
            Object widget = playerWidget.get(null);
            if (widget != null) {
                Method setRotationState = widget.getClass().getMethod("setRotationState", float.class, float.class);
                setRotationState.invoke(widget, -20.0F, -20.0F);
            }
        } catch (ReflectiveOperationException ignored) {
            // Quick Skin is optional and owns its widget implementation.
        }
    }
}
