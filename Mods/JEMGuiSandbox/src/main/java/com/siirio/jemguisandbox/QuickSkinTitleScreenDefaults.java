package com.siirio.jemguisandbox;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.Minecraft;
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
                restoreLegacyReferencePoint(event, widget);
            }
        } catch (ReflectiveOperationException ignored) {
            // Quick Skin is optional and owns its widget implementation.
        }
    }

    private static void restoreLegacyReferencePoint(ScreenEvent.Init.Post event, Object widget)
            throws ReflectiveOperationException {
        Minecraft minecraft = Minecraft.getInstance();
        boolean legacySelected = minecraft.options.resourcePacks.stream()
                .anyMatch(pack -> pack.contains("JEM Legacy"));
        if (!legacySelected) {
            return;
        }

        Class<?> clientConfig = Class.forName("com.quickskin.mod.config.ClientConfig");
        Object config = clientConfig.getMethod("getInstance").invoke(null);
        Field offsetX = clientConfig.getField("positionOffsetXTitleScreen");
        Field offsetY = clientConfig.getField("positionOffsetYTitleScreen");
        int referenceX = event.getScreen().width / 2 + 186 - offsetX.getInt(config);
        int referenceY = event.getScreen().height / 4 + 132 + 25 - offsetY.getInt(config);
        Method setCustomReferencePoint = widget.getClass().getMethod("setCustomReferencePoint", int.class, int.class);
        setCustomReferencePoint.invoke(widget, referenceX, referenceY);
    }
}
