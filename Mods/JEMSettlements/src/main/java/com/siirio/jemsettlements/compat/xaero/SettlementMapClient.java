package com.siirio.jemsettlements.compat.xaero;

import com.siirio.jemsettlements.client.SettlementClientState;
import com.siirio.jemsettlements.client.SettlementPriceRenderer;
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
    private static boolean dragging, coins, received;
    private static long lastRequest;
    private SettlementMapClient() { }
    public static void opened() { draft = null; dragging = false; refresh(true); }
    public static void clear() { areas = List.of(); draft = null; dragging = false; received = false; lastRequest = 0; }
    public static void accept(CompoundTag view) {
        List<Area> rows = new ArrayList<>(); var tags = view.getList("MapAreas", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(256, tags.size()); i++) {
            var row = tags.getCompound(i); var dim = ResourceLocation.tryParse(row.getString("Dimension"));
            if (dim == null || !row.hasUUID("Id")) continue;
            rows.add(new Area(row.getUUID("Id"), row.getString("Name"), dim, BlockPos.of(row.getLong("Center")),
                    row.getBoolean("Owned"), row.getInt("MinX"), row.getInt("MinZ"), row.getInt("MaxX"), row.getInt("MaxZ")));
        }
        areas = List.copyOf(rows); coins = view.getBoolean("CoinCurrency"); received = true;
        if (draft != null && !areas.contains(draft.area())) { draft = null; dragging = false; }
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
    public static boolean begin(ResourceLocation dim, int x, int z) {
        if (!received || SettlementClientState.pending()) return false;
        Area area = area(dim, x, z, true); if (area == null) return false;
        draft = new Draft(area, x, z, TerritoryQuote.union(area.minX(), area.minZ(), area.maxX(), area.maxZ(), x, z, x, z));
        dragging = true; return true;
    }
    public static void update(ResourceLocation dim, int x, int z) {
        if (!dragging || draft == null || !draft.area().dimension().equals(dim)) return;
        Area a = draft.area();
        int tx = Math.max(a.center().getX() - 256, Math.min(a.center().getX() + 256, x));
        int tz = Math.max(a.center().getZ() - 256, Math.min(a.center().getZ() + 256, z));
        draft = new Draft(a, tx, tz, TerritoryQuote.union(a.minX(), a.minZ(), a.maxX(), a.maxZ(), tx, tz, tx, tz));
    }
    public static boolean finish() { boolean wasDragging = dragging; dragging = false; return wasDragging; }
    public static Draft pending(ResourceLocation dim) {
        return !dragging && draft != null && draft.area().dimension().equals(dim) && draft.quote().addedCopper() > 0L ? draft : null;
    }
    public static Component price(Draft value) { return SettlementPriceRenderer.format(value.quote().addedCopper(), coins); }
    public static void confirm(Draft value) {
        if (draft != value || SettlementClientState.pending()) return;
        var a = value.area(); CompoundTag payload = new CompoundTag(); payload.putInt("X", value.x()); payload.putInt("Z", value.z());
        payload.putInt("ExpectedMinX", a.minX()); payload.putInt("ExpectedMaxX", a.maxX());
        payload.putInt("ExpectedMinZ", a.minZ()); payload.putInt("ExpectedMaxZ", a.maxZ());
        if (SettlementClientState.action("mapexpand", a.id(), payload)) draft = null;
    }
    public static void render(GuiGraphics graphics, ResourceLocation dim, double cx, double cz, double scale,
                              int width, int height, int blockX, int blockZ, int mouseX, int mouseY) {
        refresh(false); if (!received) return;
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
