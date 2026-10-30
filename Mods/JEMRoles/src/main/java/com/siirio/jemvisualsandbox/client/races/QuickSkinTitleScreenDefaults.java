package com.siirio.jemvisualsandbox.client.races;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
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
    private static final int TITLE_PREVIEW_SIZE = 25;
    private static final int TITLE_PREVIEW_OFFSET_X = -432;
    private static final int TITLE_PREVIEW_OFFSET_Y = 104;

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

            applyManagedTitlePreviewConfig();
            Method rotation = widget.getClass().getMethod("setRotationState", float.class, float.class);
            rotation.invoke(widget, DEFAULT_YAW, DEFAULT_YAW);
            restoreLegacyReferencePoint(event, widget);
        } catch (ReflectiveOperationException ignored) {
            // Quick Skin is optional and owns its widget lifecycle
        }
    }

    private static void applyManagedTitlePreviewConfig() throws ReflectiveOperationException {
        Minecraft minecraft = Minecraft.getInstance();
        boolean managedLayoutSelected = minecraft.options.resourcePacks.stream()
                .anyMatch(pack -> pack.contains("JEM Release")
                        || pack.contains("JEM Legacy")
                        || pack.contains("JEM Jalloween"));
        if (!managedLayoutSelected) {
            return;
        }

        Class<?> clientConfig = Class.forName("com.quickskin.mod.config.ClientConfig");
        Object config = clientConfig.getMethod("getInstance").invoke(null);
        clientConfig.getField("sizeModelPreviewPercentageTitleScreen").setInt(config, TITLE_PREVIEW_SIZE);
        clientConfig.getField("positionOffsetXTitleScreen").setInt(config, TITLE_PREVIEW_OFFSET_X);
        clientConfig.getField("positionOffsetYTitleScreen").setInt(config, TITLE_PREVIEW_OFFSET_Y);
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
