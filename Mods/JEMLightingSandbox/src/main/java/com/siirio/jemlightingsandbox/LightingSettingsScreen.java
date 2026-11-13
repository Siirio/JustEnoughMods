package com.siirio.jemlightingsandbox;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = JemLightingSandbox.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LightingSettingsScreen extends Screen {
    private static final String SETTINGS = "screen.jem_lighting_sandbox.settings.";
    private static final int CONTROL_HEIGHT = 20;
    private static final int ROW_SPACING = 24;
    private final Screen parent;

    public LightingSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable(SETTINGS + "title"));
        this.parent = parent;
    }

    @SubscribeEvent
    public static void registerConfigScreen(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModList.get().getModContainerById(JemLightingSandbox.ID).ifPresent(container ->
                container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory(LightingSettingsScreen::new))));
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(260, width - 24);
        int left = (width - buttonWidth) / 2;
        int top = Math.max(30, (height - 104) / 2);

        addRenderableWidget(CycleButton.builder(LightingSettingsScreen::modeLabel)
                .withValues(LightingSettings.Mode.values())
                .withInitialValue(LightingSettings.mode())
                .withTooltip(mode -> Tooltip.create(Component.translatable(modeKey(mode) + ".tooltip")))
                .create(left, top, buttonWidth, CONTROL_HEIGHT, Component.translatable(SETTINGS + "mode"),
                        (button, mode) -> LightingSettings.setMode(mode)));

        addRenderableWidget(CycleButton.<Integer>builder(distance -> Component.translatable(SETTINGS + "blocks", distance))
                .withValues(LightingSettings.LIGHT_DISTANCES)
                .withInitialValue(LightingSettings.lightDistance())
                .withTooltip(distance -> Tooltip.create(Component.translatable(SETTINGS + "distance.tooltip")))
                .create(left, top + ROW_SPACING, buttonWidth, CONTROL_HEIGHT,
                        Component.translatable(SETTINGS + "distance"),
                        (button, distance) -> LightingSettings.setLightDistance(distance)));

        addRenderableWidget(CycleButton.onOffBuilder(LightingSettings.atmosphereEnabled())
                .withTooltip(enabled -> Tooltip.create(Component.translatable(SETTINGS + "atmosphere.tooltip")))
                .create(left, top + 2 * ROW_SPACING, buttonWidth, CONTROL_HEIGHT,
                        Component.translatable(SETTINGS + "atmosphere"),
                        (button, enabled) -> LightingSettings.setAtmosphereEnabled(enabled)));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, top + 3 * ROW_SPACING + 8, buttonWidth, CONTROL_HEIGHT).build());
    }

    private static Component modeLabel(LightingSettings.Mode mode) {
        return Component.translatable(modeKey(mode));
    }

    private static String modeKey(LightingSettings.Mode mode) {
        return SETTINGS + "mode." + mode.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, Math.max(10, (height - 104) / 2 - 20), 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
