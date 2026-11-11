package dev.taste.engine.client;

import com.google.gson.JsonPrimitive;
import dev.taste.engine.config.ConfigDraft;
import dev.taste.engine.config.ConfigStore;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.config.EffectiveConfig;
import dev.taste.engine.config.SettingRegistry;
import dev.taste.engine.config.SettingSpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact native editor for the complete setting registry. */
public class TasteConfigScreen extends Screen {
    private record VisibleRow(SettingSpec spec, int y) { }
    private final Screen parent;
    private final ConfigStore store;
    private final SettingRegistry registry;
    private ConfigDraft draft;
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final List<VisibleRow> rows = new ArrayList<>();
    private final List<EditBox> textEditors = new ArrayList<>();
    private final Map<String, String> invalidText = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();
    private EditBox search;
    private Button master;
    private Button categoryButton;
    private Button apply;
    private Button previous;
    private Button next;
    private String query = "";
    private String message = "";
    private int categoryIndex;
    private int page;
    private int pageCount = 1;
    private int rowCount = 1;
    private int left;
    private int panelWidth;
    private boolean refreshRows;

    public TasteConfigScreen(Screen parent, ConfigStore store) {
        super(Component.translatable("taste.ui.title"));
        this.parent = parent;
        this.store = store;
        this.registry = store.registry();
        this.draft = store.draft();
        if (!store.warnings().isEmpty()) message = store.warnings().get(0);
    }

    @Override protected void init() {
        panelWidth = Math.min(660, width - 20);
        left = (width - panelWidth) / 2;
        rowCount = Math.max(1, (height - 190) / 44);
        rowWidgets.clear();
        textEditors.clear();
        master = addRenderableWidget(Button.builder(masterText(), button -> {
            draft.set("general.enabled", new JsonPrimitive(!draft.value("general.enabled").getAsBoolean()));
            rebuildRows();
        }).bounds(left, 44, Math.min(190, panelWidth / 2), 20).build());
        master.setTooltip(Tooltip.create(Component.translatable("taste.ui.masterHelp")));
        search = addRenderableWidget(new EditBox(font, left, 70, panelWidth / 2 - 3, 20, Component.translatable("taste.ui.search")));
        search.setMaxLength(120);
        search.setValue(query);
        search.setHint(Component.translatable("taste.ui.search"));
        search.setResponder(text -> { query = text; page = 0; refreshRows = true; });
        categoryButton = addRenderableWidget(Button.builder(categoryText(), button -> {
            int count = registry.categories().size() + 1;
            categoryIndex = Math.floorMod(categoryIndex + (Screen.hasShiftDown() ? -1 : 1), count);
            page = 0;
            rebuildRows();
        }).bounds(left + panelWidth / 2 + 3, 70, panelWidth / 2 - 3, 20).build());
        categoryButton.setTooltip(Tooltip.create(Component.translatable("taste.ui.categoryHelp")));
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildRows(); })
                .bounds(left, height - 84, 30, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildRows(); })
                .bounds(left + panelWidth - 30, height - 84, 30, 20).build());
        int buttonWidth = (panelWidth - 12) / 4;
        addRenderableWidget(Button.builder(Component.translatable("taste.ui.resetPage"), b -> {
            if (categoryIndex == 0 || !query.isBlank()) for (VisibleRow row : List.copyOf(rows)) reset(row.spec().id());
            else {
                String category = registry.categories().get(categoryIndex - 1).id();
                registry.settings().values().stream().filter(s -> s.category().equals(category)).forEach(s -> reset(s.id()));
            }
            rebuildRows();
        }).bounds(left, height - 28, buttonWidth, 20).tooltip(Tooltip.create(Component.translatable("taste.ui.resetPageHelp"))).build());
        addRenderableWidget(Button.builder(Component.translatable("taste.ui.resetAll"), b -> {
            draft.resetAll(); invalidText.clear(); errors.clear(); message = ""; rebuildRows();
        }).bounds(left + buttonWidth + 4, height - 28, buttonWidth, 20).build());
        apply = addRenderableWidget(Button.builder(Component.translatable("taste.ui.apply"), b -> applyDraft())
                .bounds(left + (buttonWidth + 4) * 2, height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(left + (buttonWidth + 4) * 3, height - 28, buttonWidth, 20).build());
        rebuildRows();
    }

    private Component masterText() {
        return Component.translatable("taste.ui.master", Component.translatable(draft.value("general.enabled").getAsBoolean() ? "options.on" : "options.off"));
    }
    private Component categoryText() {
        return categoryIndex == 0 ? Component.translatable("taste.ui.allCategories")
                : Component.literal(registry.categories().get(categoryIndex - 1).label(language()));
    }
    private String language() { return minecraft == null ? "en_us" : minecraft.getLanguageManager().getSelected(); }
    private List<SettingSpec> filtered() {
        String[] terms = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        String category = categoryIndex == 0 ? "" : registry.categories().get(categoryIndex - 1).id();
        return registry.settings().values().stream().filter(spec -> {
            if (query.isBlank() && !category.isEmpty() && !category.equals(spec.category())) return false;
            String haystack = (spec.id() + " " + spec.label("en_us") + " " + spec.label("ru_ru") + " " + spec.description()).toLowerCase(Locale.ROOT);
            for (String term : terms) if (!haystack.contains(term)) return false;
            return true;
        }).toList();
    }
    private void rebuildRows() {
        rowWidgets.forEach(this::removeWidget);
        rowWidgets.clear(); rows.clear(); textEditors.clear();
        List<SettingSpec> matching = filtered();
        pageCount = Math.max(1, (matching.size() + rowCount - 1) / rowCount);
        page = Math.max(0, Math.min(pageCount - 1, page));
        Map<String, JsonPrimitive> values = draft.effective();
        for (int index = page * rowCount; index < Math.min(matching.size(), (page + 1) * rowCount); index++) {
            SettingSpec spec = matching.get(index);
            int y = 101 + (index - page * rowCount) * 44;
            rows.add(new VisibleRow(spec, y));
            createEditor(spec, values.get(spec.id()), y + 13);
        }
        previous.active = page > 0;
        next.active = page + 1 < pageCount;
        master.setMessage(masterText());
        categoryButton.setMessage(categoryText());
        updateApplyState();
        refreshRows = false;
    }
    private <T extends AbstractWidget> T rowWidget(T widget) {
        rowWidgets.add(widget);
        return addRenderableWidget(widget);
    }
    private void createEditor(SettingSpec spec, JsonPrimitive current, int y) {
        int editorWidth = panelWidth - 34;
        EffectiveConfig effective = EffectiveConfig.resolve(new ConfigSnapshot(-1, draft.style(), draft.quality(),
                Map.of(), draft.effective()), ClientState.capabilities());
        Tooltip tooltip = Tooltip.create(Component.literal(spec.id() + "\n" + spec.description() + "\n")
                .append(Component.translatable("taste.ui.consumerStatus"))
                .append(Component.literal("\n" + String.join(", ", registry.unmetRequirements(spec.id(), effective.values(), effective.capabilities())))));
        if (spec.type() == SettingSpec.Type.BOOLEAN) {
            rowWidget(Button.builder(Component.translatable(current.getAsBoolean() ? "options.on" : "options.off"), button -> {
                draft.set(spec.id(), new JsonPrimitive(!draft.value(spec.id()).getAsBoolean()));
                rebuildRows();
            }).bounds(left, y, editorWidth, 20).tooltip(tooltip).build());
        } else if (spec.type() == SettingSpec.Type.ENUM) {
            rowWidget(Button.builder(Component.literal(current.getAsString()), button -> {
                int index = spec.options().indexOf(draft.value(spec.id()).getAsString());
                do { index = Math.floorMod(index + (Screen.hasShiftDown() ? -1 : 1), spec.options().size()); }
                while (spec.id().equals("general.backend") && ClientState.embeddiumInstalled() && spec.options().get(index).equals("vanilla"));
                draft.set(spec.id(), new JsonPrimitive(spec.options().get(index)));
                rebuildRows();
            }).bounds(left, y, editorWidth, 20).tooltip(tooltip).build());
        } else {
            int textWidth = spec.numeric() ? Math.max(72, editorWidth / 3) : editorWidth;
            int textX = spec.numeric() ? left + editorWidth - textWidth : left;
            EditBox editor = rowWidget(new EditBox(font, textX, y, textWidth, 20, Component.literal(spec.label(language()))));
            editor.setMaxLength(40);
            editor.setValue(invalidText.getOrDefault(spec.id(), display(current)));
            editor.setTextColor(errors.containsKey(spec.id()) ? 0xFF7777 : 0xFFFFFF);
            editor.setTooltip(tooltip);
            editor.setResponder(text -> {
                try {
                    draft.set(spec.id(), spec.parseEditorText(text));
                    errors.remove(spec.id()); invalidText.remove(spec.id()); editor.setTextColor(0xFFFFFF);
                } catch (IllegalArgumentException failure) {
                    errors.put(spec.id(), failure.getMessage()); invalidText.put(spec.id(), text); editor.setTextColor(0xFF7777);
                }
                updateApplyState();
            });
            textEditors.add(editor);
            if (spec.numeric()) {
                NumericSlider slider = rowWidget(new NumericSlider(left, y, editorWidth - textWidth - 4, spec, current.getAsDouble(),
                        value -> editor.setValue(display(value))));
                slider.setTooltip(tooltip);
            }
        }
        rowWidget(Button.builder(Component.literal("R"), button -> { reset(spec.id()); rebuildRows(); })
                .bounds(left + panelWidth - 28, y, 28, 20)
                .tooltip(Tooltip.create(Component.translatable("taste.ui.resetOne"))).build());
    }
    private void reset(String id) {
        draft.reset(id); invalidText.remove(id); errors.remove(id); message = "";
    }
    private void updateApplyState() {
        if (apply != null) apply.active = errors.isEmpty();
    }
    private void applyDraft() {
        if (!errors.isEmpty()) return;
        try {
            store.apply(draft);
            draft = store.draft();
            message = Component.translatable("taste.ui.saved").getString();
            rebuildRows();
        } catch (IOException | RuntimeException failure) {
            message = failure.getMessage() == null ? Component.translatable("taste.ui.saveFailed").getString() : failure.getMessage();
        }
    }
    @Override public void tick() {
        if (search != null) search.tick();
        textEditors.forEach(EditBox::tick);
        if (refreshRows) rebuildRows();
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("taste.ui.rendererStage"), width / 2, 27, 0x72D8E8);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(ClientState.rendererAvailable()
                        ? "taste.ui.statusOn" : "taste.ui.statusPending").getString(), panelWidth / 2 - 6),
                left + panelWidth / 2 + 3, 50, 0xAAAAAA, false);
        for (VisibleRow row : rows) {
            String label = (draft.hasOverride(row.spec().id()) ? "* " : "") + row.spec().label(language());
            if (!row.spec().unit().isBlank()) label += " (" + row.spec().unit() + ")";
            graphics.drawString(font, font.plainSubstrByWidth(label, panelWidth), left, row.y(), 0xE6E6E6, false);
        }
        if (rows.isEmpty()) graphics.drawCenteredString(font, Component.translatable("taste.ui.noResults"), width / 2, 110, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.translatable("taste.ui.page", page + 1, pageCount, draft.dirty() ? "*" : ""),
                width / 2, height - 78, 0xCCCCCC);
        String status = errors.isEmpty() ? message : errors.values().iterator().next();
        graphics.drawString(font, font.plainSubstrByWidth(status, panelWidth), left, height - 55, errors.isEmpty() ? 0xAACCAA : 0xFF7777, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    private static String display(JsonPrimitive value) {
        if (!value.isNumber()) return value.getAsString();
        return BigDecimal.valueOf(value.getAsDouble()).stripTrailingZeros().toPlainString();
    }

    private static final class NumericSlider extends AbstractSliderButton {
        private final SettingSpec spec;
        private final Consumer<JsonPrimitive> changed;
        NumericSlider(int x, int y, int width, SettingSpec spec, double initial, Consumer<JsonPrimitive> changed) {
            super(x, y, width, 20, Component.empty(), (initial - spec.minimum()) / Math.max(1.0e-12, spec.maximum() - spec.minimum()));
            this.spec = spec;
            this.changed = changed;
            updateMessage();
        }
        private JsonPrimitive selected() {
            double raw = spec.minimum() + value * (spec.maximum() - spec.minimum());
            // Decimal arithmetic keeps a 0.01 slider step from producing long binary tails in saved JSON.
            BigDecimal step = BigDecimal.valueOf(spec.step());
            BigDecimal minimum = BigDecimal.valueOf(spec.minimum());
            double stepped = BigDecimal.valueOf(raw).subtract(minimum).divide(step, 0, RoundingMode.HALF_UP).multiply(step).add(minimum).doubleValue();
            return spec.validate(new JsonPrimitive(Math.max(spec.minimum(), Math.min(spec.maximum(), stepped))));
        }
        @Override protected void updateMessage() { if (spec != null) setMessage(Component.literal(display(selected()))); }
        @Override protected void applyValue() { if (changed != null) changed.accept(selected()); }
    }
}
