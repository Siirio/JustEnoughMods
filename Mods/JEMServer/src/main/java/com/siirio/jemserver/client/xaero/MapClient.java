package com.siirio.jemserver.client.xaero;

import com.siirio.jemserver.MapNetwork;
import com.siirio.jemserver.map.RegionGeometry;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = Dist.CLIENT)
public final class MapClient {
    private static List<MapNetwork.Death> deaths = List.of();
    private record Layer(ResourceLocation dimension, int color, RegionGeometry.Mesh mesh) {}
    private static List<MapNetwork.EventArea> events = List.of();
    private static List<Layer> eventLayers = List.of();
    private static List<MapNetwork.ProjectArea> projects = List.of();
    private static List<Layer> projectLayers = List.of();
    private static List<MapNetwork.SettlementArea> settlements = List.of();
    private static List<Layer> settlementLayers = List.of();
    private static UUID previewSettlement;
    private static boolean connected;
    private static final int BORDER_ALPHA = 0xFF000000;
    private static final int MAP_REFRESH_TICKS = 40;
    private static int refreshTicks;
    private MapClient() {}
    @SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!connected || !net.minecraftforge.fml.ModList.get().isLoaded("xaeroworldmap") || !(Minecraft.getInstance().screen instanceof xaero.map.gui.GuiMap)) { refreshTicks = 0; return; }
        if (++refreshTicks >= MAP_REFRESH_TICKS) { refreshTicks = 0; MapNetwork.request(); }
    }
    public static void accept(MapNetwork.Snapshot snapshot) {
        deaths = List.copyOf(snapshot.deaths()); acceptEvents(snapshot.events()); acceptProjects(snapshot.projects());
        settlements = List.copyOf(snapshot.settlements());
        settlementLayers = settlements.stream().map(town -> new Layer(town.dimension(), town.owned() ? com.siirio.jemserver.client.ui.JemPalette.PEACH : com.siirio.jemserver.client.ui.JemPalette.COPPER,
                RegionGeometry.union(List.of(new RegionGeometry.Rectangle(town.center().getX() - town.radius(), town.center().getZ() - town.radius(), town.center().getX() + town.radius() + 1, town.center().getZ() + town.radius() + 1))))).toList();
        connected = true;
    }
    private static void acceptProjects(List<MapNetwork.ProjectArea> areas) {
        if (projects.equals(areas)) return;
        projects = List.copyOf(areas);
        projectLayers = projects.stream().map(project -> new Layer(project.dimension(), project.color(),
                RegionGeometry.union(project.rectangles().stream().map(rectangle -> new RegionGeometry.Rectangle(
                        rectangle.minX(), rectangle.minZ(), rectangle.maxX(), rectangle.maxZ())).toList()))).toList();
    }
    public static void acceptEvents(List<MapNetwork.EventArea> areas) {
        if (events.equals(areas)) return;
        events = List.copyOf(areas);
        eventLayers = events.stream().map(area -> new Layer(area.dimension(), eventColor(area.activity()),
                RegionGeometry.union(List.of(new RegionGeometry.Rectangle(
                        area.minX(), area.minZ(), area.maxX() + 1, area.maxZ() + 1))))).toList();
    }
    private static int eventColor(String activity) {
        return switch (activity) {
            case "BLOOD_MOON" -> com.siirio.jemserver.client.ui.JemPalette.RED;
            case "BOSS_RAID" -> com.siirio.jemserver.client.ui.JemPalette.PEACH;
            case "RESOURCE_RUSH" -> com.siirio.jemserver.client.ui.JemPalette.GREEN;
            default -> com.siirio.jemserver.client.ui.JemPalette.COPPER;
        };
    }
    public static boolean open(ResourceLocation dimension, net.minecraft.core.BlockPos position) {
        return net.minecraftforge.fml.ModList.get().isLoaded("xaeroworldmap") && XaeroMapPreview.open(dimension, position);
    }
    public static boolean connected() { return connected; }
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { deaths = List.of(); events = List.of(); eventLayers = List.of(); projects = List.of(); projectLayers = List.of(); settlements = List.of(); settlementLayers = List.of(); previewSettlement = null; connected = false; ProjectRegionClient.cancel(); }
    public static boolean previewing(UUID id) { return id != null && id.equals(previewSettlement); }
    public static void preview(UUID id) { previewSettlement = id; }
    public static void clearSettlementPreview() { previewSettlement = null; }
    public static MapNetwork.SettlementArea settlement(ResourceLocation dimension, int x, int z, boolean ownedOnly) {
        return settlements.stream().filter(town -> (!ownedOnly || town.owned()) && town.dimension().equals(dimension)
                && Math.abs(x - town.center().getX()) <= town.radius() && Math.abs(z - town.center().getZ()) <= town.radius()).findFirst().orElse(null);
    }
    public static UUID death(ResourceLocation dimension, int x, int y, int z) {
        return deaths.stream().filter(death -> death.dimension().equals(dimension) && death.position().getX() == x && death.position().getY() == y && death.position().getZ() == z).map(MapNetwork.Death::id).findFirst().orElse(null);
    }
    public static MapNetwork.EventArea event(ResourceLocation dimension, int x, int z) {
        return events.stream().filter(area -> area.dimension().equals(dimension)
                        && x >= area.minX() && x <= area.maxX()
                        && z >= area.minZ() && z <= area.maxZ())
                .min(java.util.Comparator.comparingLong(area -> {
                    long dx = x - area.position().getX();
                    long dz = z - area.position().getZ();
                    return dx * dx + dz * dz;
                }))
                .orElse(null);
    }
    public static MapNetwork.ProjectArea project(ResourceLocation dimension, int x, int z) {
        return projects.stream().filter(project -> project.dimension().equals(dimension) && project.rectangles().stream().anyMatch(rectangle ->
                x >= rectangle.minX() && x < rectangle.maxX() && z >= rectangle.minZ() && z < rectangle.maxZ())).findFirst().orElse(null);
    }
    public static void render(GuiGraphics graphics, ResourceLocation dimension, double cameraX, double cameraZ, double scale, int width, int height,
                              int hoverX, int hoverZ, int mouseX, int mouseY) {
        if (!connected) return;
        renderLayers(graphics, dimension, cameraX, cameraZ, scale, width, height, settlementLayers);
        settlements.stream().filter(town -> previewing(town.id()) && town.dimension().equals(dimension) && town.radius() < 256).findFirst().ifPresent(town -> {
            int radius = town.radius() + 16;
            var layer = new Layer(town.dimension(), com.siirio.jemserver.client.ui.JemPalette.GREEN,
                    RegionGeometry.union(List.of(new RegionGeometry.Rectangle(town.center().getX() - radius, town.center().getZ() - radius,
                            town.center().getX() + radius + 1, town.center().getZ() + radius + 1))));
            renderLayers(graphics, dimension, cameraX, cameraZ, scale, width, height, List.of(layer));
        });
        var font = Minecraft.getInstance().font;
        var settlement = settlement(dimension, hoverX, hoverZ, false);
        net.minecraft.network.chat.Component label = settlement == null ? null : net.minecraft.network.chat.Component.literal(settlement.name());
        if (label != null && mouseX >= 0 && mouseX < width && mouseY >= 0 && mouseY < height) {
            var fitted = font.substrByWidth(label, Math.max(1, width - 8));
            int x = Math.max(4, Math.min(width - font.width(fitted) - 4, mouseX + 8));
            int y = Math.max(4, Math.min(height - font.lineHeight - 4, mouseY + 12));
            graphics.drawString(font, fitted.getString(), x, y, com.siirio.jemserver.client.ui.JemPalette.CREAM, true);
        }
    }
    private static void renderLayers(GuiGraphics graphics, ResourceLocation dimension, double cameraX, double cameraZ, double scale, int width, int height, List<Layer> source) {
        for (var layer : source) {
            if (!layer.dimension().equals(dimension)) continue;
            int color = layer.color() & 0xFFFFFF;
            for (var edge : layer.mesh().edges()) {
                int x1 = (int) Math.floor(width / 2.0 + (edge.x1() - cameraX) * scale);
                int z1 = (int) Math.floor(height / 2.0 + (edge.z1() - cameraZ) * scale);
                int x2 = (int) Math.floor(width / 2.0 + (edge.x2() - cameraX) * scale);
                int z2 = (int) Math.floor(height / 2.0 + (edge.z2() - cameraZ) * scale);
                if (x2 >= 0 && z2 >= 0 && x1 <= width && z1 <= height)
                    graphics.fill(Math.max(0, x1), Math.max(0, z1), Math.min(width, x2 + 1), Math.min(height, z2 + 1), BORDER_ALPHA | color);
            }
        }
    }
}

