package com.siirio.jemvisualsandbox.client.races;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_roles", value = Dist.CLIENT)
public final class QuickSkinTitleScreenDefaults {
    private static final float DEFAULT_YAW = -20.0F;

    private QuickSkinTitleScreenDefaults() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterTitleScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || !ModList.get().isLoaded("quickskin")) {
            return;
        }

        try {
            Class<?> eventsType = Class.forName("com.quickskin.mod.event.ClientEvents");
            Field widgetField = eventsType.getDeclaredField("playerWidget");
            widgetField.setAccessible(true);
            Object widget = widgetField.get(null);
            if (widget == null) {
                return;
            }

            Method rotation = widget.getClass().getMethod("setRotationState", float.class, float.class);
            rotation.invoke(widget, DEFAULT_YAW, DEFAULT_YAW);
        } catch (ReflectiveOperationException ignored) {
            // Quick Skin is optional and owns its widget lifecycle
        }
    }
}
