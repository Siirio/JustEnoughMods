package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.SmpQuery;
import com.siirio.jemserver.smp.SmpAction;
import com.siirio.jemserver.smp.SmpUpdate;
import static com.siirio.jemserver.client.ui.JemPalette.*;

import com.google.gson.JsonObject;
import com.siirio.jemserver.smp.SmpNetwork;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

import java.util.*;

public final class SmpScreen extends Screen {
    private static final int ROW_HEIGHT = 42, GAP = 4, FIELD_HEIGHT = 36;
    private CompoundTag model = new CompoundTag();
    private final List<SmpHit> hits = new ArrayList<>();
    private final LinkedHashMap<String, EditBox> fields = new LinkedHashMap<>();
    private String tab = "events", filter = "", form = "", error = "";
    private EditBox search;
    private EditBox seller;
    private MultiLineEditBox messageBox;
    private String shopSort = "near";
    private int formScroll;
    private int memberScroll;
    private int memberMaxScroll;
    private int memberListX;
    private int memberListY;
    private int memberListWidth;
    private int memberListHeight;
    private int partyRewardScroll;
    private int partyRewardMaxScroll;
    private int partyRewardX;
    private int partyRewardY;
    private int partyRewardWidth;
    private String expandedChoice = "";
    private final Map<String, List<String>> choices = new HashMap<>();
    private final BackNavigation navigation = new BackNavigation();
    private final SmpSidebar sidebar = new SmpSidebar();
    private final EntityPreview bossPreview = new EntityPreview();
    private UUID previewEvent;
    private boolean expandedRewards;
    private boolean leaveAccepted;
    private boolean closeLobby;
    private boolean dissolvedLobby;
    private final PlayerPreviewWidget playerPreview = new PlayerPreviewWidget();
    private final Map<UUID, Integer> prizeOffsets = new HashMap<>();
    private final Set<UUID> expandedPrizeGroups = new HashSet<>();
    private final LinkedHashSet<String> sellerSuggestions = new LinkedHashSet<>();
    private UUID hoveredPrize;
    private UUID selected;
    private UUID participant;
    private int page,
            scroll,
            maxScroll,
            left,
            top,
            panelWidth,
            panelHeight,
            bodyX,
            bodyY,
            bodyWidth,
            bodyBottom;
    private long searchAt, lastSent, viewRevision, lastResync;
    private UUID viewSession;
    private SmpAction pending;
    private boolean waiting;
    private boolean clippingBody;
    private ItemStack hoveredItem = ItemStack.EMPTY;
    private Component hoveredLabel;

    public SmpScreen(SmpUpdate update) {
        super(Component.translatable("jem.smp.title"));
        accept(update);
    }

    public void accept(SmpUpdate update) {
        if (update.view() != null && !update.view().isEmpty()) {
            boolean delta = update.view().getBoolean("__delta");
            if (delta && (!Objects.equals(viewSession, update.session()) || viewRevision != update.baseRevision())) {
                long now = System.currentTimeMillis();
                if (now - lastResync >= com.siirio.jemserver.smp.SmpProtocol.RESYNC_MILLIS) {
                    lastResync = now;
                    SmpNetwork.query(new SmpQuery(tab, model.getString("filter"), model.getString("querySearch"), page, selected, true));
                }
                return;
            }
            if (update.session() != null) {
                if (Objects.equals(viewSession, update.session()) && update.revision() <= viewRevision) return;
                viewSession = update.session();
                viewRevision = update.revision();
            }
            var incoming =
                    update.view().getBoolean("__delta")
                            ? com.siirio.jemserver.smp.ViewDelta.apply(model, update.view())
                            : update.view().copy();
            String next = incoming.getString("tab");
            UUID nextId = incoming.contains("detail") && incoming.getCompound("detail").hasUUID("id") ? incoming.getCompound("detail").getUUID("id") : null;
            if (update.open() && (!next.equals(tab) || !Objects.equals(nextId, selected))) remember();
            if (update.open() && !Objects.equals(nextId, selected)) {
                memberScroll = 0;
                partyRewardScroll = 0;
                participant = null;
            }
            if (update.open() && !next.equals(tab)) {
                tab = next;
                filter = "";
                page = 0;
                scroll = 0;
                clearForm();
                form = "";
            }
            if (update.open() || next.equals(tab)) model = incoming;
            if (next.equals("shops")) {
                for (Tag value : incoming.getList("rows", Tag.TAG_COMPOUND)) {
                    String name = ((CompoundTag) value).getString("name");
                    if (!name.isBlank()) sellerSuggestions.add(name);
                }
            }
            if (!update.open()
                    && next.equals(tab)
                    && tab.equals("shops")
                    && selected != null
                    && (!incoming.contains("detail")
                            || !incoming.getCompound("detail").hasUUID("id")
                            || !selected.equals(incoming.getCompound("detail").getUUID("id"))))
                selected = null;
            if (update.open()) {
                tab = next;
                String rawFilter = model.getString("filter");
                filter = tab.equals("events") || tab.equals("shops") ? "" : rawFilter;
                if (search != null) {
                    String value = model.getString("querySearch");
                    if (tab.equals("shops")) {
                        String[] pair = value.split("\u001f", -1);
                        seller.setValue(pair[0]);
                        search.setValue(pair.length > 1 ? pair[1] : "");
                    } else search.setValue(value);
                    searchAt = 0;
                }
                selected =
                        model.contains("detail") && model.getCompound("detail").hasUUID("id") ? model.getCompound("detail").getUUID("id") : null;
                if (tab.equals("shops")) selected = null;
            }
            if (tab.equals("parties") && selected != null && currentDetail() && model.getCompound("detail").getBoolean("closeLobby")) {
                closeLobby = model.getCompound("detail").getString("state").equals("ACTIVE");
                dissolvedLobby = !closeLobby;
            }
        }
        if (update.request() != null
                && pending != null
                && update.request().equals(pending.request())) {
            String action = pending.kind().key();
            waiting = false;
            pending = null;
            error = update.error();
            if (error.isEmpty()) {
                if (action.equals("message") && messageBox != null) messageBox.setValue("");
                else if (!action.equals("invite")) clearForm();
                if (action.equals("leave")) leaveAccepted = true;
            }
        }
    }

    private void clearForm() {
        if (!fields.isEmpty()) {
            for (var field : fields.values()) removeWidget(field);
            fields.clear();
            choices.clear();
            setFocused(null);
        }
        if (messageBox != null) {
            removeWidget(messageBox);
            messageBox = null;
        }
        expandedChoice = "";
        form = "";
    }

    @Override
    protected void init() {
        com.siirio.jemserver.client.ui.SmpRenderState.restoreAfterScreen();
        if (pending != null && pending.kind().key().equals("claims")) { pending = null; waiting = false; }
        String initial = model.getString("querySearch");
        String[] pair = initial.split("\u001f", -1);
        String value = search == null ? (tab.equals("shops") ? (pair.length > 1 ? pair[1] : "") : initial) : search.getValue();
        String sellerValue = seller == null ? (tab.equals("shops") ? pair[0] : "") : seller.getValue();
        left = 10;
        top = 10;
        panelWidth = width - 20;
        panelHeight = height - 20;
        int sidebarWidth = Math.max(86, Math.min(126, panelWidth / 4));
        sidebar.layout(left + 8, top + 40, sidebarWidth - 8, panelHeight - 88);
        bodyX = left + sidebarWidth + 10;
        bodyY = top + 66;
        bodyWidth = Math.max(80, left + panelWidth - bodyX - 10);
        bodyBottom = top + panelHeight - 42;
        search =
                new EditBox(
                        font,
                        bodyX,
                        top + 37,
                        Math.max(60, bodyWidth - 142),
                        20,
                        Component.translatable("jem.smp.search"));
        search.setMaxLength(128);
        search.setValue(value);
        search.setHint(Component.translatable("jem.smp.search"));
        search.setTextColor(CREAM);
        search.setResponder(
                text -> {
                    searchAt = System.currentTimeMillis() + 300;
                });
        addWidget(search);
        seller =
                new EditBox(
                        font,
                        bodyX,
                        top + 37,
                        Math.max(60, bodyWidth / 2 - 8),
                        20,
                        Component.translatable("jem.smp.seller"));
        seller.setMaxLength(16);
        seller.setValue(sellerValue);
        seller.setHint(Component.translatable("jem.smp.seller"));
        seller.setResponder(text -> searchAt = System.currentTimeMillis() + 300);
        addWidget(seller);
        int index = 0;
        for (var field : fields.values()) {
            positionField(field, index++);
            addWidget(field);
        }
        if (messageBox != null) {
            positionMessageBox();
            addWidget(messageBox);
        }
        query();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (closeLobby) { closeLobby = false; onClose(); return; }
        if (leaveAccepted || dissolvedLobby) {
            boolean dissolved = dissolvedLobby;
            leaveAccepted = false; dissolvedLobby = false;
            clearForm(); back();
            if (dissolved) error = "party_dissolved";
        }
        search.tick();
        seller.tick();
        fields.values().forEach(EditBox::tick);
        if (messageBox != null) messageBox.tick();
        if (searchAt != 0 && System.currentTimeMillis() >= searchAt) {
            searchAt = 0;
            page = 0;
            scroll = 0;
            query();
        }
    }

    private void query() {
        SmpNetwork.query(
                new SmpQuery(
                        tab,
                        tab.equals("shops")
                                ? "search|near"
                                : tab.equals("events") ? "" : filter,
                        tab.equals("shops")
                                ? seller.getValue() + "\u001f" + search.getValue()
                                : search == null ? "" : search.getValue(),
                        page,
                        selected));
    }

    private void remember() {
        if (search != null) navigation.remember(new SmpRoute(tab, filter, selected, page, scroll, search.getValue(), seller.getValue(), shopSort, model.copy()));
    }

    public static void openRoute(String route, UUID detail) {
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.screen instanceof SmpScreen screen) { screen.visit(route, detail); return; }
        var view = new CompoundTag();
        view.putString("tab", route);
        if (detail != null) { var row = new CompoundTag(); row.putUUID("id", detail); view.put("detail", row); }
        client.setScreen(new SmpScreen(new SmpUpdate(view, true, null, "")));
    }

    private void visit(String next, UUID detail) {
        if (next.equals(tab) && Objects.equals(detail, selected)) return;
        remember();
        expandedRewards = false;
        partyRewardScroll = 0;
        tab = next;
        filter = "";
        page = 0;
        scroll = 0;
        selected = next.equals("shops") ? null : detail;
        clearForm();
        error = "";
        search.setValue("");
        seller.setValue("");
        searchAt = 0;
        query();
    }

    private void back() {
        if (!form.isEmpty()) { clearForm(); return; }
        var previous = navigation.back();
        if (previous == null) {
            if (!tab.equals("events") || selected != null) {
                tab = "events"; selected = null; filter = ""; page = 0; scroll = 0; query();
            }
            return;
        }
        tab = previous.tab();
        filter = tab.equals("events") ? "" : previous.filter();
        selected = previous.selected();
        if (tab.equals("shops")) selected = null;
        page = previous.page();
        scroll = previous.scroll();
        shopSort = previous.sort();
        model = previous.model();
        search.setValue(previous.search());
        seller.setValue(previous.seller());
        searchAt = 0;
        error = "";
        query();
    }

    private void selectTab(String next) {
        if (next.equals("claims")) { ClaimsScreenBridge.capture(this); send("claims", new JsonObject()); return; }
        if (!next.equals(tab) || selected != null) visit(next, null);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        com.siirio.jemserver.client.ui.SmpRenderState.restoreMainTarget();
        renderBackground(g);
        hits.clear();
        hoveredItem = ItemStack.EMPTY;
        hoveredLabel = null;
        frame(g, left, top, panelWidth, panelHeight, PANEL);
        g.fill(left + 3, top + 3, left + panelWidth - 3, top + 31, INSET);
        SmpIcons.draw(g, "crest", left + 10, top + 7, 20);
        text(g, title, left + 36, top + 12, CREAM, bodyX - left - 40);
        text(g, Component.translatable("jem.smp." + tab), bodyX, top + 12, CREAM, bodyWidth - 30);
        button(g, left + panelWidth - 28, top + 7, 20, 20, "close", this::onClose, mx, my);
        sidebar.render(g, tab, mx, my);
        if (navigation.available() || selected != null || !tab.equals("events") || !form.isEmpty())
            button(g, left + 8, top + panelHeight - 32, bodyX - left - 18, 22, "back", this::back, mx, my);
        bodyY = top + 66;
        boolean toolbar = selected == null && !tab.equals("prizes") && form.isEmpty();
        search.setVisible(toolbar); search.active = toolbar;
        seller.setVisible(toolbar && tab.equals("shops")); seller.active = toolbar && tab.equals("shops");
        if (toolbar) {
            boolean showFilter = tab.equals("parties");
            int reserve = tab.equals("parties") ? 138 : 0;
            search.setX(bodyX); search.setY(top + 37); search.setWidth(Math.max(30, bodyWidth - reserve));
            search.setHint(Component.translatable("jem.smp.search"));
            if (tab.equals("shops")) {
                int half = Math.max(30, (bodyWidth - 8) / 2);
                seller.setX(bodyX); seller.setY(top + 37); seller.setWidth(half);
                search.setX(bodyX + half + 8); search.setWidth(half);
                seller.setHint(Component.translatable("jem.smp.seller"));
                search.setHint(Component.translatable("jem.smp.item_search"));
                seller.render(g, mx, my, partial);
            } else {
                if (showFilter) button(g, bodyX + bodyWidth - 66, top + 37, 66, 20, "filter", this::cyclePartyFilter, mx, my);
                if (tab.equals("parties")) button(g, bodyX + bodyWidth - 134, top + 37, 64, 20, "create", () -> openForm("create"), mx, my);
            }
            search.render(g, mx, my, partial);
        } else bodyY = top + 42;
        if (form.isEmpty()) {
            clippingBody = true;
            g.enableScissor(bodyX, bodyY, bodyX + bodyWidth, bodyBottom);
            try {
                if (tab.equals("prizes")) renderPrizes(g, mx, my);
                else if (selected != null) {
                    if (currentDetail()) renderDetail(g, mx, my);
                    else text(g, Component.translatable("jem.smp.loading"), bodyX + 6, bodyY + 10, MUTED, bodyWidth - 12);
                } else if (tab.equals("events")) renderEvents(g, mx, my);
                else renderRows(g, mx, my);
            } finally {
                g.disableScissor();
                clippingBody = false;
            }
            if (selected == null) {
                int size = Math.max(1, model.contains("pageSize") ? model.getInt("pageSize") : 20);
                if (model.getInt("total") > size) {
                    button(g, bodyX, bodyBottom + 9, 32, 22, "previous", () -> { if (page > 0) { page--; scroll = 0; query(); } }, mx, my);
                    button(g, bodyX + 36, bodyBottom + 9, 32, 22, "next", () -> { if ((page + 1) * size < model.getInt("total")) { page++; scroll = 0; query(); } }, mx, my);
                }
            }
            if (!tab.equals("prizes") && !tab.equals("profiles") && !(tab.equals("parties") && selected != null) && model.getInt("pendingRewards") > 0)
                button(g, bodyX + Math.max(0, bodyWidth - 138), bodyBottom + 9, Math.min(138, bodyWidth), 22, "claim_rewards", () -> visit("prizes", null), mx, my);
        } else renderForm(g, mx, my, partial);
        String message = error.isEmpty() ? model.getString("message") : error;
        if (!message.isEmpty()) text(g, Component.translatable("jem.smp.error." + message), bodyX, top + panelHeight - 12, RED, bodyWidth);
        if (waiting) text(g, Component.translatable("jem.smp.pending"), bodyX + bodyWidth / 2, top + 12, MUTED, bodyWidth / 2 - 30);
        renderSellerSuggestions(g, mx, my);
        if (!hoveredItem.isEmpty() && my >= bodyY && my < bodyBottom) g.renderTooltip(font, hoveredItem, mx, my);
        else if (hoveredLabel != null) g.renderTooltip(font, hoveredLabel, mx, my);
    }

    private void renderSellerSuggestions(GuiGraphics g, int mx, int my) {
        if (!form.isEmpty() || selected != null || !tab.equals("shops") || !seller.isVisible() || !seller.isFocused()) return;
        String query = seller.getValue().trim().toLowerCase(Locale.ROOT);
        var matches = sellerSuggestions.stream()
                .filter(name -> query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query))
                .filter(name -> !name.equalsIgnoreCase(seller.getValue()))
                .limit(6)
                .toList();
        if (matches.isEmpty()) return;
        int x = seller.getX(), y = seller.getY() + seller.getHeight(), width = seller.getWidth();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        for (int index = 0; index < matches.size(); index++) {
            String name = matches.get(index);
            int rowY = y + index * 20;
            boolean hovered = mx >= x && mx < x + width && my >= rowY && my < rowY + 20;
            frame(g, x, rowY, width, 20, hovered ? HOVER : INSET);
            text(g, Component.literal(name), x + 7, rowY + 6, CREAM, width - 14);
            hits.add(new SmpHit(x, rowY, width, 20, () -> {
                seller.setValue(name);
                setFocused(search);
            }));
        }
        g.pose().popPose();
    }

    private static int stateColor(String state) {
        return switch (state) {
            case "ACTIVE", "OPEN", "PUBLISHED", "COMPLETED" -> GREEN;
            case "FAILED", "CANCELLED", "CLOSED" -> RED;
            default -> PEACH;
        };
    }

    private void disabledButton(GuiGraphics g, int x, int y, int w, String key, String reason, int mx, int my) {
        disabledButton(g, x, y, w, 24, key, reason, mx, my);
    }

    private void disabledButton(GuiGraphics g, int x, int y, int w, int h, String key, String reason, int mx, int my) {
        SmpGuiAssets.button(g, x, y, w, h, "secondary", false, true);
        text(g, Component.translatable("jem.smp." + key), x + 6, y + (h - 9) / 2, MUTED, w - 12);
        if (!reason.isEmpty() && mx >= x && mx < x + w && my >= y && my < y + h)
            hoveredLabel = Component.translatable("jem.smp.error." + reason);
    }

    private void renderPrizes(GuiGraphics g, int mx, int my) {
        var bundles = model.getList("rows", Tag.TAG_COMPOUND);
        int y = bodyY - scroll;
        hoveredPrize = null;
        int claimWidth = Math.min(160, bodyWidth / 2);
        if (bundles.isEmpty()) disabledButton(g, bodyX, y, claimWidth, "claim_all", "", mx, my);
        else button(g, bodyX, y, claimWidth, 24, "claim_all", () -> claimReward(null), mx, my);
        text(g, Component.translatable("jem.smp.available_items", model.getInt("totalItems")), bodyX + claimWidth + 10, y + 8, PEACH, bodyWidth - claimWidth - 10);
        y += 34;
        if (bundles.isEmpty()) {
            SmpIcons.draw(g, "prizes", bodyX + 12, y + 7, 28);
            text(g, Component.translatable("jem.smp.no_pending_rewards"), bodyX + 48, y + 16, MUTED, bodyWidth - 58);
            maxScroll = 0; scroll = 0; return;
        }
        for (var value : bundles) {
            var bundle = (CompoundTag) value;
            if (!bundle.hasUUID("id")) continue;
            var items = bundle.getList("items", Tag.TAG_COMPOUND);
            var groups = bundle.getList("groups", Tag.TAG_COMPOUND);
            UUID id = bundle.getUUID("id");
            boolean expanded = !groups.isEmpty() && expandedPrizeGroups.contains(id);
            int h = 68 + (expanded ? groups.size() * 48 : 0);
            if (y + h < bodyY || y >= bodyBottom) { y += h + GAP; continue; }
            frame(g, bodyX, y, bodyWidth, h, ROW);
            SmpIcons.draw(g, rewardIcon(bundle), bodyX + 8, y + 8, 22);
            int groupButtonWidth = groups.isEmpty() ? 0 : Math.min(108, bodyWidth / 5);
            text(g, Component.translatable(bundle.getString("titleKey")), bodyX + 38, y + 8, CREAM, bodyWidth - 48 - groupButtonWidth);
            Component date = bundle.getLong("createdAt") > 0 ? Component.literal(formatTime(bundle.getLong("createdAt"))) : Component.translatable("jem.smp.unknown_date");
            text(g, date, bodyX + 38, y + 22, MUTED, bodyWidth - 48);
            if (!groups.isEmpty()) {
                int groupX = bodyX + bodyWidth - groupButtonWidth - 7;
                SmpGuiAssets.button(g, groupX, y + 5, groupButtonWidth, 24, "secondary", mx >= groupX && mx < groupX + groupButtonWidth && my >= y + 5 && my < y + 29, false);
                text(g, Component.translatable(expanded ? "jem.smp.reward.collapse" : "jem.smp.reward.expand", groups.size()), groupX + 7, y + 13, CREAM, groupButtonWidth - 14);
                hitWithin(groupX, y + 5, groupButtonWidth, 24, bodyX, bodyY, bodyWidth, bodyBottom - bodyY, () -> {
                    if (!expandedPrizeGroups.remove(id)) expandedPrizeGroups.add(id);
                });
            }
            int buttonWidth = Math.min(104, Math.max(72, bodyWidth / 5));
            int stripX = bodyX + 8, stripY = y + 37, stripWidth = Math.max(27, bodyWidth - buttonWidth - 22);
            int maxOffset = Math.max(0, ItemRewardStrip.horizontalWidth(items) - stripWidth);
            int offset = Math.min(maxOffset, prizeOffsets.getOrDefault(id, 0));
            prizeOffsets.put(id, offset);
            ItemRewardStrip.renderHorizontal(g, items, stripX, stripY, stripWidth, offset, mx, my, item -> hoveredItem = item);
            restoreBodyScissor(g);
            if (mx >= stripX && mx < stripX + stripWidth && my >= stripY && my < stripY + ItemRewardStrip.CELL) hoveredPrize = id;
            button(g, bodyX + bodyWidth - buttonWidth - 7, stripY + 1, buttonWidth, 24, "claim", () -> claimReward(id), mx, my);
            if (expanded) {
                int groupY = y + 66;
                for (Tag groupValue : groups) {
                    var group = (CompoundTag) groupValue;
                    frame(g, bodyX + 8, groupY, bodyWidth - 16, 44, INSET);
                    text(g, Component.translatable(group.getString("titleKey")), bodyX + 15, groupY + 5, PEACH, bodyWidth - 30);
                    var groupItems = group.getList("items", Tag.TAG_COMPOUND);
                    int groupStripWidth = bodyWidth - 30;
                    int groupOffset = Math.min(Math.max(0, ItemRewardStrip.horizontalWidth(groupItems) - groupStripWidth), prizeOffsets.getOrDefault(id, 0));
                    ItemRewardStrip.renderHorizontal(g, groupItems, bodyX + 15, groupY + 16, groupStripWidth, groupOffset, mx, my, item -> hoveredItem = item);
                    restoreBodyScissor(g);
                    if (mx >= bodyX + 15 && mx < bodyX + 15 + groupStripWidth && my >= groupY + 16 && my < groupY + 16 + ItemRewardStrip.CELL) hoveredPrize = id;
                    groupY += 48;
                }
            }
            y += h + GAP;
        }
        maxScroll = Math.max(0, y + scroll - bodyBottom);
        scroll = Math.min(scroll, maxScroll);
    }

    private void claimReward(UUID id) {
        sendFor(id, 0, id == null ? "claim_all" : "claim", new JsonObject());
    }

    private String rewardIcon(CompoundTag bundle) {
        String key = (bundle.getString("titleKey") + " " + bundle.getString("sourceId")).toUpperCase(Locale.ROOT);
        if (key.contains("BLOOD")) return "BLOOD_MOON";
        if (key.contains("FISH")) return "FISHING";
        if (key.contains("COOK")) return "COOKING_SHOW";
        if (key.contains("RUSH")) return "RESOURCE_RUSH";
        if (key.contains("BOSS") || key.contains("RAID")) return "BOSS_RAID";
        return "prizes";
    }

    private void renderInvites(GuiGraphics g, int mx, int my) {
        var candidates = model.getCompound("detail").getList("inviteCandidates", Tag.TAG_COMPOUND);
        text(g, Component.translatable("jem.smp.invite_players"), bodyX, bodyY, CREAM, bodyWidth);
        boolean parentClipping = clippingBody;
        clippingBody = true;
        g.enableScissor(bodyX, bodyY + 22, bodyX + bodyWidth, bodyBottom);
        try {
            int y = bodyY + 26 - formScroll;
            if (candidates.isEmpty()) text(g, Component.translatable("jem.smp.no_invite_candidates"), bodyX + 6, y + 8, MUTED, bodyWidth - 12);
            for (var value : candidates) {
                var candidate = (CompoundTag) value;
                UUID id = candidate.getUUID("id");
                frame(g, bodyX, y, bodyWidth, ROW_HEIGHT - GAP, ROW);
                CombatHud.head(g, id, bodyX + 7, y + 8, 22);
                int bw = Math.min(92, bodyWidth / 3);
                text(g, Component.literal(candidate.getString("name")), bodyX + 36, y + 14, CREAM, bodyWidth - bw - 44);
                if (candidate.getBoolean("pending")) disabledButton(g, bodyX + bodyWidth - bw - 5, y + 7, bw, "pending_invite", "", mx, my);
                else button(g, bodyX + bodyWidth - bw - 5, y + 7, bw, 24, "invite", () -> {
                    var args = new JsonObject(); args.addProperty("player", id.toString()); send("invite", args);
                }, mx, my);
                y += ROW_HEIGHT;
            }
        } finally {
            g.disableScissor();
            clippingBody = parentClipping;
            restoreBodyScissor(g);
        }
    }

    private void renderPartyLobby(GuiGraphics g, int mx, int my) {
        var detail = model.getCompound("detail");
        var actions = detail.getList("actions", Tag.TAG_STRING);
        var members = detail.getCompound("members");
        scroll = 0;
        maxScroll = 0;
        text(g, Component.literal(detail.getString("title")), bodyX + 4, bodyY + 6, CREAM, bodyWidth - 8);
        var controls = new ArrayList<String>();
        for (String action : List.of("join", "arrive", "ready", "leave", "dissolve", "start"))
            if (hasAction(actions, action)) controls.add(action);
        int buttonColumns = Math.max(1, controls.size());
        int buttonsY = bodyBottom + 9;
        int contentTop = bodyY + 25;
        if (detail.getBoolean("leader") && !detail.getBoolean("canStart") && !detail.getString("startReason").isEmpty()) {
            var reason = font.split(Component.translatable("jem.smp.error." + detail.getString("startReason")), bodyWidth - 12);
            for (var line : reason) {
                g.drawString(font, line, bodyX + 5, contentTop, MUTED, false);
                contentTop += 11;
            }
            contentTop += 4;
        }
        int contentBottom = bodyBottom;
        var loot = detail.getList("reward", Tag.TAG_COMPOUND);
        int rewardHeight = loot.isEmpty() ? 0 : 75;
        int upperBottom = contentBottom - rewardHeight - (rewardHeight > 0 ? 7 : 0);
        int leftWidth = Math.max(104, Math.min(172, bodyWidth / 3));
        int memberX = bodyX + leftWidth + 8;
        int memberWidth = bodyWidth - leftWidth - 8;
        int previewHeight = Math.max(72, upperBottom - contentTop);
        String activity = eventActivity(detail);
        g.fill(bodyX, contentTop, bodyX + leftWidth, contentTop + previewHeight, INSET);
        if (detail.getString("bossType").isEmpty()) SmpGuiAssets.cover(g, activity, bodyX + 3, contentTop + 3, leftWidth - 6, previewHeight - 6);
        else SmpGuiAssets.coverBackground(g, activity, bodyX + 3, contentTop + 3, leftWidth - 6, previewHeight - 6);
        SmpGuiAssets.frame(g, bodyX, contentTop, leftWidth, previewHeight, SmpGuiAssets.accent(activity));
        if (!detail.getString("bossType").isEmpty()) {
            bossPreview.render(g, detail.getString("bossType"), bodyX + 4, contentTop + 4, leftWidth - 8, previewHeight - 8, mx, my);
            restoreBodyScissor(g);
        }
        else if (!Set.of("BOSS", "BOSS_RAID", "BLOOD_MOON", "RESOURCE_RUSH", "FISHING", "COOKING_SHOW").contains(activity)) SmpIcons.draw(g, activity, bodyX + leftWidth / 2 - 24, contentTop + previewHeight / 2 - 24, 48);
        restoreBodyScissor(g);
        text(g, Component.translatable("jem.smp.participants"), memberX + 4, contentTop + 4, PEACH, memberWidth - 8);
        memberListX = memberX;
        memberListY = contentTop + 22;
        memberListWidth = memberWidth;
        int locationHeight = detail.getBoolean("meaningfulLocation") ? 42 : 0;
        CompoundTag selectedMember=participant!=null&&members.contains(participant.toString())?members.getCompound(participant.toString()):null;
        boolean manage=detail.getBoolean("leader")&&selectedMember!=null
                &&(selectedMember.getBoolean("canApprove")||selectedMember.getBoolean("canRemove")||selectedMember.getBoolean("canTransfer"));
        int managementHeight=manage?31:0;
        memberListHeight = Math.max(36, upperBottom - memberListY - (locationHeight > 0 ? locationHeight + 6 : 0) - (managementHeight > 0 ? managementHeight + 6 : 0));
        int memberY = memberListY - memberScroll;
        var memberIds = new ArrayList<>(members.getAllKeys());
        memberIds.sort(Comparator.comparing((String id) -> !detail.hasUUID("owner") || !detail.getUUID("owner").toString().equals(id)).thenComparing(id -> members.getCompound(id).getString("name")));
        boolean parentClipping = clippingBody;
        clippingBody = true;
        g.enableScissor(memberListX, memberListY, memberListX + memberListWidth, memberListY + memberListHeight);
        try {
            for (String raw : memberIds) {
                var member = members.getCompound(raw);
                UUID id;
                try { id = UUID.fromString(raw); } catch (IllegalArgumentException invalid) { continue; }
                if (memberY + 40 >= memberListY && memberY < memberListY + memberListHeight) {
                    frame(g, memberX, memberY, memberWidth, 40, id.equals(participant)?HOVER:ROW);
                    CombatHud.head(g, id, memberX + 7, memberY + 9, 22);
                    String name = member.getString("name");
                    text(g, Component.literal(name.isBlank() ? raw : name), memberX + 36, memberY + 7, CREAM, memberWidth - 42);
                    String state = !member.getBoolean("online") ? "offline" : !member.getBoolean("accepted") ? "awaiting_approval" : member.getBoolean("ready") ? "ready" : "not_ready";
                    Component status = Component.translatable("jem.smp." + state);
                    if (detail.hasUUID("owner") && detail.getUUID("owner").equals(id)) status = Component.translatable("jem.smp.leader").append(" · ").append(status);
                    text(g, status, memberX + 36, memberY + 23, member.getBoolean("ready") ? GREEN : MUTED, memberWidth - 42);
                    hitWithin(memberX, memberY, memberWidth, 40, memberListX, memberListY, memberListWidth, memberListHeight, () -> participant=id);
                }
                memberY += 44;
            }
            if (hasAction(actions, "invite")) {
                frame(g, memberX, memberY, memberWidth, 40, mx >= memberX && mx < memberX + memberWidth && my >= memberY && my < memberY + 40 ? HOVER : ROW);
                text(g, Component.literal("+"), memberX + 12, memberY + 11, PEACH, 18);
                text(g, Component.translatable("jem.smp.add_player"), memberX + 36, memberY + 15, CREAM, memberWidth - 42);
                hitWithin(memberX, memberY, memberWidth, 40, memberListX, memberListY, memberListWidth, memberListHeight, () -> openForm("invite"));
                memberY += 44;
            }
        } finally {
            g.disableScissor();
            clippingBody = parentClipping;
            restoreBodyScissor(g);
        }
        memberMaxScroll = Math.max(0, memberY + memberScroll - memberListY - memberListHeight);
        memberScroll = Math.max(0, Math.min(memberScroll, memberMaxScroll));
        if(manage) {
            var management=new ArrayList<String>();
            if(selectedMember.getBoolean("canApprove")) management.add("approve");
            if(selectedMember.getBoolean("canRemove")) management.add("remove");
            if(selectedMember.getBoolean("canTransfer")) management.add("transfer");
            int managementY=memberListY+memberListHeight+5;
            int managementWidth=(memberWidth-(management.size()-1)*GAP)/management.size();
            for(int index=0;index<management.size();index++) {
                String action=management.get(index);
                button(g,memberX+index*(managementWidth+GAP),managementY,managementWidth,24,action,()->invoke(action),mx,my);
            }
        }
        if (locationHeight > 0) renderCompactLocation(g, detail, memberX, upperBottom - locationHeight, memberWidth, locationHeight, mx, my);
        if (rewardHeight > 0) {
            int rewardY = contentBottom - rewardHeight;
            frame(g, bodyX, rewardY, bodyWidth, rewardHeight, ROW);
            text(g, Component.translatable("jem.smp.possible_rewards"), bodyX + 8, rewardY + 7, PEACH, bodyWidth - 16);
            partyRewardX = bodyX + 8;
            partyRewardY = rewardY + 20;
            partyRewardWidth = bodyWidth - 16;
            partyRewardMaxScroll = Math.max(0, ItemRewardStrip.twoRowWidth(loot) - partyRewardWidth);
            partyRewardScroll = Math.max(0, Math.min(partyRewardScroll, partyRewardMaxScroll));
            ItemRewardStrip.renderTwoRows(g, loot, partyRewardX, partyRewardY, partyRewardWidth, partyRewardScroll, mx, my, item -> hoveredItem = item);
            restoreBodyScissor(g);
        } else {
            partyRewardScroll = 0;
            partyRewardMaxScroll = 0;
            partyRewardWidth = 0;
        }
        int bw = (bodyWidth - (buttonColumns - 1) * GAP) / buttonColumns;
        g.disableScissor();
        clippingBody = false;
        try {
            for (int i = 0; i < controls.size(); i++) {
                String action = controls.get(i);
                int bx = bodyX + i * (bw + GAP);
                String label = action.equals("dissolve") ? "leave" : action;
                if (action.equals("ready") && model.hasUUID("viewer") && members.getCompound(model.getUUID("viewer").toString()).getBoolean("ready")) label = "unready";
                if (action.equals("start") && !detail.getBoolean("canStart")) disabledButton(g, bx, buttonsY, bw, "start", detail.getString("startReason"), mx, my);
                else button(g, bx, buttonsY, bw, 24, label, () -> invoke(action), mx, my);
            }
        } finally {
            clippingBody = true;
            g.enableScissor(bodyX, bodyY, bodyX + bodyWidth, bodyBottom);
        }
    }

    private boolean hasAction(ListTag actions, String action) {
        for (Tag value : actions) if (((StringTag) value).getAsString().equals(action)) return true;
        return false;
    }

    private void renderCompactLocation(GuiGraphics g, CompoundTag detail, int x, int y, int width, int height, int mx, int my) {
        var dimension = ResourceLocation.tryParse(detail.getString("dimension"));
        if (dimension == null || height < 40) return;
        var pos = net.minecraft.core.BlockPos.of(detail.getLong("position"));
        frame(g, x, y, width, height, INSET);
        SmpIcons.draw(g, "map", x + 7, y + 11, 18);
        text(g, Component.literal(dimensionName(dimension.toString())), x + 31, y + 7, CREAM, width - 112);
        text(g, Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), x + 31, y + 23, PEACH, width - 112);
        button(g, x + width - 77, y + 8, 70, 25, "map", () -> openLocation(detail), mx, my);
    }

    private void renderProfile(GuiGraphics g, CompoundTag row, int mx, int my) {
        UUID id = row.getUUID("id");
        int y = bodyY - scroll, portraitWidth = Math.max(90, Math.min(140, bodyWidth / 3));
        int portraitHeight = 192;
        frame(g, bodyX, y, portraitWidth, portraitHeight, INSET);
        playerPreview.render(g, id, row.getString("name"), bodyX + 4, y + 4, portraitWidth - 8, portraitHeight - 29, mx, my);
        restoreBodyScissor(g);
        text(g, Component.translatable(row.getBoolean("online") ? "jem.smp.online" : "jem.smp.offline"), bodyX + 9, y + portraitHeight - 20, row.getBoolean("online") ? GREEN : MUTED, portraitWidth - 18);
        int sx = bodyX + portraitWidth + 10, sw = bodyWidth - portraitWidth - 10;
        text(g, Component.literal(row.getString("name")), sx, y + 8, CREAM, sw);
        var stats = new ArrayList<CompoundTag>();
        var playTime = new CompoundTag();
        playTime.putString("type", "playTicks");
        playTime.putLong("count", row.getLong("playTicks"));
        stats.add(playTime);
        var mobs = new CompoundTag();
        mobs.putString("type", "mobsKilled");
        mobs.putLong("count", row.getLong("mobsKilled"));
        stats.add(mobs);
        for (Tag value : row.getList("eventStats", Tag.TAG_COMPOUND)) stats.add((CompoundTag) value);
        int columns = sw >= 240 ? 2 : 1, cellWidth = sw / columns;
        for (int i = 0; i < stats.size(); i++) {
            var statistic = stats.get(i);
            String key = statistic.getString("type");
            int cx = sx + (i % columns) * cellWidth, cy = y + 30 + (i / columns) * 47;
            frame(g, cx, cy, cellWidth - GAP, 43, ROW);
            SmpIcons.draw(g, key, cx + 7, cy + 12, 20);
            text(g, Component.translatable("jem.smp." + key), cx + 33, cy + 8, MUTED, cellWidth - 41);
            String value = key.equals("playTicks") ? java.time.Duration.ofSeconds(statistic.getLong("count") / 20).toHours() + " h" : Long.toString(statistic.getLong("count"));
            text(g, Component.literal(value), cx + 33, cy + 24, CREAM, cellWidth - 41);
        }
        int statsBottom = y + 30 + ((stats.size() + columns - 1) / columns) * 47;
        var actions = row.getList("actions", Tag.TAG_STRING);
        int actionColumns = Math.max(1, Math.min(3, sw / 100));
        int actionWidth = (sw - (actionColumns - 1) * GAP) / actionColumns;
        for (int index = 0; index < actions.size(); index++) {
            String action = actions.getString(index);
            int ax = sx + index % actionColumns * (actionWidth + GAP);
            int ay = statsBottom + 6 + index / actionColumns * 29;
            button(g, ax, ay, actionWidth, 24, action, () -> invoke(action), mx, my);
        }
        int contentHeight = Math.max(portraitHeight, statsBottom - y + 6 + ((actions.size() + actionColumns - 1) / actionColumns) * 29);
        maxScroll = Math.max(0, contentHeight + 8 - (bodyBottom - bodyY));
        scroll = Math.min(scroll, maxScroll);
    }

    private void eventFrame(GuiGraphics g, String activity, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        SmpGuiAssets.frame(g, x, y, w, h, SmpGuiAssets.accent(activity));
    }

    private String eventActivity(CompoundTag event) {
        String activity = event.getString("eventActivity");
        return activity.isEmpty() ? event.getString("activity") : activity;
    }

    private void renderEvents(GuiGraphics g, int mx, int my) {
        var rows = model.getList("rows", Tag.TAG_COMPOUND);
        if (rows.isEmpty()) {
            SmpIcons.draw(g, "events", bodyX + 12, bodyY + 13, 28);
            text(g, Component.translatable("jem.smp.no_events"), bodyX + 48, bodyY + 23, MUTED, bodyWidth - 58);
            maxScroll = 0;
            return;
        }
        boolean preview = bodyWidth >= 580;
        int previewWidth = preview ? Math.min(240, bodyWidth / 3) : 0;
        int listWidth = bodyWidth - (preview ? previewWidth + 10 : 0);
        int rowHeight = 66;
        maxScroll = Math.max(0, rows.size() * (rowHeight + GAP) - GAP - (bodyBottom - bodyY));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        CompoundTag chosen = rows.getCompound(0);
        for (int index = 0; index < rows.size(); index++) {
            var event = rows.getCompound(index);
            int y = bodyY + index * (rowHeight + GAP) - scroll;
            boolean over = mx >= bodyX && mx < bodyX + listWidth && my >= Math.max(bodyY, y) && my < Math.min(bodyBottom, y + rowHeight);
            if (over) previewEvent = event.getUUID("id");
            if (event.getUUID("id").equals(previewEvent)) chosen = event;
            if (y + rowHeight < bodyY || y >= bodyBottom) continue;
            String activity = eventActivity(event);
            eventFrame(g, activity, bodyX, y, listWidth, rowHeight, over ? HOVER : ROW);
            int coverWidth = Math.min(76, listWidth / 3), coverHeight = rowHeight - 10;
            SmpGuiAssets.cover(g, activity, bodyX + 5, y + 5, coverWidth, coverHeight);
            int tx = bodyX + coverWidth + 13, tw = listWidth - coverWidth - 34;
            text(g, eventTitle(event), tx, y + 9, CREAM, tw);
            String lifecycle = event.contains("lifecycle") ? event.getString("lifecycle") : event.getString("state");
            text(g, Component.translatable("jem.smp." + lifecycle), tx, y + 23, SmpGuiAssets.accent(activity), tw);
            text(g, eventClock(event), tx, y + 36, MUTED, tw);
            text(g, eventDescription(event), tx, y + 50, MUTED, tw);
            SmpIcons.draw(g, "next", bodyX + listWidth - 19, y + 25, 13);
            if (over) hoveredLabel = eventTitle(event).copy().append("\n").append(eventDescription(event));
            hit(bodyX, y, listWidth, rowHeight, () -> visit("events", event.getUUID("id")));
        }
        if (!preview) return;
        int x = bodyX + listWidth + 10, y = bodyY;
        String activity = eventActivity(chosen);
        eventFrame(g, activity, x, y, previewWidth, bodyBottom - y, INSET);
        int coverHeight = Math.min(112, (previewWidth - 12) * 3 / 4);
        SmpGuiAssets.cover(g, activity, x + 6, y + 6, previewWidth - 12, coverHeight);
        int next = y + coverHeight + 17;
        text(g, eventTitle(chosen), x + 10, next, CREAM, previewWidth - 20);
        next += 18;
        text(g, eventClock(chosen), x + 10, next, SmpGuiAssets.accent(activity), previewWidth - 20);
        next += 23;
        int actionY = bodyBottom - (chosen.getBoolean("meaningfulLocation") ? 60 : 32);
        for (var line : font.split(eventDescription(chosen), previewWidth - 20)) {
            if (next + font.lineHeight > actionY - 12) break;
            g.drawString(font, line, x + 10, next, MUTED, false);
            next += font.lineHeight + 4;
        }
        final CompoundTag selectedPreview = chosen;
        button(g, x + 8, actionY, previewWidth - 16, 23, "event_details", () -> visit("events", selectedPreview.getUUID("id")), mx, my);
        if (chosen.getBoolean("meaningfulLocation"))
            button(g, x + 8, actionY + 28, previewWidth - 16, 23, "map", () -> openLocation(selectedPreview), mx, my);
    }

    private void renderEventDetail(GuiGraphics g, CompoundTag event, int mx, int my) {
        int y = bodyY - scroll;
        String activity = eventActivity(event);
        eventFrame(g, activity, bodyX, y, bodyWidth, 45, INSET);
        SmpIcons.draw(g, activity, bodyX + 8, y + 8, 28);
        text(g, eventTitle(event), bodyX + 44, y + 9, CREAM, bodyWidth - 54);
        String lifecycle = event.contains("lifecycle") ? event.getString("lifecycle") : event.getString("state");
        text(g, Component.translatable("jem.smp." + lifecycle).append(" · ").append(eventClock(event)), bodyX + 44, y + 27, SmpGuiAssets.accent(activity), bodyWidth - 54);
        y += 54;
        y += renderEventLoot(g, event, bodyX, y, bodyWidth, mx, my);
        frame(g, bodyX, y, bodyWidth, 34, ROW);
        SmpIcons.draw(g, "calendar", bodyX + 8, y + 8, 16);
        long start = event.getLong("planned"), end = event.getLong("ends");
        Component schedule = start > 0 && end > 0
                ? Component.literal(formatTime(start) + " — " + formatTime(end))
                : eventClock(event);
        text(g, schedule, bodyX + 31, y + 7, CREAM, bodyWidth - 39);
        text(g, eventClock(event), bodyX + 31, y + 20, SmpGuiAssets.accent(activity), bodyWidth - 39);
        y += 44;
        var description = font.split(eventDescription(event), Math.max(30, bodyWidth - 44));
        int descriptionHeight = 31 + description.size() * (font.lineHeight + 4);
        frame(g, bodyX, y, bodyWidth, descriptionHeight, INSET);
        SmpIcons.draw(g, activity, bodyX + 8, y + 8, 20);
        text(g, Component.translatable("jem.smp.what_happens"), bodyX + 35, y + 9, PEACH, bodyWidth - 43);
        int descriptionY = y + 27;
        for (var line : description) {
            g.drawString(font, line, bodyX + 35, descriptionY, CREAM, false);
            descriptionY += font.lineHeight + 4;
        }
        y += descriptionHeight + 10;
        y += renderEventActions(g, event, y, mx, my);
        boolean columns = bodyWidth >= 390;
        int visualWidth = columns ? Math.max(145, bodyWidth * 2 / 5) : bodyWidth;
        int visualHeight = columns ? Math.min(158, visualWidth * 3 / 4) : Math.min(128, visualWidth * 3 / 4);
        int visualY = y;
        if (event.getString("bossType").isBlank()) SmpGuiAssets.cover(g, activity, bodyX + 3, y + 3, visualWidth - 6, visualHeight - 6);
        else {
            SmpGuiAssets.coverBackground(g, activity, bodyX + 3, y + 3, visualWidth - 6, visualHeight - 6);
            bossPreview.render(g, event.getString("bossType"), bodyX + 4, y + 4, visualWidth - 8, visualHeight - 8, mx, my);
            restoreBodyScissor(g);
        }
        SmpGuiAssets.frame(g, bodyX, y, visualWidth, visualHeight, SmpGuiAssets.accent(activity));
        int infoX = columns ? bodyX + visualWidth + 12 : bodyX + 6;
        int infoWidth = columns ? bodyWidth - visualWidth - 12 : bodyWidth - 12;
        int infoY = columns ? y + 5 : y + visualHeight + 14;
        infoY += EventRulesPanel.render(g, event.getList("rules", Tag.TAG_COMPOUND), infoX, infoY, infoWidth);
        y = Math.max(visualY + visualHeight, infoY) + 14;
        y += renderCookingProgress(g, event, bodyX, y, bodyWidth, mx, my);
        maxScroll = Math.max(0, y + scroll - bodyBottom + 8);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private int renderEventActions(GuiGraphics g, CompoundTag event, int y, int mx, int my) {
        var available = new ArrayList<String>();
        if (eventActionAvailable(event, "create_party")) available.add("create_party");
        if (eventActionAvailable(event, "solo")) available.add("solo");
        if (eventActionAvailable(event, "teleport_event")) available.add("teleport_event");
        if (event.getBoolean("meaningfulLocation")) available.add("map");
        if (available.isEmpty()) return 0;
        int columns = Math.max(1, Math.min(available.size(), bodyWidth / 110));
        int actionWidth = (bodyWidth - (columns - 1) * 6) / columns;
        for (int index = 0; index < available.size(); index++) {
            String action = available.get(index);
            button(g, bodyX + index % columns * (actionWidth + 6), y + index / columns * 28, actionWidth, 23, action, () -> {
                if (action.equals("map")) openLocation(event);
                else if (action.equals("solo")) {
                    minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(confirmed -> {
                        minecraft.setScreen(this);
                        if (confirmed) send("solo", new JsonObject());
                    }, Component.translatable("jem.smp.solo_confirm_title"), Component.translatable("jem.smp.solo_confirm_message")));
                } else send(action, new JsonObject());
            }, mx, my);
        }
        return ((available.size() + columns - 1) / columns) * 28 + 8;
    }

    private Component eventTitle(CompoundTag event) {
        String title = event.getString("title");
        String activity = event.getString("eventActivity");
        if (activity.isBlank()) activity = event.getString("activity");
        if (title.isBlank() || title.equals(activity)) return Component.translatable("jem.smp." + activity);
        return Component.literal(title);
    }

    private Component eventDescription(CompoundTag event) {
        if (!event.getString("descriptionKey").isEmpty()) {
            var args = event.getList("descriptionArgs", Tag.TAG_STRING);
            Object[] values = new Object[args.size()];
            for (int i = 0; i < args.size(); i++) values[i] = args.getString(i);
            return Component.translatable(event.getString("descriptionKey"), values);
        }
        if (!event.getString("description").isBlank()) return Component.literal(event.getString("description"));
        String activity = event.getString("eventActivity");
        if (activity.isBlank()) activity = event.getString("activity");
        return Component.translatable("jem.smp.description." + activity);
    }

    private Component eventClock(CompoundTag event) {
        long now = System.currentTimeMillis();
        boolean active = event.getString("state").equals("ACTIVE");
        long target = event.getLong(active ? "ends" : "planned");
        if (target <= 0) return Component.translatable("jem.smp." + event.getString("state"));
        if (!active && !Set.of("PLANNED", "SCHEDULED", "STAGING").contains(event.getString("state"))) return Component.literal(formatTime(target));
        if (target <= now) return Component.translatable(active ? "jem.smp.event_finishing" : "jem.smp.event_waiting");
        long seconds = (target - now + 999) / 1000;
        String time = String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
        return Component.translatable(active ? "jem.smp.ends_in" : "jem.smp.starts_in", time);
    }

    private boolean eventActionAvailable(CompoundTag event, String action) {
        return event.getList("actions", Tag.TAG_STRING).contains(StringTag.valueOf(action));
    }

    private int renderEventLoot(GuiGraphics g, CompoundTag event, int x, int y, int w, int mx, int my) {
        var rewards = event.getList("reward", Tag.TAG_COMPOUND);
        if (rewards.isEmpty()) return 0;
        int columns = Math.max(1, (w - 12) / 38);
        int count = expandedRewards ? rewards.size() : Math.min(columns, rewards.size());
        int rows = (count + columns - 1) / columns;
        boolean overflow = rewards.size() > columns;
        int height = 31 + rows * 37 + (overflow ? 29 : 0);
        frame(g, x, y, w, height, INSET);
        text(g, Component.translatable("jem.smp.POSSIBLE_LOOT"), x + 8, y + 9, PEACH, w - 16);
        for (int index = 0; index < count; index++) {
            var reward = rewards.getCompound(index);
            ItemStack item;
            if (reward.contains("id", Tag.TAG_STRING)) item = ItemStack.of(reward);
            else {
                var id = ResourceLocation.tryParse(reward.getString("item"));
                var type = id == null ? Items.AIR : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
                item = new ItemStack(type, Math.max(1, reward.getInt("count")));
            }
            if (item.isEmpty()) continue;
            int ix = x + 8 + index % columns * 38;
            int iy = y + 29 + index / columns * 37;
            SmpGuiAssets.panel(g, ix + 3, iy - 3, 24, 24, INSET);
            g.renderItem(item, ix + 7, iy);
            int min = Math.max(1, reward.contains("minCount") ? reward.getInt("minCount") : item.getCount());
            int max = Math.max(min, reward.contains("maxCount") ? reward.getInt("maxCount") : item.getCount());
            text(g, Component.literal(min == max ? Integer.toString(min) : min + "–" + max), ix, iy + 19, MUTED, 34);
            if (mx >= ix && mx < ix + 36 && my >= Math.max(bodyY, iy) && my < Math.min(bodyBottom, iy + 33)) hoveredItem = item;
        }
        if (overflow) button(g, x + 7, y + height - 26, Math.min(145, w - 14), 21, expandedRewards ? "less_rewards" : "all_rewards", () -> expandedRewards = !expandedRewards, mx, my);
        return height + 12;
    }

    private int renderCookingProgress(GuiGraphics g, CompoundTag event, int x, int y, int w, int mx, int my) {
        if (!event.getString("activity").equals("COOKING_SHOW")) return 0;
        var requirements = event.getCompound("cookingRequirements");
        if (requirements.isEmpty()) return 0;
        var progress = event.getCompound("cookingProgress");
        int height = 28 + requirements.size() * 28;
        frame(g, x, y, w, height, ROW);
        text(g, Component.translatable("jem.smp.cooking_progress"), x + 8, y + 9, CREAM, w - 16);
        int rowY = y + 27;
        for (String id : requirements.getAllKeys().stream().sorted().toList()) {
            var key = ResourceLocation.tryParse(id);
            var item = key == null ? Items.AIR : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
            var stack = new ItemStack(item);
            g.renderItem(stack, x + 9, rowY);
            int required = requirements.getInt(id), supplied = Math.min(required, progress.getInt(id));
            text(g, stack.getHoverName(), x + 33, rowY + 3, CREAM, w - 104);
            text(g, Component.literal(supplied + " / " + required), x + w - 64, rowY + 3, supplied >= required ? GREEN : MUTED, 56);
            if (mx >= x + 8 && mx < x + 29 && my >= Math.max(bodyY, rowY) && my < Math.min(bodyBottom, rowY + 20)) hoveredItem = stack;
            rowY += 28;
        }
        return height + 12;
    }

    private void renderRows(GuiGraphics g, int mx, int my) {
        int y = bodyY - scroll;
        if (model.getList("rows", Tag.TAG_COMPOUND).isEmpty()) {
            SmpIcons.draw(g, tab, bodyX + bodyWidth / 2 - 20, bodyY + 25, 40);
            text(g, Component.translatable("jem.smp.empty"), bodyX + 16, bodyY + 80, CREAM, bodyWidth - 32);
            maxScroll = 0;
            return;
        }
        if (tab.equals("shops")) {
            renderShopRows(g, mx, my);
            return;
        }
        for (Tag tag : model.getList("rows", Tag.TAG_COMPOUND)) {
                var row = (CompoundTag) tag;
                if (!row.hasUUID("id")) continue;
                UUID id = row.getUUID("id");
                String title = row.getString("title");
                if (title.isBlank()) title = row.getString("name");
                Component label =
                        tab.equals("events")
                                ? Component.translatable("jem.smp." + title)
                                : Component.literal(title);
                String subtitle =
                        row.getString("name")
                                + " · "
                                + Component.translatable("jem.smp." + row.getString("state"))
                                        .getString();
                if (tab.equals("parties") && !row.getString("organizerName").isBlank()) subtitle = Component.translatable("jem.smp.organizer").getString() + ": " + row.getString("organizerName") + " · " + Component.translatable("jem.smp." + row.getString("state")).getString();
                row(
                        g,
                        y,
                        label,
                        subtitle,
                        () -> {
                            remember();
                            selected = id;
                            scroll = 0;
                            memberScroll = 0;
                            query();
                        },
                        mx,
                        my);
                if (tab.equals("profiles")) SmpIcons.face(g, id, bodyX + 9, y + 7, 28);
                else SmpIcons.draw(g, row.getString("activity"), bodyX + 9, y + 7, 28);
                y += ROW_HEIGHT + GAP;
            }
        maxScroll = Math.max(0, y + scroll - bodyBottom);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private void renderShopRows(GuiGraphics g, int mx, int my) {
        int rowHeight = 70;
        int y = bodyY - scroll;
        for (Tag value : model.getList("rows", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            if (!row.hasUUID("id")) continue;
            if (y + rowHeight >= bodyY && y < bodyBottom) {
                frame(g, bodyX, y, bodyWidth, rowHeight, ROW);
                if (row.hasUUID("owner")) SmpIcons.face(g, row.getUUID("owner"), bodyX + 8, y + 18, 32);
                int sellerWidth = Math.max(104, Math.min(150, bodyWidth / 4));
                text(g, Component.literal(row.getString("name")), bodyX + 46, y + 18, CREAM, sellerWidth - 48);
                String location = row.contains("distance") ? Math.round(row.getDouble("distance")) + " m" : dimensionName(row.getString("dimension"));
                text(g, Component.literal(location), bodyX + 46, y + 37, MUTED, sellerWidth - 48);
                int actionWidth = Math.min(76, Math.max(58, bodyWidth / 10));
                int mapWidth = actionWidth;
                int actionX = bodyX + bodyWidth - actionWidth - mapWidth - 12;
                int tradeX = bodyX + sellerWidth;
                int tradeWidth = Math.max(94, actionX - tradeX - 9);
                var offers = row.getList("offers", Tag.TAG_COMPOUND);
                var firstOffer = offers.isEmpty() ? new CompoundTag() : offers.getCompound(0);
                var payment = ItemStack.of(offers.isEmpty() ? row.getCompound("payment") : firstOffer.getCompound("payment"));
                var output = ItemStack.of(offers.isEmpty() ? row.getCompound("offer") : firstOffer.getCompound("output"));
                if (offers.isEmpty() && !payment.isEmpty()) payment.setCount(Math.max(1, row.getInt("price")));
                if (offers.isEmpty() && !output.isEmpty()) output.setCount(Math.max(1, row.getInt("quantity")));
                int available = offers.isEmpty() ? (row.getString("state").equals("OUT_OF_STOCK") ? 0 : -1) : firstOffer.getInt("availableTrades");
                Component stock = available == 0 ? Component.translatable("jem.smp.out_of_stock") : available < 0 ? Component.translatable("jem.smp.unlimited_stock") : Component.translatable("jem.smp.available_stock", available * Math.max(1, output.getCount()));
                text(g, stock, tradeX, y + 7, available == 0 ? RED : MUTED, tradeWidth);
                int iconSize = 30;
                int paymentX = tradeX + 3;
                int outputX = tradeX + Math.max(iconSize + 28, tradeWidth / 2 + 8);
                renderShopTradeStack(g, payment, paymentX, y + 29, iconSize, mx, my);
                int arrowX = Math.max(paymentX + iconSize + 7, outputX - 22);
                text(g, Component.literal("→"), arrowX, y + 39, PEACH, 18);
                renderShopTradeStack(g, output, outputX, y + 29, iconSize, mx, my);
                UUID shop = row.getUUID("id");
                if (row.getBoolean("remoteAvailable") && row.getString("state").equals("ACTIVE"))
                    button(g, actionX, y + 20, actionWidth, 30, "buy", () -> sendFor(shop, row.getInt("revision"), "buy", new JsonObject()), mx, my);
                else disabledButton(g, actionX, y + 20, actionWidth, 30, "buy", shopUnavailableReason(row), mx, my);
                iconButton(g, actionX + actionWidth + 4, y + 20, mapWidth, 30, "map", () -> navigateShop(row), mx, my);
            }
            y += rowHeight + GAP;
        }
        maxScroll = Math.max(0, y + scroll - bodyBottom);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private void renderShopTradeStack(GuiGraphics g, ItemStack stack, int x, int y, int size, int mx, int my) {
        if (stack.isEmpty()) return;
        float scale = size / 16f;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.renderItem(stack, 0, 0);
        g.renderItemDecorations(font, stack, 0, 0);
        g.pose().popPose();
        if (mx >= x && mx < x + size && my >= y && my < y + size) hoveredItem = stack;
    }

    private String dimensionName(String value) {
        int separator = value.indexOf(':');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    private String shopUnavailableReason(CompoundTag row) {
        if (!row.getBoolean("remoteAvailable")) return "shop_unloaded";
        return switch (row.getString("state")) {
            case "OUT_OF_STOCK" -> "out_of_stock";
            case "PAYMENT_FULL" -> "shop_payment_full";
            default -> "shop_unconfigured";
        };
    }

    private void renderDetail(GuiGraphics g, int mx, int my) {
        if (tab.equals("events")) { renderEventDetail(g, model.getCompound("detail"), mx, my); return; }
        if (tab.equals("parties")) { renderPartyLobby(g, mx, my); return; }
        renderDetailBody(g, mx, my);
    }

    private boolean openLocation(CompoundTag detail) {
        var dimension = ResourceLocation.tryParse(detail.getString("dimension"));
        if (dimension == null) return false;
        var pos = net.minecraft.core.BlockPos.of(detail.getLong("position"));
        if (!tab.equals("events")) {
            UUID id = detail.hasUUID("id") ? detail.getUUID("id") : UUID.randomUUID();
            String label = detail.getString("title");
            if (label.isEmpty()) label = detail.getString("name");
            if (label.isEmpty()) label = Component.translatable("jem.smp.map").getString();
            TemporaryWaypoints.showPreview(id, label, dimension, pos);
        }
        boolean opened = com.siirio.jemserver.client.xaero.MapClient.open(dimension, pos);
        if (!opened) {
            TemporaryWaypoints.clearLocal();
            String coordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ();
            minecraft.keyboardHandler.setClipboard(coordinates);
            minecraft.getToasts().addToast(new net.minecraft.client.gui.components.toasts.SystemToast(
                    net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                    Component.translatable("jem.smp.map"), Component.translatable("jem.smp.map_fallback", coordinates)));
        }
        return opened;
    }

    private void navigateShop(CompoundTag row) {
        openLocation(row);
    }

    private void renderDetailBody(GuiGraphics g, int mx, int my) {
        var row = model.getCompound("detail");
        if (tab.equals("profiles")) { renderProfile(g, row, mx, my); return; }
        int y = bodyY - scroll;
        String title = row.getString("title");
        if (title.isBlank()) title = row.getString("name");
        frame(g, bodyX, y, bodyWidth, 48, INSET);
        SmpIcons.draw(g, tab.equals("events") ? row.getString("activity") : tab, bodyX + 8, y + 8, 30);
        text(g, tab.equals("events") ? Component.translatable("jem.smp." + title) : Component.literal(title), bodyX + 48, y + 10, CREAM, bodyWidth - 56);
        text(g, Component.translatable("jem.smp." + row.getString("state")), bodyX + 48, y + 28, stateColor(row.getString("state")), bodyWidth - 56);
        y += 56;
        for (String key :
                List.of("activity", "description", "name", "language")) {
            String value = row.getString(key);
            if (value.isBlank()) continue;
            var label = Set.of("category", "activity", "state").contains(key) ? Component.translatable("jem.smp." + value) : Component.literal(value);
            var lines = font.split(label, bodyWidth - 12);
            for (var line : lines) {
                g.drawString(font, line, bodyX + 4, y, MUTED, false);
                y += 12;
            }
            y += 4;
        }
        for (String key :
                List.of("planned", "started", "ends", "scheduled", "deadline", "firstJoin"))
            if (row.getLong(key) > 0) {
                text(
                        g,
                        Component.translatable("jem.smp." + key)
                                .append(": " + formatTime(row.getLong(key))),
                        bodyX + 4,
                        y,
                        MUTED,
                        bodyWidth - 8);
                y += 16;
            }
        if (row.contains("dimension")) {
            var pos = net.minecraft.core.BlockPos.of(row.getLong("position"));
            text(
                    g,
                    Component.literal(
                            pos.getX()
                                    + ", "
                                    + pos.getY()
                                    + ", "
                                    + pos.getZ()
                                    + " · "
                                    + row.getString("dimension")),
                    bodyX + 4,
                    y,
                    PEACH,
                    bodyWidth - 8);
            y += 20;
        }
        var actions = row.getList("actions", Tag.TAG_STRING);
        int columns = Math.max(1, bodyWidth / 130), buttonWidth = bodyWidth / columns - 4;
        for (int i = 0; i < actions.size(); i++) {
            String action = actions.getString(i);
            int x = bodyX + (i % columns) * (buttonWidth + 4), cy = y + (i / columns) * 29;
            button(g, x, cy, buttonWidth, 25, action, () -> invoke(action), mx, my);
        }
        y += ((actions.size() + columns - 1) / columns) * 29;
        maxScroll = Math.max(0, y + scroll - bodyBottom + 6);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private void invoke(String action) {
        if (action.equals("navigate")) { openLocation(model.getCompound("detail")); return; }
        if (action.equals("territory_rights")) ClaimsScreenBridge.capture(this);
        if (action.equals("invite") && tab.equals("parties")) { openForm("invite"); return; }
        if (action.equals("dissolve")) { openForm("dissolve_confirm"); return; }
        if (Set.of("approve", "remove", "transfer").contains(action) && participant != null) {
            var args = new JsonObject();
            args.addProperty("player", participant.toString());
            send(action, args);
        } else if (Set.of("message", "approve", "remove", "transfer").contains(action))
            openForm(action);
        else send(action, new JsonObject());
    }

    private void openForm(String action) {
        if (waiting) return;
        clearForm();
        form = action;
        choices.clear();
        formScroll = 0;
        error = "";
        if (action.equals("create")) {
            var scheduled = java.time.ZonedDateTime.now().plusHours(1).withSecond(0).withNano(0);
            choice("activity", List.of("CUSTOM"));
            field("title", "");
            field("description", "");
            field("slots", "0");
            choice("solo", List.of("false", "true"));
            choice("approval", List.of("false", "true"));
            choice("schedule", List.of("now", "scheduled"));
            choice("language", List.of("ru_ru", "en_us"));
            choice("year", java.util.stream.IntStream.rangeClosed(scheduled.getYear(), scheduled.getYear() + 2).mapToObj(Integer::toString).toList());
            choice("month", java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(value -> String.format(Locale.ROOT, "%02d", value)).toList());
            choice("day", java.util.stream.IntStream.rangeClosed(1, 31).mapToObj(value -> String.format(Locale.ROOT, "%02d", value)).toList());
            choice("hour", java.util.stream.IntStream.rangeClosed(0, 23).mapToObj(value -> String.format(Locale.ROOT, "%02d", value)).toList());
            choice("minute", List.of("00", "15", "30", "45"));
            fields.get("year").setValue(Integer.toString(scheduled.getYear()));
            fields.get("month").setValue(String.format(Locale.ROOT, "%02d", scheduled.getMonthValue()));
            fields.get("day").setValue(String.format(Locale.ROOT, "%02d", scheduled.getDayOfMonth()));
            fields.get("hour").setValue(String.format(Locale.ROOT, "%02d", scheduled.getHour()));
            fields.get("minute").setValue(String.format(Locale.ROOT, "%02d", scheduled.getMinute() / 15 * 15));
        } else if (action.equals("message")) {
            messageBox = new MultiLineEditBox(font, bodyX + 8, bodyY + 52, Math.max(80, bodyWidth - 16), Math.max(72, bodyBottom - bodyY - 94), Component.translatable("jem.smp.field.message"), Component.translatable("jem.smp.message_placeholder"));
            messageBox.setCharacterLimit(256);
            addWidget(messageBox);
        } else if (!Set.of("invite", "dissolve_confirm").contains(action) || !tab.equals("parties")) field("player", "");
    }

    private void choice(String name, List<String> values) {
        choices.put(name, values);
        field(name, values.get(0));
        fields.get(name).setEditable(false);
    }

    private void field(String name, String initial) {
        var box = new EditBox(font, 0, 0, 100, 20, Component.translatable("jem.smp.field." + name));
        box.setMaxLength(name.equals("description") ? 512 : 256);
        box.setValue(initial);
        box.setTextColor(CREAM);
        positionField(box, fields.size());
        fields.put(name, box);
        addWidget(box);
    }

    private void positionField(EditBox box, int index) {
        int columns = formColumns();
        int w = bodyWidth / columns - 8;
        box.setX(bodyX + (index % columns) * (w + 8));
        box.setY(bodyY + 18 + (index / columns) * FIELD_HEIGHT - formScroll);
        box.setWidth(w);
    }

    private int formColumns() {
        int preferred = bodyWidth >= 390 ? 3 : bodyWidth >= 240 ? 2 : 1;
        int count = Math.max(1, visibleFormFields().size());
        int availableRows = Math.max(1, (bodyBottom - bodyY - 6) / FIELD_HEIGHT);
        int required = (count + availableRows - 1) / availableRows;
        int widthLimit = Math.max(1, bodyWidth / 82);
        return Math.max(1, Math.min(widthLimit, Math.max(preferred, required)));
    }

    private List<Map.Entry<String, EditBox>> visibleFormFields() {
        boolean scheduled = fields.containsKey("schedule") && fields.get("schedule").getValue().equals("scheduled");
        return fields.entrySet().stream().filter(entry -> scheduled || !Set.of("year", "month", "day", "hour", "minute").contains(entry.getKey())).toList();
    }

    private void positionMessageBox() {
        if (messageBox == null) return;
        messageBox.setX(bodyX + 8);
        messageBox.setY(bodyY + 52);
        messageBox.setWidth(Math.max(80, bodyWidth - 16));
        messageBox.setHeight(Math.max(72, bodyBottom - bodyY - 94));
    }

    private void renderForm(GuiGraphics g, int mx, int my, float partial) {
        if (form.equals("invite") && tab.equals("parties")) { renderInvites(g, mx, my); return; }
        if (form.equals("message")) { renderMessageForm(g, mx, my, partial); return; }
        if (form.equals("dissolve_confirm")) {
            int y = bodyY + 12;
            for (var line : font.split(Component.translatable("jem.smp.dissolve_confirm"), Math.max(20, bodyWidth - 16))) {
                g.drawString(font, line, bodyX + 8, y, CREAM, false); y += 12;
            }
            int w = Math.max(30, (bodyWidth - 8) / 2);
            button(g, bodyX, y + 14, w, 24, "confirm", () -> send("dissolve", new JsonObject()), mx, my);
            button(g, bodyX + w + 8, y + 14, w, 24, "cancel", this::clearForm, mx, my);
            return;
        }
        int index = 0;
        var visible = visibleFormFields();
        boolean parentClipping = clippingBody;
        clippingBody = true;
        g.enableScissor(bodyX, bodyY, bodyX + bodyWidth, bodyBottom);
        try {
            for (var entry : visible) {
                var box = entry.getValue();
                positionField(box, index++);
                String name = entry.getKey();
                text(
                        g,
                        Component.translatable("jem.smp.field." + name),
                        box.getX(),
                        box.getY() - 12,
                        MUTED,
                        box.getWidth());
                if (choices.containsKey(name)) {
                    dropdown(g, name, box, mx, my);
                } else box.render(g, mx, my, partial);
            }
            if (!expandedChoice.isEmpty() && fields.containsKey(expandedChoice)) renderDropdownOptions(g, expandedChoice, fields.get(expandedChoice), mx, my);
        } finally {
            g.disableScissor();
            clippingBody = parentClipping;
        }
        button(
                g,
                bodyX,
                bodyBottom + 9,
                90,
                23,
                "submit",
                () -> {
                    var args = new JsonObject();
                    fields.forEach((key, box) -> {
                        if (!Set.of("schedule", "year", "month", "day", "hour", "minute").contains(key)) args.addProperty(key, box.getValue());
                    });
                    try {
                        if (fields.containsKey("schedule") && fields.get("schedule").getValue().equals("scheduled")) {
                            var time = java.time.LocalDateTime.of(
                                    Integer.parseInt(fields.get("year").getValue()),
                                    Integer.parseInt(fields.get("month").getValue()),
                                    Integer.parseInt(fields.get("day").getValue()),
                                    Integer.parseInt(fields.get("hour").getValue()),
                                    Integer.parseInt(fields.get("minute").getValue()));
                            args.addProperty("scheduled", time.atZone(java.time.ZoneId.systemDefault()).toInstant().toString());
                        }
                    } catch (java.time.DateTimeException invalid) {
                        error = "invalid_deadline";
                        return;
                    }
                    send(form, args);
                },
                mx,
                my);

    }

    private void renderMessageForm(GuiGraphics g, int mx, int my, float partial) {
        var detail = model.getCompound("detail");
        UUID id = detail.hasUUID("id") ? detail.getUUID("id") : new UUID(0, 0);
        frame(g, bodyX, bodyY, bodyWidth, bodyBottom - bodyY, INSET);
        if (detail.hasUUID("id")) SmpIcons.face(g, id, bodyX + 9, bodyY + 9, 28);
        text(g, Component.translatable("jem.smp.write_to", detail.getString("name")), bodyX + 45, bodyY + 11, CREAM, bodyWidth - 54);
        text(g, Component.translatable("jem.smp.message_stays_open"), bodyX + 45, bodyY + 28, MUTED, bodyWidth - 54);
        positionMessageBox();
        messageBox.render(g, mx, my, partial);
        int buttonY = bodyBottom - 32;
        button(g, bodyX + 8, buttonY, Math.min(120, bodyWidth - 16), 24, "send", () -> {
            var args = new JsonObject();
            args.addProperty("message", messageBox.getValue());
            send("message", args);
        }, mx, my);
    }

    private void dropdown(GuiGraphics g, String name, EditBox box, int mx, int my) {
        boolean open = expandedChoice.equals(name);
        boolean over = mx >= box.getX() && mx < box.getX() + box.getWidth() && my >= box.getY() && my < box.getY() + 20;
        SmpGuiAssets.button(g, box.getX(), box.getY(), box.getWidth(), 20, "secondary", over || open, false);
        text(g, choiceLabel(name, box.getValue()), box.getX() + 6, box.getY() + 6, CREAM, box.getWidth() - 24);
        text(g, Component.literal(open ? "▲" : "▼"), box.getX() + box.getWidth() - 15, box.getY() + 6, PEACH, 10);
        hit(box.getX(), box.getY(), box.getWidth(), 20, () -> expandedChoice = open ? "" : name);
    }

    private void renderDropdownOptions(GuiGraphics g, String name, EditBox box, int mx, int my) {
        var values = choices.get(name);
        if (values == null || values.isEmpty()) return;
        int maxRows = Math.max(1, (bodyBottom - bodyY - 2) / 20);
        int columns = Math.max(1, (values.size() + maxRows - 1) / maxRows);
        int rows = (values.size() + columns - 1) / columns;
        int optionWidth = Math.min(box.getWidth(), Math.max(34, bodyWidth / columns));
        int menuWidth = optionWidth * columns;
        int menuHeight = rows * 20;
        int menuX = Math.max(bodyX, Math.min(box.getX(), bodyX + bodyWidth - menuWidth));
        int menuY = box.getY() + 21;
        if (menuY + menuHeight > bodyBottom) menuY = box.getY() - menuHeight - 1;
        g.pose().pushPose();
        g.pose().translate(0, 0, 250);
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            int x = menuX + i / rows * optionWidth;
            int y = menuY + i % rows * 20;
            boolean over = mx >= x && mx < x + optionWidth && my >= y && my < y + 20;
            SmpGuiAssets.button(g, x, y, optionWidth, 20, "secondary", over, value.equals(box.getValue()));
            text(g, choiceLabel(name, value), x + 6, y + 6, value.equals(box.getValue()) ? PEACH : CREAM, optionWidth - 12);
            hit(x, y, optionWidth, 20, () -> {
                box.setValue(value);
                expandedChoice = "";
            });
        }
        g.pose().popPose();
    }

    private Component choiceLabel(String name, String value) {
        if (name.equals("activity")) return Component.translatable("jem.smp." + value);
        if (name.equals("solo") || name.equals("approval")) return Component.translatable("jem.smp." + value);
        if (name.equals("schedule") || name.equals("language")) return Component.translatable("jem.smp.choice." + value);
        if (name.equals("month")) {
            var locale = minecraft.getLanguageManager().getSelected().equals("ru_ru") ? new Locale("ru", "RU") : Locale.ENGLISH;
            return Component.literal(java.time.Month.of(Integer.parseInt(value)).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale));
        }
        return Component.literal(value);
    }

    private boolean currentDetail() {
        return model.contains("detail")
                && model.getCompound("detail").hasUUID("id")
                && model.getCompound("detail").getUUID("id").equals(selected);
    }

    private void send(String action, JsonObject args) {
        if (waiting
                || selected != null
                        && !Set.of("create", "claims", "claim_rewards").contains(action)
                        && !currentDetail()) return;
        sendFor(selected, model.getCompound("detail").getInt("revision"), action, args);
    }

    private void sendFor(UUID target, int revision, String action, JsonObject args) {
        if (waiting) return;
        try {
            pending = SmpClientRequests.action(UUID.randomUUID(), tab, target, revision, action, args);
        } catch (com.siirio.jemserver.smp.SmpActionFailure failure) {
            error = failure.code();
            return;
        }
        waiting = true;
        lastSent = System.currentTimeMillis();
        SmpNetwork.action(pending);
    }

    private void cyclePartyFilter() {
        List<String> values = List.of("", "mine", "accepted", "history");
        filter = values.get((values.indexOf(filter) + 1) % values.size());
        page = 0;
        selected = null;
        scroll = 0;
        query();
    }

    private void row(
            GuiGraphics g,
            int y,
            Component title,
            String subtitle,
            Runnable action,
            int mx,
            int my) {
        if (y + ROW_HEIGHT < bodyY || y > bodyBottom) return;
        boolean over =
                mx >= bodyX
                        && mx < bodyX + bodyWidth
                        && my >= Math.max(y, bodyY)
                        && my < Math.min(y + ROW_HEIGHT, bodyBottom);
        frame(g, bodyX, y, bodyWidth, ROW_HEIGHT, over ? HOVER : ROW);
        text(g, title, bodyX + 46, y + 7, CREAM, bodyWidth - 54);
        text(g, Component.literal(subtitle), bodyX + 46, y + 24, MUTED, bodyWidth - 54);
        hit(bodyX, y, bodyWidth, ROW_HEIGHT, action);
    }

    private void button(
            GuiGraphics g,
            int x,
            int y,
            int w,
            int h,
            String key,
            Runnable action,
            int mx,
            int my) {
        boolean over = mx >= x && mx < x + w && my >= y && my < y + h;
        String role = switch (key) {
            case "start", "claim", "claim_all", "create", "create_party", "submit" -> "primary";
            case "ready", "confirm", "approve", "buy" -> "confirm";
            case "leave", "dissolve", "remove" -> "danger";
            default -> "secondary";
        };
        SmpGuiAssets.button(g, x, y, w, h, role, over, waiting && !key.equals("close"));
        Component label = Component.translatable("jem.smp." + key);
        if (choices.containsKey("location"))
            for (Tag value : model.getList("locations", Tag.TAG_COMPOUND)) {
                var location = (CompoundTag) value;
                if (location.getUUID("id").toString().equals(key)) {
                    label = Component.literal(location.getString("name"));
                    break;
                }
            }
        boolean icon = w >= 70 && h >= 20 && SmpIcons.item(key) != Items.AIR;
        int available = w - (icon ? 30 : 12);
        int labelWidth = Math.min(font.width(label), available);
        int contentWidth = labelWidth + (icon ? 20 : 0);
        int contentX = x + Math.max(6, (w - contentWidth) / 2);
        if (icon) SmpIcons.draw(g, key, contentX, y + (h - 16) / 2, 16);
        text(g, label, contentX + (icon ? 20 : 0), y + (h - 9) / 2, waiting ? MUTED : role.equals("primary") ? OUTLINE : CREAM, available);
        if (over && font.width(label) > w - (icon ? 30 : 12)) hoveredLabel = label;
        if (!waiting || key.equals("retry") || key.equals("close")) hit(x, y, w, h, action);
    }

    private void iconButton(GuiGraphics g, int x, int y, int w, int h, String key, Runnable action, int mx, int my) {
        boolean over = mx >= x && mx < x + w && my >= y && my < y + h;
        SmpGuiAssets.button(g, x, y, w, h, key.equals("map") ? "map" : "secondary", over, waiting);
        SmpIcons.draw(g, key, x + (w - 16) / 2, y + (h - 16) / 2, 16);
        if (over) hoveredLabel = Component.translatable("jem.smp." + key);
        if (!waiting) hit(x, y, w, h, action);
    }

    private void hit(int x, int y, int w, int h, Runnable action) {
        if (clippingBody) {
            int right = Math.min(x + w, bodyX + bodyWidth), bottom = Math.min(y + h, bodyBottom);
            x = Math.max(x, bodyX);
            y = Math.max(y, bodyY);
            w = right - x;
            h = bottom - y;
        }
        if (h > 0 && w > 0) hits.add(new SmpHit(x, y, w, h, action));
    }

    private void hitWithin(int x, int y, int w, int h, int clipX, int clipY, int clipWidth, int clipHeight, Runnable action) {
        int right = Math.min(x + w, clipX + clipWidth);
        int bottom = Math.min(y + h, clipY + clipHeight);
        x = Math.max(x, clipX);
        y = Math.max(y, clipY);
        if (right > x && bottom > y) hits.add(new SmpHit(x, y, right - x, bottom - y, action));
    }

    private void text(GuiGraphics g, Component text, int x, int y, int color, int max) {
        String value = text.getString();
        if (font.width(value) > max)
            value = font.plainSubstrByWidth(value, Math.max(0, max - 8)) + "…";
        g.drawString(font, value, x, y, color, false);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h, int fill) {
        SmpGuiAssets.panel(g, x, y, w, h, fill);
    }

    private void restoreBodyScissor(GuiGraphics g) {
        if (!clippingBody) return;
        var window = minecraft.getWindow();
        double scale = window.getGuiScale();
        int x = (int) Math.floor(bodyX * scale);
        int y = (int) Math.floor(window.getHeight() - bodyBottom * scale);
        int width = (int) Math.ceil(bodyWidth * scale);
        int height = (int) Math.ceil((bodyBottom - bodyY) * scale);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(x, y, width, height);
    }

    private static String formatTime(long value) {
        return java.time.Instant.ofEpochMilli(value)
                .atZone(java.time.ZoneId.systemDefault())
                .format(
                        java.time.format.DateTimeFormatter.ofLocalizedDateTime(
                                java.time.format.FormatStyle.SHORT));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (form.isEmpty() && selected != null && tab.equals("parties")
                && x >= partyRewardX && x < partyRewardX + partyRewardWidth
                && y >= partyRewardY && y < partyRewardY + ItemRewardStrip.CELL * 2) {
            partyRewardScroll = Math.max(0, Math.min(partyRewardMaxScroll, partyRewardScroll - (int) (delta * ItemRewardStrip.CELL)));
            return true;
        }
        if (form.isEmpty() && selected != null && tab.equals("parties")
                && x >= memberListX && x < memberListX + memberListWidth
                && y >= memberListY && y < memberListY + memberListHeight) {
            memberScroll = Math.max(0, Math.min(memberMaxScroll, memberScroll - (int) (delta * 22)));
            return true;
        }
        if (tab.equals("prizes") && hoveredPrize != null) {
            int offset = Math.max(0, prizeOffsets.getOrDefault(hoveredPrize, 0) - (int) (delta * ItemRewardStrip.CELL));
            prizeOffsets.put(hoveredPrize, offset);
            return true;
        }
        if (messageBox != null && messageBox.isMouseOver(x, y)) return messageBox.mouseScrolled(x, y, delta);
        if (!form.isEmpty() && x >= bodyX && y >= bodyY && y < bodyBottom) {
            if (form.equals("invite")) {
                int max = Math.max(0, model.getCompound("detail").getList("inviteCandidates", Tag.TAG_COMPOUND).size() * ROW_HEIGHT + 26 - (bodyBottom - bodyY));
                formScroll = Math.max(0, Math.min(max, formScroll - (int) (delta * 22)));
            }
            return true;
        }
        if (form.isEmpty() && x >= bodyX && y >= bodyY && y < bodyBottom) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (delta * 22)));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) return true;
        if (!hoveredItem.isEmpty() && Set.of("events", "parties", "prizes").contains(tab) && JeiItemLink.open(hoveredItem)) return true;
        String sidebarTab = sidebar.at(x, y);
        if (sidebarTab != null && !waiting) { selectTab(sidebarTab); return true; }
        if (form.isEmpty() && selected == null) {
            if (tab.equals("shops") && seller.mouseClicked(x, y, button)) {
                setFocused(seller);
                return true;
            }
            if (search.mouseClicked(x, y, button)) {
                setFocused(search);
                return true;
            }
        } else if (messageBox != null && messageBox.mouseClicked(x, y, button)) {
            setFocused(messageBox);
            return true;
        } else if (y >= bodyY && y < bodyBottom)
            for (var entry : fields.entrySet())
                if (!choices.containsKey(entry.getKey())
                        && entry.getValue().mouseClicked(x, y, button)) {
                    setFocused(entry.getValue());
                    return true;
                }
        for (int i = hits.size() - 1; i >= 0; i--) {
            var hit = hits.get(i);
            if (hit.contains(x, y)) {
                hit.action().run();
                return true;
            }
        }
        return true;
    }

    @Override
    public void removed() {
        SmpNetwork.query(new SmpQuery("", "", "", 0, null));
        com.siirio.jemserver.client.ui.SmpRenderState.restoreAfterScreen();
        super.removed();
    }

    @Override
    public void onClose() {
        com.siirio.jemserver.client.ui.SmpRenderState.restoreAfterScreen();
        super.onClose();
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        com.siirio.jemserver.client.ui.SmpRenderState.restoreAfterScreen();
        super.resize(minecraft, width, height);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        memberScroll = Math.max(0, Math.min(memberScroll, memberMaxScroll));
    }
}
