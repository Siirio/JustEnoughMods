package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.api.MarketCatalogSnapshot;
import com.siirio.jemeconomy.api.MarketItemSnapshot;
import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.network.MarketActionPacket;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemcurrency.CurrencyItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MarketScreen extends Screen {
    private static final int PANEL = 0xF52B2019, INNER = 0xF0382A20, EDGE = 0xFFC17A3C, CREAM = 0xFFFFE7C2, MUTED = 0xFFD0B99A;
    private static final String[] CATEGORIES = {"all", "food", "resources", "blocks", "drops"};
    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private MarketCatalogSnapshot data;
    private EditBox search;
    private LivingEntity merchant;
    private boolean exchange;
    private CoinDenomination source = CoinDenomination.COPPER, target = CoinDenomination.SILVER;
    private int exchangeCount = 10;
    private int left, top, panelWidth, panelHeight;
    private boolean categoriesOpen;
    private int catalogScrollRows, maxCatalogScrollRows;

    MarketScreen(Screen parent, MarketCatalogSnapshot data) {
        super(Component.translatable("jem.economy.market.title"));
        this.parent = parent;
        this.data = data;
    }

    void accept(MarketCatalogSnapshot next) {
        if (data.page() != next.page() || !data.search().equals(next.search()) || !data.category().equals(next.category())) catalogScrollRows = 0;
        data = next;
        if (search != null && !search.isFocused() && !search.getValue().equals(next.search())) search.setValue(next.search());
    }

    @Override protected void init() {
        panelWidth = Math.min(430, width - 16); panelHeight = Math.min(280, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        search = new EditBox(font, left + 8, top + 72, Math.max(80, panelWidth - 112), 18, Component.translatable("jem.economy.market.search"));
        search.setValue(data.search()); search.setMaxLength(64); search.setHint(Component.translatable("jem.economy.market.search"));
        search.setResponder(value -> { if (!exchange) EconomyNetwork.marketQuery(value, data.category(), 0, false); });
        addRenderableWidget(search);
        if (minecraft != null && minecraft.level != null) {
            try { merchant = EntityType.VILLAGER.create(minecraft.level); if (merchant != null) merchant.setSilent(true); }
            catch (RuntimeException ignored) { merchant = null; }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); hits.clear();
        g.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        g.fill(left, top, left + panelWidth, top + 3, EDGE);
        renderHeader(g, mouseX, mouseY);
        search.visible = !exchange;
        if (exchange) renderExchange(g, mouseX, mouseY); else renderCatalog(g, mouseX, mouseY);
        if (!data.errorKey().isBlank()) g.drawCenteredString(font, Component.translatable(data.errorKey()), width / 2, top + panelHeight - 12, 0xFFFF7777);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(left + 7, top + 8, left + panelWidth - 7, top + 66, INNER);
        if (merchant != null) {
            g.flush(); g.enableScissor(left + 10, top + 10, left + 72, top + 64);
            try { InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 41, top + 82, 34, left + 41 - mouseX, top + 36 - mouseY, merchant); g.flush(); }
            catch (RuntimeException ignored) { merchant = null; }
            finally { g.disableScissor(); }
        }
        tab(g, left + 78, top + 15, 72, Component.translatable("jem.economy.market.storefront"), !exchange, () -> exchange = false);
        tab(g, left + 153, top + 15, 88, Component.translatable("jem.economy.market.exchange"), exchange, () -> exchange = true);
        int x = left + panelWidth - 174;
        long remaining = data.walletBalance();
        for (CoinDenomination denomination : CoinDenomination.descending()) {
            long count = remaining / denomination.value(); remaining %= denomination.value();
            g.renderItem(new ItemStack(CurrencyItems.item(denomination)), x, top + 39);
            g.drawString(font, "×" + count, x + 16, top + 44, CREAM, false); x += 34;
        }
    }

    private void renderCatalog(GuiGraphics g, int mouseX, int mouseY) {
        int categoryX = left + panelWidth - 98;
        categoryButton(g, categoryX, top + 72, 90, data.category(), () -> categoriesOpen = !categoriesOpen);
        int contentTop = top + 96, bottom = top + panelHeight - 22;
        int columns = Math.max(2, Math.min(4, panelWidth / 102));
        int gap = 4, cardWidth = (panelWidth - 16 - gap * (columns - 1)) / columns, cardHeight = 70;
        // The server searches packaged native/overlay names and variant titles in both supported locales.
        List<MarketItemSnapshot> filtered = data.items();
        int visibleRows = Math.max(1, (bottom - contentTop + gap) / (cardHeight + gap));
        maxCatalogScrollRows = Math.max(0, (filtered.size() + columns - 1) / columns - visibleRows);
        catalogScrollRows = Math.max(0, Math.min(catalogScrollRows, maxCatalogScrollRows));
        for (int index = 0; index < filtered.size(); index++) {
            int row = index / columns - catalogScrollRows, column = index % columns, y = contentTop + row * (cardHeight + gap);
            if (row < 0) continue;
            if (y + cardHeight > bottom) break;
            int x = left + 8 + column * (cardWidth + gap); MarketItemSnapshot item = filtered.get(index);
            g.fill(x, y, x + cardWidth, y + cardHeight, INNER); g.fill(x, y, x + cardWidth, y + 1, 0xFF805438);
            g.renderItem(item.stack(), x + 5, y + 5);
            g.drawString(font, font.plainSubstrByWidth(marketName(item.stack()).getString(), cardWidth - 28), x + 25, y + 6, CREAM, false);
            g.drawString(font, "↓", x + 4, y + 24, MUTED, false);
            if (item.sellCopper() > 0) CoinPriceRenderer.draw(g, font, item.sellCopper(), x + 14, y + 20, cardWidth - 19, MUTED);
            else g.drawString(font, font.plainSubstrByWidth(Component.translatable("jem.economy.market.no_buyback").getString(), cardWidth - 19), x + 14, y + 24, MUTED, false);
            g.drawString(font, "↑", x + 4, y + 39, CREAM, false);
            CoinPriceRenderer.draw(g, font, item.buyCopper(), x + 14, y + 35, cardWidth - 19, CREAM);
            if (mouseX >= x && mouseX < x + cardWidth && mouseY >= y + 20 && mouseY < y + 51)
                g.renderTooltip(font, Component.translatable(mouseY < y + 35 ? "jem.economy.market.sell" : "jem.economy.market.buy"), mouseX, mouseY);
            int half = (cardWidth - 13) / 2;
            if (item.buyAvailable()) button(g, x + 5, y + 53, half, 14, Component.translatable("jem.economy.market.buy"),
                    () -> EconomyNetwork.marketAction(MarketActionPacket.Action.BUY, item.id(), 1, data.revision(), data.search(), data.category(), data.page()));
            if (item.sellAvailable()) button(g, x + 8 + half, y + 53, half, 14, Component.translatable("jem.economy.market.sell"),
                    () -> EconomyNetwork.marketAction(MarketActionPacket.Action.SELL, item.id(), 1, data.revision(), data.search(), data.category(), data.page()));
        }
        button(g, left + 8, top + panelHeight - 19, 44, 14, Component.literal("<"), () -> queryPage(data.page() - 1));
        g.drawCenteredString(font, (data.page() + 1) + "/" + data.pages(), width / 2, top + panelHeight - 16, MUTED);
        button(g, left + panelWidth - 52, top + panelHeight - 19, 44, 14, Component.literal(">"), () -> queryPage(data.page() + 1));
        if (categoriesOpen) renderCategories(g, categoryX, top + 92);
    }

    private static Component marketName(ItemStack stack) {
        var entityTag = stack.getTagElement("EntityTag");
        if (stack.is(Items.PAINTING) && entityTag != null && entityTag.contains("variant")) {
            var variant = net.minecraft.resources.ResourceLocation.tryParse(entityTag.getString("variant"));
            if (variant != null) {
                String key = "painting." + variant.getNamespace() + "." + variant.getPath() + ".title";
                return net.minecraft.client.resources.language.I18n.exists(key) ? Component.translatable(key) : Component.literal(variant.toString());
            }
        }
        return stack.getHoverName();
    }

    private void renderCategories(GuiGraphics g, int x, int y) {
        g.pose().pushPose(); g.pose().translate(0, 0, 300);
        for (int i = 0; i < CATEGORIES.length; i++) {
            String category = CATEGORIES[i]; int cy = y + i * 20;
            g.fill(x, cy, x + 90, cy + 20, category.equals(data.category()) ? 0xFF70482F : 0xFF3B2A21);
            g.fill(x, cy, x + 90, cy + 1, EDGE);
            g.renderItem(categoryIcon(category), x + 3, cy + 2);
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("jem.economy.market.category." + category).getString(), 65), x + 22, cy + 6, CREAM, false);
            hits.add(new Hit(x, cy, 90, 20, () -> { categoriesOpen = false; EconomyNetwork.marketQuery(search.getValue(), category, 0, false); }));
        }
        g.pose().popPose();
    }

    private void categoryButton(GuiGraphics g, int x, int y, int w, String category, Runnable action) {
        g.fill(x, y, x + w, y + 18, categoriesOpen ? 0xFF70482F : 0xFF4A3327); g.fill(x, y, x + w, y + 1, EDGE);
        g.renderItem(categoryIcon(category), x + 3, y + 1);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable("jem.economy.market.category." + category).getString(), w - 34), x + 22, y + 5, CREAM, false);
        g.drawString(font, categoriesOpen ? "▲" : "▼", x + w - 11, y + 5, EDGE, false);
        hits.add(new Hit(x, y, w, 18, action));
    }

    private static ItemStack categoryIcon(String category) {
        return new ItemStack(switch (category) { case "food" -> Items.BREAD; case "resources" -> Items.IRON_INGOT; case "blocks" -> Items.BRICKS; case "drops" -> Items.ROTTEN_FLESH; default -> Items.CHEST; });
    }

    private void renderExchange(GuiGraphics g, int mouseX, int mouseY) {
        search.visible = false;
        int cx = width / 2, y = top + 92;
        g.drawCenteredString(font, Component.translatable("jem.economy.exchange.physical_only"), cx, y, MUTED);
        button(g, cx - 142, y + 22, 92, 20, denominationName(source), () -> source = next(source));
        button(g, cx + 50, y + 22, 92, 20, denominationName(target), () -> target = next(target));
        g.drawCenteredString(font, Component.literal("→"), cx, y + 28, EDGE);
        button(g, cx - 46, y + 50, 28, 18, Component.literal("−"), () -> exchangeCount = Math.max(1, exchangeCount - 1));
        g.drawCenteredString(font, Integer.toString(exchangeCount), cx, y + 55, CREAM);
        button(g, cx + 18, y + 50, 28, 18, Component.literal("+"), () -> exchangeCount = Math.min(4096, exchangeCount + 1));
        long value = (long) exchangeCount * source.value(); boolean exact = source != target && value % target.value() == 0L;
        long receive = exact ? value / target.value() : 0L;
        g.renderItem(new ItemStack(CurrencyItems.item(source)), cx - 86, y + 82);
        g.drawString(font, "×" + exchangeCount, cx - 66, y + 87, CREAM, false);
        g.renderItem(new ItemStack(CurrencyItems.item(target)), cx + 35, y + 82);
        g.drawString(font, "×" + receive, cx + 55, y + 87, exact ? CREAM : 0xFFFF7777, false);
        g.drawCenteredString(font, exact ? Component.translatable("jem.economy.exchange.exact") : Component.translatable("jem.economy.exchange.no_fraction"), cx, y + 108, exact ? MUTED : 0xFFFF7777);
        if (exact) button(g, cx - 58, y + 126, 116, 20, Component.translatable("jem.economy.exchange.confirm"),
                () -> EconomyNetwork.exchange(source, exchangeCount, target, data.search(), data.category(), data.page()));
        int[] held = data.physicalCounts(); int hx = cx - 84;
        for (CoinDenomination denomination : CoinDenomination.values()) {
            g.renderItem(new ItemStack(CurrencyItems.item(denomination)), hx, top + panelHeight - 42);
            g.drawString(font, "×" + held[denomination.ordinal()], hx + 16, top + panelHeight - 37, MUTED, false); hx += 36;
        }
    }

    private void tab(GuiGraphics g, int x, int y, int w, Component label, boolean active, Runnable action) {
        g.fill(x, y, x + w, y + 18, active ? 0xFF70482F : 0xFF493226); if (active) g.fill(x, y + 16, x + w, y + 18, EDGE);
        g.drawCenteredString(font, label, x + w / 2, y + 5, active ? CREAM : MUTED); hits.add(new Hit(x, y, w, 18, action));
    }
    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, Runnable action) {
        g.fill(x, y, x + w, y + h, 0xFF4A3327); g.fill(x, y, x + w, y + 1, 0xFF8B5A38);
        g.drawCenteredString(font, font.plainSubstrByWidth(label.getString(), w - 4), x + w / 2, y + Math.max(2, (h - 8) / 2), CREAM); hits.add(new Hit(x, y, w, h, action));
    }
    private void queryPage(int page) { if (page >= 0 && page < data.pages()) EconomyNetwork.marketQuery(search.getValue(), data.category(), page, false); }
    private static CoinDenomination next(CoinDenomination value) { CoinDenomination[] values = CoinDenomination.values(); return values[(value.ordinal() + 1) % values.length]; }
    private static Component denominationName(CoinDenomination value) { return Component.translatable("item.jem_economy." + value.id()); }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { var current = List.copyOf(hits); for (int i = current.size() - 1; i >= 0; i--) { Hit hit = current.get(i); if (hit.contains(mouseX, mouseY)) { hit.action.run(); return true; } } }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!exchange && mouseY >= top + 96 && mouseY < top + panelHeight - 22) {
            catalogScrollRows = Math.max(0, Math.min(maxCatalogScrollRows, catalogScrollRows - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    private record Hit(int x, int y, int width, int height, Runnable action) { boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; } }
}
