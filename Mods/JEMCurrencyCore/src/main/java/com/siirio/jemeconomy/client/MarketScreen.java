package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.api.MarketCatalogSnapshot;
import com.siirio.jemeconomy.api.MarketItemSnapshot;
import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.network.MarketActionPacket;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemcurrency.CurrencyItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Reference-aligned storefront. The server remains the sole catalog and transaction authority. */
public final class MarketScreen extends Screen {
    private static final ResourceLocation ART = new ResourceLocation("jem_economy", "textures/gui/market/storefront.png");
    private static final int ART_WIDTH = 1671, ART_HEIGHT = 941, CANVAS_WIDTH = 836, CANVAS_HEIGHT = 471;
    private static final int CREAM = 0xFFFFE7C2, MUTED = 0xFFD0B99A, EDGE = 0xFFC17A3C;
    private static final String[] CATEGORIES = {"blocks", "food", "plants", "resources", "drops"};
    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private MarketCatalogSnapshot data;
    private EditBox search;
    private String activeCategory;
    private Villager merchant;
    private boolean exchange, categoriesOpen;
    private CoinDenomination source = CoinDenomination.COPPER, target = CoinDenomination.SILVER;
    private int exchangeCount = 10;
    private float uiScale, originX, originY;
    private ItemStack hoveredItem = ItemStack.EMPTY;
    private Component hoveredText;
    private long hoveredPrice = -1;

    MarketScreen(Screen parent, MarketCatalogSnapshot data) {
        super(Component.translatable("jem.economy.market.title"));
        this.parent = parent;
        this.data = data;
        this.activeCategory = java.util.Arrays.asList(CATEGORIES).contains(data.category())?data.category():"blocks";
    }

    void accept(MarketCatalogSnapshot next) {
        if(next.errorKey().isBlank()&&!next.category().equals(activeCategory))return;
        data = next;
        if (search != null && !search.isFocused() && !search.getValue().equals(next.search())) search.setValue(next.search());
    }

    @Override protected void init() {
        String draft = search == null ? data.search() : search.getValue();
        uiScale = Math.min((width - 8f) / CANVAS_WIDTH, (height - 8f) / CANVAS_HEIGHT);
        originX = (width - CANVAS_WIDTH * uiScale) / 2f;
        originY = (height - CANVAS_HEIGHT * uiScale) / 2f;
        search = new EditBox(font, 61, 382, 129, 16, Component.translatable("jem.economy.market.search"));
        search.setBordered(true);
        search.setTextColor(CREAM);
        search.setValue(draft);
        search.setMaxLength(64);
        search.setHint(Component.translatable("jem.economy.market.search"));
        search.setResponder(value -> { if (!exchange) EconomyNetwork.marketQuery(value, activeCategory, 0, false); });
        addWidget(search);
        if (minecraft != null && minecraft.level != null) {
            merchant = EntityType.VILLAGER.create(minecraft.level);
            if (merchant != null) {
                merchant.setVillagerData(merchant.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(5));
                merchant.setSilent(true);
                merchant.setNoAi(true);
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        hits.clear(); hoveredItem = ItemStack.EMPTY; hoveredText = null; hoveredPrice = -1;
        int mx = (int) localX(mouseX), my = (int) localY(mouseY);
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        g.pose().scale(uiScale, uiScale, 1);
        g.blit(ART, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, 0, 0, ART_WIDTH, ART_HEIGHT, ART_WIDTH, ART_HEIGHT);
        renderMerchant(g, mx, my);
        renderHeader(g, mx, my);
        search.visible = !exchange; search.active = !exchange;
        if (exchange) renderExchange(g, mx, my);
        else {
            renderCatalog(g, mx, my);
            renderCategories(g, mx, my);
            search.render(g, mx, my, partialTick);
        }
        if (!data.errorKey().isBlank()) {
            g.fill(245, 387, 786, 398, 0xE0201510);
            g.drawCenteredString(font, Component.translatable(data.errorKey()), 516, 389, 0xFFFF9999);
        }
        g.flush();
        g.pose().popPose();
        if (!hoveredItem.isEmpty()) g.renderTooltip(font, hoveredItem, mouseX, mouseY);
        else if (hoveredPrice >= 0) CoinPriceRenderer.tooltip(g, font, hoveredPrice, mouseX, mouseY, width, height);
        else if (hoveredText != null) g.renderTooltip(font, hoveredText, mouseX, mouseY);
    }

    private void renderMerchant(GuiGraphics g, int mx, int my) {
        if (merchant != null) {
            g.flush();
            g.enableScissor(screenX(65), screenY(69), screenX(220), screenY(255));
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, 142, 310, 120,
                        (142 - mx) * .35f, (159 - my) * .35f, merchant);
                g.flush();
            } finally { g.disableScissor(); }
        }
        g.drawString(font, tr("merchant"), 58, 270, 0xFF302015, false);
        if(exchange)wrapped(g, tr("merchant_quote"), 58, 288, 151, 0xFF664329);
        if(exchange)wrapped(g, Component.translatable("jem.economy.exchange.physical_only"), 58, 328, 150, 0xFF664329);
        button(g, 58, 406, 151, 18, Component.translatable("gui.back"), this::onClose);
    }

    private void renderHeader(GuiGraphics g, int mx, int my) {
        g.pose().pushPose();
        g.pose().translate(271, 43, 0); g.pose().scale(1.85f, 1.85f, 1);
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 114), 0, 0, CREAM, true);
        g.pose().popPose();
        long remaining = data.walletBalance();
        int x = 505;
        for (CoinDenomination denomination : CoinDenomination.descending()) {
            long count = remaining / denomination.value(); remaining %= denomination.value();
            g.renderItem(new ItemStack(CurrencyItems.item(denomination)), x, 47);
            fitted(g, Component.literal("×" + count), x + 18, 52, 34, CREAM);
            if (inside(mx, my, x, 45, 53, 25)) hoveredText = denominationName(denomination);
            x += 53;
        }
        // Exchange remains reachable through the integrated sign, without another category toolbar.
        Component alternate = Component.translatable(exchange ? "jem.economy.market.storefront" : "jem.economy.market.exchange");
        fitted(g, alternate, 369, 69, 114, exchange ? CREAM : MUTED);
        hits.add(new Hit(269, 35, 217, 45, () -> {
            exchange = !exchange; categoriesOpen = false; search.setFocused(false); setFocused(null);
        }));
    }

    private void renderCatalog(GuiGraphics g, int mx, int my) {
        g.fill(244,95,787,124,0xff35251c);
        g.renderItem(categoryIcon(activeCategory),252,101);
        g.drawString(font,categoryName(activeCategory),273,105,CREAM,false);
        if(!data.category().equals(activeCategory)||!data.errorKey().isBlank()) {
            g.drawCenteredString(font,Component.translatable("jem.economy.market.loading_category"),516,244,MUTED);
            return;
        }
        for (int index = 0; index < 12; index++) {
            int x = 245 + index % 4 * 137, y = 134 + index / 4 * 86;
            if (index >= data.items().size()) {
                g.fill(x + 4, y + 53, x + 127, y + 76, 0xAC201A14);
                continue;
            }
            MarketItemSnapshot item = data.items().get(index);
            ItemStack stack = item.stack();
            g.pose().pushPose(); g.pose().translate(x + 8, y + 12, 0); g.pose().scale(1.75f, 1.75f, 1);
            g.renderItem(stack, 0, 0); g.pose().popPose();
            g.renderItemDecorations(font, stack, x + 17, y + 23);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(marketName(stack), 79);
            for (int line = 0; line < Math.min(2, lines.size()); line++) g.drawString(font, lines.get(line), x + 49, y + 9 + line * 10, CREAM, false);
            CoinPriceRenderer.draw(g, font, item.buyCopper(), x + 49, y + 31, 77, CREAM);
            if (inside(mx, my, x + 4, y + 4, 123, 48) && !categoriesOpen) hoveredItem = stack;
            artButton(g, x + 4, y + 53, 59, 21, tr("buy"), item.buyAvailable(), mx, my,
                    () -> transact(MarketActionPacket.Action.BUY, item));
            artButton(g, x + 67, y + 53, 59, 21, tr("sell"), item.sellAvailable(), mx, my,
                    () -> transact(MarketActionPacket.Action.SELL, item));
            if (!categoriesOpen && inside(mx, my, x + 67, y + 53, 59, 21)) {
                if (item.sellCopper() > 0) hoveredPrice = item.sellCopper();
                else hoveredText = tr("no_buyback");
            }
        }
        artButton(g, 424, 403, 44, 21, Component.literal("‹"), data.page() > 0, mx, my, () -> queryPage(data.page() - 1));
        g.drawCenteredString(font, (data.page() + 1) + " / " + data.pages(), 506, 411, CREAM);
        artButton(g, 544, 403, 43, 21, Component.literal("›"), data.page() + 1 < data.pages(), mx, my, () -> queryPage(data.page() + 1));
    }

    private void renderCategories(GuiGraphics g,int mx,int my) {
        for(int i=0;i<CATEGORIES.length;i++) {
            String category=CATEGORIES[i];int y=284+i*18;
            g.fill(58,y,210,y+18,category.equals(activeCategory)?0xff70482f:0xff35251c);
            g.renderItem(categoryIcon(category),61,y+1);
            fitted(g,categoryName(category),81,y+5,125,CREAM);
            hits.add(new Hit(58,y,152,18,()->{activeCategory=category;EconomyNetwork.marketQuery(search.getValue(),category,0,false);}));
        }
        g.fill(58,378,210,402,0xff35251c);
        hits.add(new Hit(192,381,16,18,()->{search.setValue("");search.setFocused(true);setFocused(search);}));
        g.drawCenteredString(font,Component.literal("x"),200,386,CREAM);
    }

    private void transact(MarketActionPacket.Action action, MarketItemSnapshot item) {
        if(!data.category().equals(activeCategory)||!data.errorKey().isBlank())return;
        EconomyNetwork.marketAction(action, item.id(), 1, data.revision(), data.search(), activeCategory, data.page());
    }

    private void artButton(GuiGraphics g, int x, int y, int w, int h, Component label, boolean enabled, int mx, int my, Runnable action) {
        if (!enabled) g.fill(x, y, x + w, y + h, 0x99201812);
        else if (!categoriesOpen && inside(mx, my, x, y, w, h)) g.fill(x, y, x + w, y + h, 0x30FFEDBD);
        g.drawCenteredString(font, label, x + w / 2, y + 6, enabled ? CREAM : 0xFF9C9080);
        if (enabled) hits.add(new Hit(x, y, w, h, action));
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, Runnable action) {
        g.fill(x, y, x + w, y + h, 0xFF4A3327); g.fill(x, y, x + w, y + 1, EDGE);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, CREAM);
        hits.add(new Hit(x, y, w, h, action));
    }

    private void fitted(GuiGraphics g, Component label, int x, int y, int w, int color) {
        float scale = Math.min(1f, w / (float) Math.max(1, font.width(label)));
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, label, 0, 0, color, false); g.pose().popPose();
    }

    private void wrapped(GuiGraphics g, Component label, int x, int y, int w, int color) {
        for (var line : font.split(label, w)) { g.drawString(font, line, x, y, color, false); y += 11; }
    }

    private static Component tr(String key) { return Component.translatable("jem.economy.market." + key); }
    private static Component categoryName(String category) { return tr("category." + category); }
    private static ItemStack categoryIcon(String category) {
        return new ItemStack(switch (category) {
            case "food" -> Items.BREAD; case "plants" -> Items.OAK_SAPLING;
            case "resources" -> Items.IRON_INGOT; case "blocks" -> Items.BRICKS;
            case "drops" -> Items.ROTTEN_FLESH; default -> Items.CHEST;
        });
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

    private void renderExchange(GuiGraphics g, int mouseX, int mouseY) {
        search.visible = false;
        int cx = 516, y = 166;
        g.fill(244, 132, 788, 387, 0xF02C2019);
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
                () -> EconomyNetwork.exchange(source, exchangeCount, target, data.search(), activeCategory, data.page()));
        int[] held = data.physicalCounts(); int hx = cx - 84;
        for (CoinDenomination denomination : CoinDenomination.values()) {
            g.renderItem(new ItemStack(CurrencyItems.item(denomination)), hx, 350);
            g.drawString(font, "×" + held[denomination.ordinal()], hx + 16, 355, MUTED, false); hx += 36;
        }
    }


    private void queryPage(int page) {
        if (page >= 0 && page < data.pages()) EconomyNetwork.marketQuery(search.getValue(), activeCategory, page, false);
    }
    private static CoinDenomination next(CoinDenomination value) {
        CoinDenomination[] values = CoinDenomination.values(); return values[(value.ordinal() + 1) % values.length];
    }
    private static Component denominationName(CoinDenomination value) { return Component.translatable("item.jem_economy." + value.id()); }
    private double localX(double x) { return (x - originX) / uiScale; }
    private double localY(double y) { return (y - originY) / uiScale; }
    private int screenX(int x) { return Math.round(originX + x * uiScale); }
    private int screenY(int y) { return Math.round(originY + y * uiScale); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = localX(mouseX), my = localY(mouseY);
        if (categoriesOpen) {
            if (button == 0 && inside(mx, my, 444, 125, 335, CATEGORIES.length * 23)) {
                int index = (int) (my - 125) / 23;
                categoriesOpen = false;
                EconomyNetwork.marketQuery(search.getValue(), CATEGORIES[index], 0, false);
            } else categoriesOpen = false;
            return true;
        }
        if (button == 0) {
            var current = List.copyOf(hits);
            for (int i = current.size() - 1; i >= 0; i--) {
                Hit hit = current.get(i);
                if (hit.contains(mx, my)) { hit.action.run(); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(localX(x), localY(y), button, dx / uiScale, dy / uiScale);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(localX(x), localY(y), button); }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (categoriesOpen) return true;
        if (!exchange && delta != 0 && inside(localX(x), localY(y), 244, 133, 545, 253)) {
            queryPage(data.page() - (int) Math.signum(delta)); return true;
        }
        return false;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if(key==256&&search.isFocused()){if(!search.getValue().isEmpty())search.setValue("");else{search.setFocused(false);setFocused(null);}return true;}
        if (key == 256 && categoriesOpen) { categoriesOpen = false; return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    private record Hit(int x, int y, int width, int height, Runnable action) {
        boolean contains(double mx, double my) { return inside(mx, my, x, y, width, height); }
    }
}
