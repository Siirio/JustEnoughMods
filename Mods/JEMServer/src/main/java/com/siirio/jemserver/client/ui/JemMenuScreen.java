package com.siirio.jemserver.client.ui;

import static com.siirio.jemserver.client.ui.JemPalette.*;

import com.siirio.jemserver.client.smp.ClaimsScreenBridge;
import com.siirio.jemserver.client.smp.SmpGuiAssets;
import com.siirio.jemserver.client.smp.SmpSidebar;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class JemMenuScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final int PADDING = 12;
    private static final int HEADER_HEIGHT = 39;
    private static final int FOOTER_HEIGHT = 38;
    private static final int ROW_GAP = 2;
    private static final int COMPACT_ROW_HEIGHT = 28;
    private static final int DETAIL_ROW_HEIGHT = 38;
    private static final int DETAIL_LINE_HEIGHT = 12;
    private static final int SUMMARY_COLUMNS_WIDTH = 360;
    private static final int CHEST_SIZE = 54;
    private static final int ICON_SIZE = 16;
    private static final int BUTTON_ICON_PADDING = 6;
    private static final int BUTTON_TEXT_GAP = 5;
    private static final int BACK_ACTION = -3;
    private final SmpSidebar sidebar = new SmpSidebar();
    private boolean claimsContext;
    private String kind;
    private String displayTitle;
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentBottom;
    private EditBox input;
    private EditBox search;
    private boolean inputLoaded;
    private boolean inputChanged;
    private ItemStack hovered = ItemStack.EMPTY;
    private net.minecraft.nbt.CompoundTag mapLocation;
    private int previewSlot = -1;
    private static final int MAP_ACTION = -2;

    private record Hit(int slot, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    public JemMenuScreen(AbstractContainerMenu menu, Inventory inventory, Component title, String kind) {
        super(menu, inventory, title);
        this.kind = kind;
        claimsContext = claimKind(kind) || ClaimsScreenBridge.active()
                || net.minecraft.client.Minecraft.getInstance().screen instanceof JemMenuScreen previous && previous.claimsContext;
        this.displayTitle = title.getString();
    }

    @Override
    protected void init() {
        String previous = input == null ? null : input.getValue();
        String previousSearch = search == null ? "" : search.getValue();
        imageWidth = claimsContext ? width - 20 : Math.min(590, width - 20);
        imageHeight = claimsContext ? height - 20 : Math.min(370, height - 18);
        super.init();
        if (!(menu instanceof AnvilMenu)) {
            search = new EditBox(font, 0, 0, 100, 20, Component.translatable("jem.smp.search"));
            search.setHint(Component.translatable("jem.smp.search"));
            search.setMaxLength(16);
            search.setValue(previousSearch);
            search.setResponder(value -> scroll = 0);
            addRenderableWidget(search);
        }
        if (claimsContext) sidebar.layout(leftPos + 8, topPos + 40, sidebarWidth() - 8, imageHeight - 88);
        if (menu instanceof AnvilMenu) {
            int fieldWidth = Math.max(60, Math.min(320, imageWidth - PADDING * 4 - (claimsContext ? sidebarWidth() : 0)));
            int inputLeft = leftPos + (imageWidth - fieldWidth + (claimsContext ? sidebarWidth() : 0)) / 2;
            input = new EditBox(font, inputLeft, topPos + imageHeight / 2 - 10, fieldWidth, 22, title);
            input.setMaxLength(50);
            input.setTextColor(CREAM);
            if (previous != null) input.setValue(previous);
            input.setResponder(value -> {
                inputChanged = true;
                if (minecraft.getConnection() != null) minecraft.getConnection().send(new ServerboundRenameItemPacket(value));
            });
            addRenderableWidget(input);
            setInitialFocus(input);
        }
    }

    @Override
    protected void containerTick() {
        if (search != null) search.tick();
        if (input != null) {
            input.tick();
            if (!inputLoaded && !menu.getSlot(0).getItem().isEmpty()) {
                inputLoaded = true;
                if (!inputChanged) {
                    input.setValue(menu.getSlot(0).getItem().getHoverName().getString());
                    input.setHighlightPos(0);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        syncView();
        renderBackground(graphics);
        hits.clear();
        hovered = ItemStack.EMPTY;
        frame(graphics, leftPos, topPos, imageWidth, imageHeight, PANEL);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + HEADER_HEIGHT, INSET);
        emblem(graphics, leftPos + 14, topPos + 13);
        text(graphics, displayTitle, leftPos + 37, topPos + 15, imageWidth - 74, CREAM);
        button(graphics, -1, leftPos + imageWidth - 27, topPos + 10, 17, 18, "×", false, mouseX, mouseY);
        graphics.fill(leftPos + 8, topPos + HEADER_HEIGHT, leftPos + imageWidth - 8, topPos + HEADER_HEIGHT + 1, EDGE);
        if (claimsContext) sidebar.render(graphics, "claims", mouseX, mouseY);
        if (input != null) {
            renderInput(graphics, mouseX, mouseY, partialTick);
            if (claimsContext) button(graphics, BACK_ACTION, leftPos + PADDING, topPos + imageHeight - 31, 100, 23,
                    Component.translatable("jem.smp.back").getString(), false, mouseX, mouseY);
        } else renderMenu(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("jem.smp.close"), leftPos + imageWidth - 85, topPos + imageHeight - 19, MUTED, false);
        if (!hovered.isEmpty()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(hovered.getHoverName());
            for (String line : lore(hovered)) tooltip.add(Component.literal(line));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderInput(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = input.getX() + input.getWidth() / 2;
        graphics.drawCenteredString(font, "Введите значение", centerX, input.getY() - 25, MUTED);
        input.render(graphics, mouseX, mouseY, partialTick);
        button(graphics, 2, centerX - 70, input.getY() + 39, 140, 24, "Подтвердить", true, mouseX, mouseY);
    }

    private void renderMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Integer> rows = new ArrayList<>();
        List<Integer> actions = new ArrayList<>();
        List<Integer> footer = new ArrayList<>();
        for (int slot = 0; slot < Math.min(CHEST_SIZE, menu.slots.size()); slot++) {
            ItemStack item = menu.getSlot(slot).getItem();
            if (item.isEmpty()) continue;
            String role = role(item);
            if (role.equals("row_remove")) continue;
            if (role.equals("navigation")) actions.add(slot);
            else if (slot == 49 || item.is(net.minecraft.world.item.Items.ARROW) && slot >= 45) footer.add(slot);
            else {
                if (searching() && slot < 40 && !item.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(search.getValue().toLowerCase(java.util.Locale.ROOT))) continue;
                rows.add(slot);
            }
        }
        int sidebarWidth = sidebarWidth();
        int sidebarX = leftPos + PADDING;
        int sidebarY = topPos + HEADER_HEIGHT + 13;
        int sidebarBottom = topPos + imageHeight - FOOTER_HEIGHT;
        if (!claimsContext && !actions.isEmpty()) {
            graphics.fill(sidebarX, sidebarY, sidebarX + sidebarWidth, sidebarBottom, INSET);
            text(graphics, section(), sidebarX + 9, sidebarY + 11, sidebarWidth - 18, PEACH);
            for (int index = 0; index < actions.size(); index++) {
                int slot = actions.get(index);
                ItemStack item = menu.getSlot(slot).getItem();
                button(graphics, slot, sidebarX + 5, sidebarY + 30 + index * 29, sidebarWidth - 10, 26, item.getHoverName().getString(), !action(item), mouseX, mouseY);
            }
        }
        contentX = sidebarX + (claimsContext || !actions.isEmpty() ? sidebarWidth + 10 : 0);
        contentWidth = leftPos + imageWidth - PADDING - contentX;
        contentY = sidebarY;
        contentBottom = sidebarBottom;
        if (claimsContext && !actions.isEmpty()) {
            int columns = Math.min(actions.size(), Math.max(1, contentWidth / 95));
            int actionWidth = (contentWidth - (columns - 1) * ROW_GAP) / columns;
            for (int index = 0; index < actions.size(); index++) {
                int slot = actions.get(index);
                var item = menu.getSlot(slot).getItem();
                button(graphics, slot, contentX + (index % columns) * (actionWidth + ROW_GAP), contentY + (index / columns) * 29,
                        actionWidth, 26, item.getHoverName().getString(), !action(item), mouseX, mouseY);
            }
            contentY += ((actions.size() + columns - 1) / columns) * 29 + ROW_GAP;
        }
        mapLocation = null;
        if (imageWidth >= 430) {
            for (int slot : rows) {
                var item = menu.getSlot(slot).getItem();
                if (item.hasTag() && item.getTag().contains("jem_ui_dimension")) {
                    if (mapLocation == null || slot == previewSlot) mapLocation = item.getTag();
                    if (slot == previewSlot) break;
                }
            }
            if (mapLocation != null) {
                int mapWidth = Math.min(142, contentWidth / 3), mapX = contentX + contentWidth - mapWidth;
                var dimension = net.minecraft.resources.ResourceLocation.tryParse(mapLocation.getString("jem_ui_dimension"));
                if (dimension != null) {
                    var position = net.minecraft.core.BlockPos.of(mapLocation.getLong("jem_ui_position"));
                    frame(graphics, mapX, contentY, mapWidth, 82, INSET);
                    graphics.renderItem(new ItemStack(net.minecraft.world.item.Items.FILLED_MAP), mapX + 8, contentY + 9);
                    String dimensionName = dimension.getPath();
                    text(graphics, dimensionName, mapX + 30, contentY + 9, mapWidth - 38, CREAM);
                    text(graphics, position.getX() + ", " + position.getY() + ", " + position.getZ(), mapX + 8, contentY + 31, mapWidth - 16, PEACH);
                    button(graphics, MAP_ACTION, mapX + 6, contentY + 52, mapWidth - 12, 24, Component.translatable("jem.smp.map").getString(), false, mouseX, mouseY);
                    contentWidth -= mapWidth + 8;
                }
            }
        }
        int footerX = leftPos + PADDING;
        int footerWidth = Math.min(125, (imageWidth - 110) / Math.max(1, footer.size()));
        if (claimsContext && footer.contains(49)) {
            button(graphics, 49, footerX, topPos + imageHeight - 31, footerWidth, 23,
                    Component.translatable("jem.smp.back").getString(), false, mouseX, mouseY);
            footerX += footerWidth + ROW_GAP;
        }
        for (int slot : footer) {
            if (claimsContext && slot == 49) continue;
            button(graphics, slot, footerX, topPos + imageHeight - 31, footerWidth, 23, menu.getSlot(slot).getItem().getHoverName().getString(), false, mouseX, mouseY);
            footerX += footerWidth + ROW_GAP;
        }
        if (searching()) {
            search.setX(contentX);
            search.setY(contentY);
            search.setWidth(contentWidth);
            search.render(graphics, mouseX, mouseY, 0);
            contentY += 25;
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, "Пока ничего нет", contentX + contentWidth / 2, contentY + 35, MUTED);
            maxScroll = 0;
            return;
        }
        int viewport = contentBottom - contentY;
        int totalHeight = rows.stream().mapToInt(slot -> rowHeight(menu.getSlot(slot).getItem()) + ROW_GAP).sum() - ROW_GAP;
        maxScroll = Math.max(0, totalHeight - viewport);
        scroll = Math.min(scroll, maxScroll);
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentBottom);
        try {
            int y = contentY - scroll;
            for (int slot : rows) {
                int rowHeight = rowHeight(menu.getSlot(slot).getItem());
                if (y + rowHeight > contentY && y < contentBottom) renderRow(graphics, slot, y, rowHeight, mouseX, mouseY);
                y += rowHeight + ROW_GAP;
            }
        } finally {
            graphics.disableScissor();
        }
        if (maxScroll > 0) {
            int thumbHeight = Math.max(14, viewport * viewport / (maxScroll + viewport));
            int thumbY = contentY + (viewport - thumbHeight) * scroll / maxScroll;
            graphics.fill(contentX + contentWidth - 3, contentY, contentX + contentWidth, contentBottom, INSET);
            graphics.fill(contentX + contentWidth - 3, thumbY, contentX + contentWidth, thumbY + thumbHeight, COPPER);
        }
    }

    private void renderRow(GuiGraphics graphics, int slot, int y, int rowHeight, int mouseX, int mouseY) {
        ItemStack item = menu.getSlot(slot).getItem();
        int rowWidth = contentWidth - (maxScroll > 0 ? 7 : 0);
        Hit visible = new Hit(slot, contentX, Math.max(y, contentY), rowWidth, Math.min(y + rowHeight, contentBottom) - Math.max(y, contentY));
        boolean over = visible.contains(mouseX, mouseY);
        if (over && item.hasTag() && item.getTag().contains("jem_ui_dimension")) previewSlot = slot;
        graphics.fill(contentX, y, contentX + rowWidth, y + rowHeight, over && action(item) ? HOVER : ROW);
        graphics.fill(contentX, y + rowHeight - 1, contentX + rowWidth, y + rowHeight, EDGE);
        if (over && action(item)) graphics.fill(contentX, y + 2, contentX + 2, y + rowHeight - 2, COPPER);
        drawIcon(graphics, slot, item, contentX + 7, y + (rowHeight - ICON_SIZE) / 2);
        List<String> lines = lore(item);
        Boolean state = state(lines);
        int removeSlot = item.hasTag() && item.getTag().contains("jem_ui_remove_slot") ? item.getTag().getInt("jem_ui_remove_slot") : -1;
        int textWidth = rowWidth - (state == null ? removeSlot >= 0 ? 75 : 47 : 91);
        int nameY = y + (rowHeight <= COMPACT_ROW_HEIGHT ? (rowHeight - font.lineHeight) / 2 : 6);
        text(graphics, item.getHoverName().getString(), contentX + 31, nameY, textWidth, CREAM);
        if (state != null) toggle(graphics, contentX + rowWidth - 57, y + (rowHeight - 16) / 2, state);
        else if (action(item)) graphics.drawString(font, "›", contentX + rowWidth - 13, y + (rowHeight - font.lineHeight) / 2, PEACH, false);
        if (kind.equals("claim") && !action(item)) {
            int columns = contentWidth >= SUMMARY_COLUMNS_WIDTH ? 2 : 1;
            int columnWidth = textWidth / columns;
            for (int index = 0; index < lines.size(); index++)
                text(graphics, lines.get(index), contentX + 31 + index % columns * columnWidth,
                        y + 20 + index / columns * DETAIL_LINE_HEIGHT, columnWidth - BUTTON_TEXT_GAP, MUTED);
        } else if (rowHeight > COMPACT_ROW_HEIGHT && !lines.isEmpty()) {
            text(graphics, claimsContext ? String.join(" · ", lines) : lines.get(0), contentX + 31, y + 20, textWidth, MUTED);
            if (rowHeight > 48 && lines.size() > 1) {
                text(graphics, String.join(" · ", lines.subList(1, Math.min(lines.size(), 5))), contentX + 31, y + 34, textWidth, MUTED);
            }
        }
        if (removeSlot >= 0) button(graphics, removeSlot, contentX + rowWidth - 28, y + (rowHeight - 20) / 2, 22, 20, "×", false, mouseX, mouseY);
        if (action(item)) hits.add(visible);
        if (over) hovered = item;
    }

    private void toggle(GuiGraphics graphics, int x, int y, boolean enabled) {
        graphics.fill(x, y, x + 53, y + 16, OUTLINE);
        graphics.fill(x + 1, y + 1, x + 52, y + 15, enabled ? GREEN : RED);
        graphics.fill(enabled ? x + 39 : x + 3, y + 3, enabled ? x + 50 : x + 14, y + 13, CREAM);
        graphics.drawString(font, enabled ? "Да" : "Нет", enabled ? x + 7 : x + 19, y + 4, CREAM, false);
    }

    private void button(GuiGraphics graphics, int slot, int x, int y, int buttonWidth, int buttonHeight, String label, boolean selected, int mouseX, int mouseY) {
        Hit hit = new Hit(slot, x, y, buttonWidth, buttonHeight);
        boolean over = hit.contains(mouseX, mouseY);
        SmpGuiAssets.button(graphics, x, y, buttonWidth, buttonHeight, selected ? "primary" : "secondary", over, false);
        ItemStack icon = slot >= 0 && slot < menu.slots.size() ? menu.getSlot(slot).getItem() : ItemStack.EMPTY;
        boolean showIcon = (!icon.isEmpty() || slot == BACK_ACTION || slot == MAP_ACTION)
                && buttonHeight >= ICON_SIZE + BUTTON_ICON_PADDING && buttonWidth > ICON_SIZE * 2;
        int textStart = showIcon ? BUTTON_ICON_PADDING + ICON_SIZE + BUTTON_TEXT_GAP : BUTTON_TEXT_GAP;
        String fitted = fit(label, buttonWidth - textStart - BUTTON_TEXT_GAP);
        if (showIcon) drawIcon(graphics, slot, icon, x + BUTTON_ICON_PADDING, y + (buttonHeight - ICON_SIZE) / 2);
        graphics.drawString(font, fitted, showIcon ? x + textStart : x + (buttonWidth - font.width(fitted)) / 2, y + (buttonHeight - font.lineHeight) / 2, CREAM, false);
        hits.add(hit);
        if (over && slot >= 0 && slot < menu.slots.size()) hovered = menu.getSlot(slot).getItem();
    }

    private void frame(GuiGraphics graphics, int x, int y, int frameWidth, int frameHeight, int fill) {
        SmpGuiAssets.panel(graphics, x, y, frameWidth, frameHeight, fill);
    }

    private void emblem(GuiGraphics graphics, int x, int y) {
        SmpGuiAssets.icon(graphics, claimsContext ? "claims" : "crest", x, y, ICON_SIZE);
    }

    private void drawIcon(GuiGraphics graphics, int slot, ItemStack item, int x, int y) {
        String key = "";
        if (slot == BACK_ACTION || slot == 49) key = "back";
        else if (slot == MAP_ACTION) key = "map";
        else if (role(item).equals("navigation")) key = switch (slot) {
            case 45, 46 -> "claims";
            case 47 -> "profiles";
            case 48 -> "filter";
            default -> "open";
        };
        else if (claimsContext && item.is(net.minecraft.world.item.Items.GRASS_BLOCK)) key = "claims";
        else if (item.is(net.minecraft.world.item.Items.ARROW) && slot >= 45) key = slot == 53 ? "next" : "back";
        if (key.isEmpty() || !SmpGuiAssets.icon(graphics, key, x, y, ICON_SIZE)) graphics.renderItem(item, x, y);
    }

    private void text(GuiGraphics graphics, String value, int x, int y, int available, int color) {
        graphics.drawString(font, fit(value, available), x, y, color, false);
    }

    private String fit(String value, int available) {
        return font.width(value) <= available ? value : font.plainSubstrByWidth(value, Math.max(0, available - font.width("…"))) + "…";
    }

    private static List<String> lore(ItemStack item) {
        var display = item.getTagElement("display");
        if (display == null) return List.of();
        List<String> lines = new ArrayList<>();
        var tags = display.getList("Lore", Tag.TAG_STRING);
        for (int index = 0; index < tags.size(); index++) {
            try {
                Component line = Component.Serializer.fromJson(tags.getString(index));
                if (line != null) lines.add(line.getString());
            } catch (RuntimeException ignored) {
                lines.add(tags.getString(index));
            }
        }
        return lines;
    }

    private static Boolean state(List<String> lines) {
        if (lines.contains("Разрешено") || lines.contains("Включено")) return true;
        if (lines.contains("Запрещено") || lines.contains("Выключено")) return false;
        return null;
    }

    private static String role(ItemStack item) {
        return item.hasTag() ? item.getTag().getString("jem_ui_role") : "";
    }

    private static boolean action(ItemStack item) {
        return item.hasTag() && item.getTag().getBoolean("jem_ui_action");
    }

    private boolean searching() {
        return search != null && (kind.equals("members") || kind.equals("member_candidates") || kind.equals("claim_players"));
    }

    private void syncView() {
        for (int slot = 0; slot < Math.min(CHEST_SIZE, menu.slots.size()); slot++) {
            ItemStack item = menu.getSlot(slot).getItem();
            if (!item.hasTag() || !item.getTag().contains("jem_ui_kind")) continue;
            String nextKind = item.getTag().getString("jem_ui_kind");
            String nextTitle = item.getTag().getString("jem_ui_title");
            if (!kind.equals(nextKind) || !displayTitle.equals(nextTitle)) {
                previewSlot = -1;
                kind = nextKind;
                claimsContext |= claimKind(nextKind);
                displayTitle = nextTitle;
                scroll = 0;
                if (search != null) {
                    search.setValue("");
                    search.setFocused(false);
                }
                setFocused(null);
            }
            return;
        }
    }

    private int rowHeight() {
        return switch (kind) {
            case "permissions", "member", "member_defaults", "claim_settings", "claim", "members", "member_candidates", "claim_players", "claim_player", "all_claims" -> COMPACT_ROW_HEIGHT;
            case "claims_overview", "profile_claims" -> DETAIL_ROW_HEIGHT;
            case "deaths", "landmarks", "landmark", "bosses", "confirm" -> 58;
            default -> 36;
        };
    }

    private int rowHeight(ItemStack item) {
        if (kind.equals("claim") && !action(item)) {
            int columns = contentWidth >= SUMMARY_COLUMNS_WIDTH ? 2 : 1;
            return COMPACT_ROW_HEIGHT + (lore(item).size() + columns - 1) / columns * DETAIL_LINE_HEIGHT;
        }
        return claimsContext && lore(item).isEmpty() ? COMPACT_ROW_HEIGHT : rowHeight();
    }

    private String section() {
        return switch (kind) {
            case "claims_overview", "claim", "permissions", "members", "member", "member_defaults", "claim_settings", "claim_players", "claim_player", "profile_claims", "all_claims" -> "Территория";
            case "deaths" -> "История смертей";
            case "landmarks", "landmark", "landmark_category" -> "Места сервера";
            case "bosses", "boss_details" -> "Боссы";
            default -> "Управление";
        };
    }

    private void activate(int slot) {
        if (claimsContext && (slot == BACK_ACTION || slot == 49 && kind.equals("claims_overview"))) {
            ClaimsScreenBridge.back();
            return;
        }
        if (slot == MAP_ACTION && mapLocation != null) {
            var dimension = net.minecraft.resources.ResourceLocation.tryParse(mapLocation.getString("jem_ui_dimension"));
            if (dimension != null) com.siirio.jemserver.client.xaero.MapClient.open(dimension, net.minecraft.core.BlockPos.of(mapLocation.getLong("jem_ui_position")));
            return;
        }
        if (slot < 0) {
            onClose();
            return;
        }
        if (minecraft.getConnection() != null && minecraft.player != null && minecraft.player.containerMenu == menu)
            minecraft.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(
                    menu.containerId, menu.getStateId(), slot, 0, ClickType.PICKUP, menu.getCarried().copy(),
                    new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        if (claimsContext) {
            String route = sidebar.at(mouseX, mouseY);
            if (route != null) { ClaimsScreenBridge.open(route); return true; }
        }
        if (searching()) {
            boolean focused = search.mouseClicked(mouseX, mouseY, button);
            search.setFocused(focused);
            if (focused) { setFocused(search); return true; }
        }
        if (input != null && input.mouseClicked(mouseX, mouseY, button)) {
            setFocused(input);
            return true;
        }
        for (Hit hit : hits) {
            if (hit.contains(mouseX, mouseY)) {
                activate(hit.slot());
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (amount * (rowHeight() + ROW_GAP))));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return input != null && input.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (searching() && search.isFocused() && search.keyPressed(key, scanCode, modifiers)) return true;
        if (input != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) activate(2);
            else input.keyPressed(key, scanCode, modifiers);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_PAGE_DOWN) scroll = Math.min(maxScroll, scroll + rowHeight() + ROW_GAP);
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_PAGE_UP) scroll = Math.max(0, scroll - rowHeight() - ROW_GAP);
        return true;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return searching() && search.isFocused() ? search.charTyped(character, modifiers) : input != null && input.charTyped(character, modifiers);
    }

    private int sidebarWidth() {
        return claimsContext ? Math.max(86, Math.min(126, imageWidth / 4)) : imageWidth >= 430 ? 133 : 99;
    }

    private static boolean claimKind(String kind) {
        return java.util.Set.of("claims_overview", "claim", "permissions", "members", "member", "member_defaults", "member_candidates", "claim_settings", "claim_players", "claim_player", "profile_claims", "all_claims").contains(kind);
    }

    @Override
    public void onClose() {
        if (claimsContext) ClaimsScreenBridge.clear();
        super.onClose();
    }

    @Override
    public void removed() {
        SmpRenderState.restoreAfterScreen();
        super.removed();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
}
