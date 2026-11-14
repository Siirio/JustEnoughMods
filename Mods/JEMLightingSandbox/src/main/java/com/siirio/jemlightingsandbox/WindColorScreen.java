package com.siirio.jemlightingsandbox;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class WindColorScreen extends Screen {
    private static final String KEY = "screen.jem_lighting_sandbox.settings.wind.";
    private final Screen parent;
    private EditBox biome;
    private EditBox color;
    private Button save;
    private Button reset;
    private Component status = Component.empty();

    WindColorScreen(Screen parent) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String lastBiome = biome == null ? currentBiome() : biome.getValue();
        String lastColor = color == null ? null : color.getValue();
        int fieldWidth = Math.min(260, width - 24);
        int left = (width - fieldWidth) / 2;
        int top = Math.max(30, (height - 113) / 2);
        biome = addRenderableWidget(new EditBox(font, left, top, fieldWidth, 20, Component.translatable(KEY + "biome")));
        biome.setMaxLength(150);
        biome.setHint(Component.literal("minecraft:desert"));
        biome.setValue(lastBiome);
        color = addRenderableWidget(new EditBox(font, left, top + 34, fieldWidth, 20, Component.translatable(KEY + "color")));
        color.setMaxLength(7);
        color.setHint(Component.literal("#FFFFFF"));
        int half = (fieldWidth - 4) / 2;
        save = addRenderableWidget(Button.builder(Component.translatable(KEY + "save"), button -> apply(false))
                .bounds(left, top + 60, half, 20).build());
        reset = addRenderableWidget(Button.builder(Component.translatable(KEY + "reset"), button -> apply(true))
                .bounds(left + half + 4, top + 60, fieldWidth - half - 4, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, top + 93, fieldWidth, 20).build());
        biome.setResponder(value -> loadColor());
        color.setResponder(value -> updateButtons());
        if (lastColor == null) {
            loadColor();
        } else {
            color.setValue(lastColor);
        }
    }

    private String currentBiome() {
        if (minecraft.level == null || minecraft.player == null) {
            return "";
        }
        return minecraft.level.getBiome(minecraft.player.blockPosition()).unwrapKey()
                .map(key -> key.location().toString()).orElse("");
    }

    private ResourceLocation biomeId() {
        String value = biome.getValue().trim();
        if (!value.contains(":")) {
            return null;
        }
        var location = ResourceLocation.tryParse(value);
        if (location != null && minecraft.level != null
                && !minecraft.level.registryAccess().registryOrThrow(Registries.BIOME).containsKey(location)) {
            return null;
        }
        return location;
    }

    private void loadColor() {
        status = Component.empty();
        var id = biomeId();
        int override = LightingSettings.windColor(id);
        int selected = override;
        if (selected < 0 && id != null && minecraft.level != null) {
            var registry = minecraft.level.registryAccess().registryOrThrow(Registries.BIOME);
            selected = registry.getHolder(net.minecraft.resources.ResourceKey.create(Registries.BIOME, id))
                    .map(WindGusts::defaultTint).orElse(0xFFFFFF);
        }
        color.setValue(selected < 0 ? "" : String.format(Locale.ROOT, "#%06X", selected));
        updateButtons();
    }

    private void updateButtons() {
        var id = biomeId();
        save.active = id != null && color.getValue().matches("#[a-fA-F0-9]{6}");
        reset.active = id != null && LightingSettings.windColor(id) >= 0;
    }

    private void apply(boolean restore) {
        LightingSettings.setWindColor(biomeId(), restore ? -1 : Integer.parseInt(color.getValue().substring(1), 16));
        loadColor();
        status = Component.translatable(KEY + (restore ? "reset_done" : "saved"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 9, 0xFFFFFF);
        graphics.drawString(font, Component.translatable(KEY + "biome"), biome.getX(), biome.getY() - 10, 0xFFFFFF);
        graphics.drawString(font, Component.translatable(KEY + "color"), color.getX(), color.getY() - 10, 0xFFFFFF);
        graphics.drawCenteredString(font, status, width / 2, color.getY() + 50, 0xB3EBB9);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        biome.tick();
        color.tick();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
