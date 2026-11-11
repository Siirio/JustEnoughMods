package dev.taste.engine.client;

import dev.taste.engine.client.ui.DiagnosticsScreen;
import dev.taste.engine.client.ui.PresetDiffScreen;
import dev.taste.engine.config.ConfigStore;
import java.util.Map;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EngineSettingsScreen extends TasteConfigScreen {
    public EngineSettingsScreen(Screen parent, ConfigStore store) { super(parent, store); }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("taste.ui.diagnostics"), button -> minecraft.setScreen(new DiagnosticsScreen(this))).bounds(width-112,8,104,20).build());
        addRenderableWidget(Button.builder(Component.translatable("taste.ui.presetDiff"), button -> minecraft.setScreen(new PresetDiffScreen(this, Map.of()))).bounds(8,8,104,20).build());
    }
}
