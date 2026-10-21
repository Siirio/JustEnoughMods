package com.justenoughmods.achievementguide.client;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import com.justenoughmods.achievementguide.JemAdvancementGuide;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import vazkii.patchouli.api.PatchouliAPI;

public final class AdvancementScreenEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation GUIDE_BOOK = new ResourceLocation(JemAdvancementGuide.GUIDE_NAMESPACE, "just_enough_guide");
    private static final int CONTENT_X_OFFSET = 9;
    private static final int CONTENT_Y_OFFSET = 18;
    private static final int CONTENT_RIGHT_RESERVED = 87;
    private static final int CONTENT_BOTTOM_RESERVED = 97;
    private static Access access;
    private static boolean unavailable;

    private AdvancementScreenEvents() {
    }

    public static void directClick(BetterAdvancementsScreen screen, double mouseX, double mouseY, int button) {
        LOGGER.info("[JEM Advancements] BetterAdvancements direct click: x={}, y={}, button={}", mouseX, mouseY, button);
        tryOpen(screen, mouseX, mouseY, button);
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !(event.getScreen() instanceof BetterAdvancementsScreen screen)) {
            return;
        }
        LOGGER.info("[JEM Advancements] BetterAdvancements screen click event: x={}, y={}", event.getMouseX(), event.getMouseY());
        if (tryOpen(screen, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    public static boolean tryOpen(BetterAdvancementsScreen screen, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        Access fields = access();
        if (fields == null) {
            return false;
        }
        try {
            BetterAdvancementTab tab = (BetterAdvancementTab) fields.selectedTab.get(screen);
            if (tab == null) {
                LOGGER.warn("[JEM Advancements] Click ignored: BetterAdvancements has no selected tab");
                return false;
            }
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int internalWidth = fields.internalWidth.getInt(screen);
            int internalHeight = fields.internalHeight.getInt(screen);
            int windowX = 30 + (width - internalWidth) / 2;
            int windowY = 40 + (height - internalHeight) / 2;
            double localX = mouseX - windowX - CONTENT_X_OFFSET;
            double localY = mouseY - windowY - CONTENT_Y_OFFSET;
            int contentWidth = internalWidth - CONTENT_RIGHT_RESERVED;
            int contentHeight = internalHeight - CONTENT_BOTTOM_RESERVED;
            if (localX < 0 || localY < 0 || localX >= contentWidth || localY >= contentHeight) {
                return false;
            }
            int scrollX = fields.scrollX.getInt(tab);
            int scrollY = fields.scrollY.getInt(tab);
            for (BetterAdvancementWidget widget : widgets(fields, tab).values()) {
                Advancement advancement = widget.getAdvancement();
                ResourceLocation entry = guideEntry(advancement.getId());
                if (entry != null && isMouseOver(widget, scrollX, scrollY, localX, localY)) {
                    LOGGER.info("[JEM Advancements] Opening Patchouli entry {} for advancement {}", entry, advancement.getId());
                    PatchouliAPI.get().openBookEntry(GUIDE_BOOK, entry, 0);
                    return true;
                }
            }
            LOGGER.info("[JEM Advancements] Click did not hit a mapped advancement widget");
        } catch (IllegalAccessException | RuntimeException exception) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("jem_advancements.patchouli.open_failed"), false);
            LOGGER.warn("[JEM Advancements] Could not open advancement guide entry", exception);
        }
        return false;
    }

    private static boolean isMouseOver(BetterAdvancementWidget widget, int scrollX, int scrollY, double mouseX, double mouseY) {
        for (int offsetX = -4; offsetX <= 4; offsetX += 4) {
            for (int offsetY = -4; offsetY <= 4; offsetY += 4) {
                if (widget.isMouseOver(scrollX, scrollY, mouseX + offsetX, mouseY + offsetY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ResourceLocation guideEntry(ResourceLocation advancement) {
        if ("jem_guide".equals(advancement.getNamespace()) || "jem".equals(advancement.getNamespace())) {
            return new ResourceLocation(JemAdvancementGuide.GUIDE_NAMESPACE, advancement.getPath());
        }
        String resourcePath = "patchouli_books/just_enough_guide/en_us/entries/native/"
            + advancement.getNamespace() + "/" + advancement.getPath() + ".json";
        ResourceLocation resource = new ResourceLocation(JemAdvancementGuide.GUIDE_NAMESPACE, resourcePath);
        return Minecraft.getInstance().getResourceManager().getResource(resource).isPresent()
            ? new ResourceLocation(JemAdvancementGuide.GUIDE_NAMESPACE, "native/" + advancement.getNamespace() + "/" + advancement.getPath())
            : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Advancement, BetterAdvancementWidget> widgets(Access fields, BetterAdvancementTab tab) throws IllegalAccessException {
        return (Map<Advancement, BetterAdvancementWidget>) fields.widgets.get(tab);
    }

    private static synchronized Access access() {
        if (unavailable) {
            return null;
        }
        if (access != null) {
            return access;
        }
        try {
            access = new Access(
                field(BetterAdvancementsScreen.class, "selectedTab"),
                field(BetterAdvancementsScreen.class, "internalWidth"),
                field(BetterAdvancementsScreen.class, "internalHeight"),
                field(BetterAdvancementTab.class, "widgets"),
                field(BetterAdvancementTab.class, "scrollX"),
                field(BetterAdvancementTab.class, "scrollY")
            );
            return access;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable(exception);
            return null;
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static synchronized void disable(Exception exception) {
        if (!unavailable) {
            unavailable = true;
            access = null;
            LOGGER.warn("[JEM Advancements] BetterAdvancements guide click integration disabled: {}", exception.toString());
        }
    }

    private record Access(
        Field selectedTab,
        Field internalWidth,
        Field internalHeight,
        Field widgets,
        Field scrollX,
        Field scrollY
    ) {
    }
}
