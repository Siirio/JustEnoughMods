package com.justenoughmods.achievementguide.client.handbook;

import com.justenoughmods.achievementguide.client.handbook.manuals.NativeManuals;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.common.book.Book;

/** A paginated native Patchouli spread; each card opens the actual manual owner. */
final class HandbookManualScreen extends GuiBook {
    private final List<ManualCard> cards = new ArrayList<>();
    private EditBox search;
    private String query = "";
    private static final int PAGE_SIZE = 8;

    HandbookManualScreen(Book book) {
        super(book, Component.translatable("jem_advancements.handbook.section.manuals"));
    }

    @Override
    public void init() {
        if (search != null) query = search.getValue();
        super.init();
        search = new EditBox(font, bookLeft + 20, bookTop + 21, 228, 16,
                Component.translatable("jem_advancements.handbook.search_manuals"));
        search.setMaxLength(128);
        search.setValue(query);
        search.setResponder(value -> { query = value; spread = 0; rebuildCards(); });
        addRenderableWidget(search);
        rebuildCards();
    }

    private void rebuildCards() {
        removeDrawablesIn(cards);
        cards.clear();
        String needle = query.strip().toLowerCase(Locale.ROOT);
        var manuals = NativeManuals.available().stream().filter(m -> m.title().getString().toLowerCase(Locale.ROOT).contains(needle)
                || m.aliases().toLowerCase(Locale.ROOT).contains(needle) || m.id().toLowerCase(Locale.ROOT).contains(needle)).toList();
        maxSpreads = Math.max(1, (manuals.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        spread = Math.min(spread, maxSpreads - 1);
        for (int i = spread * PAGE_SIZE; i < Math.min(manuals.size(), (spread + 1) * PAGE_SIZE); i++) {
            int slot = i % PAGE_SIZE;
            ManualCard card = new ManualCard(manuals.get(i), bookLeft + 17 + (slot / 4) * 126,
                    bookTop + 46 + (slot % 4) * 28);
            cards.add(card);
            addRenderableWidget(card);
        }
    }

    @Override
    public void handleButtonArrow(Button button) {
        super.handleButtonArrow(button);
        rebuildCards();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean result = super.mouseScrolled(mouseX, mouseY, delta);
        rebuildCards();
        return result;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (search.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && search.keyPressed(key, scan, modifiers)) return true;
        int previousSpread = spread;
        boolean result = super.keyPressed(key, scan, modifiers);
        if (spread != previousSpread) rebuildCards();
        return result;
    }

    @Override
    public boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (search.mouseClicked(mouseX, mouseY, button)) {
            setFocused(search);
            return true;
        }
        search.setFocused(false);
        int previousSpread = spread;
        boolean result = super.mouseClickedScaled(mouseX, mouseY, button);
        if (spread != previousSpread) rebuildCards();
        return result;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return search.isFocused() ? search.charTyped(codePoint, modifiers) : super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        search.tick();
    }

    private final class ManualCard extends Button {
        private final NativeManuals.Manual manual;
        ManualCard(NativeManuals.Manual manual, int x, int y) {
            super(x, y, 112, 24, manual.title(), b -> HandbookNavigation.openManual(HandbookManualScreen.this, manual),
                    supplier -> manual.title().copy().append(". ").append(manual.status()));
            this.manual = manual;
            active = manual.enabled();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.renderItem(manual.icon(), getX() + 1, getY() + 3);
            int color = active ? (isHoveredOrFocused() ? book.linkHoverColor : book.linkColor) : 0x777777;
            var lines = font.split(getMessage(), 91);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(font, lines.get(i), getX() + 20, getY() + 2 + i * 10, color, false);
            }
            if (isHoveredOrFocused()) HandbookManualScreen.this.setTooltip(manual.title(), manual.status());
        }
    }
}
