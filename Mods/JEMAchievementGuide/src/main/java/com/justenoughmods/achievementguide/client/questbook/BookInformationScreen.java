package com.justenoughmods.achievementguide.client.questbook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Short reference cards outside quest completion; the native book remains the parent. */
public final class BookInformationScreen extends Screen {
    private static final JsonObject CARDS = load();
    private final Screen parent;
    private final List<String> paragraphs;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int scroll;
    private int left, top, contentWidth, contentHeight;

    private BookInformationScreen(Screen parent, JsonObject card) {
        super(dev.ftb.mods.ftbquests.util.TextUtils.parseRawText(card.get("title").getAsString()));
        this.parent = parent;
        paragraphs = new ArrayList<>();
        card.getAsJsonArray("paragraphs").forEach(value -> paragraphs.add(value.getAsString()));
    }

    private static JsonObject load() {
        try (var stream = BookInformationScreen.class.getResourceAsStream("/data/jem_advancements/questbook/information.json")) {
            if (stream == null) throw new IllegalStateException("Missing quest reference cards");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read quest reference cards", failure); }
    }

    public static void open(String id) {
        JsonObject card = CARDS.getAsJsonObject(id);
        if (card != null) Minecraft.getInstance().setScreen(new BookInformationScreen(Minecraft.getInstance().screen, card));
    }

    @Override protected void init() {
        contentWidth = Math.max(100, Math.min(500, width - 48));
        contentHeight = Math.max(60, height - 104);
        left = (width - contentWidth) / 2;
        top = 48;
        lines.clear();
        for (String paragraph : paragraphs) {
            lines.addAll(font.split(dev.ftb.mods.ftbquests.util.TextUtils.parseRawText(paragraph), contentWidth - 24));
            lines.add(FormattedCharSequence.EMPTY);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 60, height - 35, 120, 20).build());
        clampScroll();
    }

    private void clampScroll() { scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() * 12 - contentHeight + 16))); }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        scroll -= (int)(amount * 24); clampScroll(); return true;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left - 8, top - 8, left + contentWidth + 8, top + contentHeight + 8, 0xE0101010);
        graphics.drawCenteredString(font, title, width / 2, 22, 0xFFE6BF);
        graphics.enableScissor(left, top, left + contentWidth, top + contentHeight);
        for (int i = 0; i < lines.size(); i++) {
            int y = top + 8 + i * 12 - scroll;
            if (y + 12 >= top && y < top + contentHeight) graphics.drawString(font, lines.get(i), left + 12, y, 0xEEEEEE, false);
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
