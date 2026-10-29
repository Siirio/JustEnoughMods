package com.siirio.jemsettlements.compat.xaero;

import com.siirio.jemsettlements.client.SettlementClientState;
import com.siirio.jemsettlements.client.SettlementPriceRenderer;
import com.siirio.jemsettlements.client.TerritoryDragState;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.town.TerritoryQuote;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Connection-scoped server projection. Xaero and personal waypoint state are never rewritten. */
public final class SettlementMapClient {
    public record Area(UUID id, String name, ResourceLocation dimension, BlockPos center, boolean owned,
                       int minX, int minZ, int maxX, int maxZ) { }
    public record Draft(Area area, int x, int z, TerritoryQuote quote) { }
    private static List<Area> areas = List.of();
    private static Draft draft;
    private static boolean coins, received;
    private static TerritoryDragState state = TerritoryDragState.IDLE;
    private static Object activeScreen;
    private static Area expansionArea;
    private static long perBlock, minimum;
    private static long lastRequest;
    private SettlementMapClient() { }
    public static void opened(Object screen) { cancel(); activeScreen = screen; refresh(true); }
    public static void cancel() { expansionArea = null; state = TerritoryDragState.CANCEL; draft = null; state = TerritoryDragState.IDLE; }
    /** Also catches removal/focus loss when the native screen receives no mouse-up. */
    public static void tick() {
        var client = Minecraft.getInstance();
        if (activeScreen == null) return;
        if (client.screen != activeScreen || !client.isWindowActive()) { cancel(); activeScreen = null; return; }
        if (state == TerritoryDragState.DRAGGING && org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getWindow(), 0) != org.lwjgl.glfw.GLFW.GLFW_PRESS) cancel();
        if (org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) == org.lwjgl.glfw.GLFW.GLFW_PRESS) cancel();
    }
    public static void clear() { areas = List.of(); cancel(); activeScreen = null; received = false; perBlock = 0; minimum = 0; lastRequest = 0; }
    public static void accept(CompoundTag view) {
        List<Area> rows = new ArrayList<>(); var tags = view.getList("MapAreas", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(256, tags.size()); i++) {
            var row = tags.getCompound(i); var dim = ResourceLocation.tryParse(row.getString("Dimension"));
            if (dim == null || !row.hasUUID("Id")) continue;
            rows.add(new Area(row.getUUID("Id"), row.getString("Name"), dim, BlockPos.of(row.getLong("Center")),
                    row.getBoolean("Owned"), row.getInt("MinX"), row.getInt("MinZ"), row.getInt("MaxX"), row.getInt("MaxZ")));
        }
        areas = List.copyOf(rows); coins = view.getBoolean("CoinCurrency");
        long nextRate = view.getLong("ExpansionCostPerBlock"), nextMinimum = view.getLong("ExpansionMinimumCost");
        if (nextRate != perBlock || nextMinimum != minimum) cancel();
        perBlock = nextRate; minimum = nextMinimum; received = perBlock > 0L && minimum > 0L;
        if (state == TerritoryDragState.COMMIT && !SettlementClientState.pending()) cancel();
        if (draft != null && !areas.contains(draft.area())) cancel();
        if (view.hasUUID("Request") && view.contains("Reason") && !view.getString("Result").equals("RETRY")) {
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable(view.getString("Reason")), true);
        }
    }
    private static void refresh(boolean force) {
        long now = System.currentTimeMillis();
        SettlementClientState.reconcile();
        if (!SettlementClientState.pending() && SettlementNetwork.supported() && (force || now - lastRequest >= 5000L)) {
            lastRequest = now; SettlementNetwork.action("mapopen", null, new CompoundTag(), UUID.randomUUID());
        }
    }
    private static Area area(ResourceLocation dim, int x, int z, boolean owned) {
        return areas.stream().filter(a -> (!owned || a.owned()) && a.dimension().equals(dim)
                && x >= a.minX() && x <= a.maxX() && z >= a.minZ() && z <= a.maxZ()).findFirst().orElse(null);
    }
    public static Area ownedAt(ResourceLocation dim,int x,int z) { return area(dim,x,z,true); }
    public static void enter(Area value) { cancel(); expansionArea=value; activeScreen=Minecraft.getInstance().screen; }
    public static boolean dragging() { return state==TerritoryDragState.DRAGGING; }
    public static boolean coins() { return coins; }
    public static boolean begin(ResourceLocation dim, int x, int z) {
        if (!received || state == TerritoryDragState.COMMIT || SettlementClientState.pending()) return false;
        Area area=expansionArea;
        if(area==null || !area.dimension().equals(dim)) return false;
        activeScreen = Minecraft.getInstance().screen;
        draft = new Draft(area, x, z, TerritoryQuote.union(area.minX(), area.minZ(), area.maxX(), area.maxZ(), x, z, x, z, perBlock, minimum));
        state = TerritoryDragState.DRAGGING; return true;
    }
    public static void update(ResourceLocation dim, int x, int z) {
        if (state != TerritoryDragState.DRAGGING || draft == null) return;
        if (!draft.area().dimension().equals(dim)) { cancel(); return; }
        Area a = draft.area();
        draft = new Draft(a, x, z, TerritoryQuote.union(a.minX(), a.minZ(), a.maxX(), a.maxZ(), x, z, x, z, perBlock, minimum));
    }
    public static boolean finish() {
        if (state != TerritoryDragState.DRAGGING) return false;
        if (draft == null || draft.quote().addedCopper() <= 0L) cancel();
        else state = TerritoryDragState.PREVIEW;
        return true;
    }
    public static Draft pending(ResourceLocation dim) {
        return state == TerritoryDragState.PREVIEW && draft != null && draft.area().dimension().equals(dim) && draft.quote().addedCopper() > 0L ? draft : null;
    }
    public static Component price(Draft value) { return coins ? SettlementPriceRenderer.formatAll(value.quote().addedCopper(), true) : SettlementPriceRenderer.format(value.quote().addedCopper(), false); }
    public static void confirm(Draft value) {
        if (state != TerritoryDragState.PREVIEW || draft != value || SettlementClientState.pending()) return;
        var a = value.area(); CompoundTag payload = new CompoundTag(); payload.putInt("X", value.x()); payload.putInt("Z", value.z());
        payload.putInt("ExpectedMinX", a.minX()); payload.putInt("ExpectedMaxX", a.maxX());
        payload.putInt("ExpectedMinZ", a.minZ()); payload.putInt("ExpectedMaxZ", a.maxZ());
        payload.putLong("ExpectedCost", value.quote().addedCopper()); payload.putBoolean("ExpectedCoins", coins);
        if (SettlementClientState.action("mapexpand", a.id(), payload)) { state = TerritoryDragState.COMMIT; draft = null; }
    }
    public static void render(GuiGraphics graphics, ResourceLocation dim, double cx, double cz, double scale,
                              int width, int height, int blockX, int blockZ, int mouseX, int mouseY, boolean menuOpen) {
        refresh(false); if (!received) return;
        if (state == TerritoryDragState.PREVIEW && !menuOpen) cancel();
        if (draft != null && !draft.area().dimension().equals(dim)) cancel();
        update(dim, blockX, blockZ);
        for (Area a : areas) if (a.dimension().equals(dim)) rectangle(graphics, a.minX(), a.minZ(), a.maxX(), a.maxZ(),
                a.owned() ? 0xE4AF87 : 0xB37D55, a.owned() ? 0x42 : 0x2C, cx, cz, scale, width, height);
        if (draft != null && draft.area().dimension().equals(dim)) {
            var q = draft.quote(); rectangle(graphics, q.minX(), q.minZ(), q.maxX(), q.maxZ(), 0x82B67A, 0x56, cx, cz, scale, width, height);
        }
        Area hover = area(dim, blockX, blockZ, false);
        if (hover != null && mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height) {
            var font = Minecraft.getInstance().font; String label = font.plainSubstrByWidth(hover.name(), Math.max(1, width - 8));
            graphics.drawString(font, label, Math.max(4, Math.min(width - font.width(label) - 4, mouseX + 8)),
                    Math.max(4, Math.min(height - font.lineHeight - 4, mouseY + 12)), 0xFFF0D7, true);
        }
    }
    private static void rectangle(GuiGraphics g, int minX, int minZ, int maxX, int maxZ, int color, int alpha,
                                  double cx, double cz, double scale, int width, int height) {
        if (!Double.isFinite(scale) || scale <= 0) return;
        double left = width / 2.0 + (minX - cx) * scale, top = height / 2.0 + (minZ - cz) * scale;
        double right = width / 2.0 + ((double) maxX + 1 - cx) * scale, bottom = height / 2.0 + ((double) maxZ + 1 - cz) * scale;
        if (right < 0 || bottom < 0 || left > width || top > height) return;
        int x1 = (int) Math.max(0, Math.floor(left)), y1 = (int) Math.max(0, Math.floor(top));
        int x2 = (int) Math.min(width, Math.ceil(right)), y2 = (int) Math.min(height, Math.ceil(bottom));
        g.fill(x1, y1, x2, y2, alpha << 24 | color);
        int border = 0xD0000000 | color;
        if (left >= 0) g.fill(x1, y1, Math.min(x1 + 1, x2), y2, border);
        if (right <= width) g.fill(Math.max(x1, x2 - 1), y1, x2, y2, border);
        if (top >= 0) g.fill(x1, y1, x2, Math.min(y1 + 1, y2), border);
        if (bottom <= height) g.fill(x1, Math.max(y1, y2 - 1), x2, y2, border);
    }
}
