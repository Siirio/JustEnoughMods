package com.siirio.jemlightingsandbox;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.gui.widget.ForgeSlider;
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
    private static final int ROWS_HEIGHT = 6 * ROW_SPACING + 8 + CONTROL_HEIGHT;
    private final Screen parent;
    private ChunkDistanceSlider chunkDistance;

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
        int top = Math.max(30, (height - ROWS_HEIGHT) / 2);

        addRenderableWidget(CycleButton.builder(LightingSettingsScreen::modeLabel)
                .withValues(LightingSettings.Mode.values())
                .withInitialValue(LightingSettings.mode())
                .withTooltip(mode -> Tooltip.create(Component.translatable(modeKey(mode) + ".tooltip")))
                .create(left, top, buttonWidth, CONTROL_HEIGHT, Component.translatable(SETTINGS + "mode"),
                        (button, mode) -> LightingSettings.setMode(mode)));

        chunkDistance = addRenderableWidget(new ChunkDistanceSlider(left, top + ROW_SPACING, buttonWidth));
        updateChunkDistanceControl();
        var followRenderDistance = addRenderableWidget(new Checkbox(left, top + 2 * ROW_SPACING, buttonWidth,
                CONTROL_HEIGHT, Component.translatable(SETTINGS + "follow_render_distance"),
                LightingSettings.followsRenderDistance()) {
            @Override
            public void onPress() {
                super.onPress();
                LightingSettings.setFollowsRenderDistance(selected());
                updateChunkDistanceControl();
            }
        });
        followRenderDistance.setTooltip(Tooltip.create(Component.translatable(SETTINGS + "follow_render_distance.tooltip")));

        addRenderableWidget(CycleButton.<Integer>builder(distance -> Component.translatable(SETTINGS + "blocks", distance))
                .withValues(LightingSettings.LIGHT_DISTANCES)
                .withInitialValue(LightingSettings.lightDistance())
                .withTooltip(distance -> Tooltip.create(Component.translatable(SETTINGS + "distance.tooltip")))
                .create(left, top + 3 * ROW_SPACING, buttonWidth, CONTROL_HEIGHT,
                        Component.translatable(SETTINGS + "distance"),
                        (button, distance) -> LightingSettings.setLightDistance(distance)));

        int atmosphereWidth = (buttonWidth - 4) / 2;
        addRenderableWidget(CycleButton.onOffBuilder(LightingSettings.atmosphereEnabled())
                .withTooltip(enabled -> Tooltip.create(Component.translatable(SETTINGS + "atmosphere.tooltip")))
                .create(left, top + 4 * ROW_SPACING, atmosphereWidth, CONTROL_HEIGHT,
                        Component.translatable(SETTINGS + "atmosphere"),
                        (button, enabled) -> LightingSettings.setAtmosphereEnabled(enabled)));
        addRenderableWidget(CycleButton.<Integer>builder(resolution -> Component.literal(resolution + " px"))
                .withValues(LightingSettings.SHADOW_RESOLUTIONS)
                .withInitialValue(LightingSettings.shadowResolution())
                .withTooltip(resolution -> Tooltip.create(Component.translatable(SETTINGS + "shadow_resolution.tooltip")))
                .create(left + atmosphereWidth + 4, top + 4 * ROW_SPACING, buttonWidth - atmosphereWidth - 4,
                        CONTROL_HEIGHT, Component.translatable(SETTINGS + "shadow_resolution"),
                        (button, resolution) -> LightingSettings.setShadowResolution(resolution)));

        int sectionWidth = (buttonWidth - 8) / 3;
        addRenderableWidget(Button.builder(Component.translatable(SETTINGS + "fog"),
                button -> minecraft.setScreen(new FogSettingsScreen(this)))
                .bounds(left, top + 5 * ROW_SPACING, sectionWidth, CONTROL_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable(SETTINGS + "precipitation"),
                button -> minecraft.setScreen(new WeatherDensityScreen(this)))
                .bounds(left + sectionWidth + 4, top + 5 * ROW_SPACING, sectionWidth, CONTROL_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable(SETTINGS + "wind_colors"),
                button -> minecraft.setScreen(new WindColorScreen(this)))
                .bounds(left + 2 * sectionWidth + 8, top + 5 * ROW_SPACING, buttonWidth - 2 * sectionWidth - 8,
                        CONTROL_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, top + 6 * ROW_SPACING + 8, buttonWidth, CONTROL_HEIGHT).build());
    }

    private void updateChunkDistanceControl() {
        chunkDistance.active = !LightingSettings.followsRenderDistance();
        int distance = LightingSettings.lightingChunks(minecraft.options.getEffectiveRenderDistance());
        if (distance != chunkDistance.getValueInt()) {
            chunkDistance.setValue(distance);
        }
    }

    @Override
    public void tick() {
        if (LightingSettings.followsRenderDistance()) {
            updateChunkDistanceControl();
        }
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
        graphics.drawCenteredString(font, title, width / 2, Math.max(10, (height - ROWS_HEIGHT) / 2 - 20), 0xFFFFFFFF);
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

    private static final class ChunkDistanceSlider extends ForgeSlider {
        ChunkDistanceSlider(int x, int y, int width) {
            super(x, y, width, CONTROL_HEIGHT, Component.empty(), Component.empty(), LightingSettings.MIN_LIGHTING_CHUNKS,
                    LightingSettings.MAX_LIGHTING_CHUNKS, LightingSettings.requestedChunkDistance(), true);
            setTooltip(Tooltip.create(Component.translatable(SETTINGS + "lighting_chunks.tooltip")));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(SETTINGS + "lighting_chunks", getValueInt()));
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            if (active) {
                LightingSettings.setRequestedChunkDistance(getValueInt());
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!active || !super.keyPressed(keyCode, scanCode, modifiers)) {
                return false;
            }
            LightingSettings.setRequestedChunkDistance(getValueInt());
            return true;
        }
    }
}
