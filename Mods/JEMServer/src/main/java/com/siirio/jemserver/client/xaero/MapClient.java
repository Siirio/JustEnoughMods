package com.siirio.jemserver.client.xaero;

import com.siirio.jemserver.MapNetwork;
import com.siirio.jemserver.ClaimBoundaryGeometry;
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
    private static List<MapNetwork.Claim> claims = List.of();
    private static List<MapNetwork.Death> deaths = List.of();
    private record Layer(ResourceLocation dimension, int color, ClaimBoundaryGeometry.Mesh mesh) {}
    private static List<Layer> layers = List.of();
    private static List<MapNetwork.EventArea> events = List.of();
    private static List<Layer> eventLayers = List.of();
    private static boolean connected;
    private static final int FILL_ALPHA = 0x38000000;
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
        if (!claims.equals(snapshot.claims())) {
            claims = List.copyOf(snapshot.claims());
            record Group(UUID owner, ResourceLocation dimension) {}
            layers = claims.stream().collect(java.util.stream.Collectors.groupingBy(claim -> new Group(claim.owner(), claim.dimension())))
                    .values().stream().map(group -> new Layer(group.get(0).dimension(), group.get(0).color(),
                            ClaimBoundaryGeometry.union(group.stream().map(claim -> new ClaimBoundaryGeometry.Rectangle(claim.minX(), claim.minZ(), claim.maxX() + 1, claim.maxZ() + 1)).toList()))).toList();
        }
        deaths = List.copyOf(snapshot.deaths()); acceptEvents(snapshot.events()); connected = true;
    }
    public static void acceptEvents(List<MapNetwork.EventArea> areas) {
        if (events.equals(areas)) return;
        events = List.copyOf(areas);
        eventLayers = events.stream().map(area -> new Layer(area.dimension(), eventColor(area.activity()),
                ClaimBoundaryGeometry.union(List.of(new ClaimBoundaryGeometry.Rectangle(
                        area.position().getX() - area.radius(), area.position().getZ() - area.radius(),
                        area.position().getX() + area.radius() + 1, area.position().getZ() + area.radius() + 1))))).toList();
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
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { claims = List.of(); deaths = List.of(); layers = List.of(); events = List.of(); eventLayers = List.of(); connected = false; }
    public static UUID death(ResourceLocation dimension, int x, int y, int z) {
        return deaths.stream().filter(death -> death.dimension().equals(dimension) && death.position().getX() == x && death.position().getY() == y && death.position().getZ() == z).map(MapNetwork.Death::id).findFirst().orElse(null);
    }
    public static MapNetwork.Claim owned(ResourceLocation dimension, int x, int z) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        return claims.stream().filter(claim -> claim.owner().equals(player.getUUID()) && claim.dimension().equals(dimension) && x >= claim.minX() && x <= claim.maxX() && z >= claim.minZ() && z <= claim.maxZ()).findFirst().orElse(null);
    }
    public static MapNetwork.EventArea event(ResourceLocation dimension, int x, int z) {
        return events.stream().filter(area -> area.dimension().equals(dimension)
                        && Math.abs(x - area.position().getX()) <= area.radius()
                        && Math.abs(z - area.position().getZ()) <= area.radius())
                .min(java.util.Comparator.comparingLong(area -> {
                    long dx = x - area.position().getX();
                    long dz = z - area.position().getZ();
                    return dx * dx + dz * dz;
                }))
                .orElse(null);
    }
    public static void render(GuiGraphics graphics, ResourceLocation dimension, double cameraX, double cameraZ, double scale, int width, int height) {
        if (!connected) return;
        renderLayers(graphics, dimension, cameraX, cameraZ, scale, width, height, layers);
        renderLayers(graphics, dimension, cameraX, cameraZ, scale, width, height, eventLayers);
        var font = Minecraft.getInstance().font;
        for (var area : events) {
            if (!area.dimension().equals(dimension)) continue;
            int x = (int) Math.floor(width / 2.0 + (area.position().getX() - cameraX) * scale);
            int y = (int) Math.floor(height / 2.0 + (area.position().getZ() - cameraZ) * scale);
            if (x < 0 || x >= width || y < 0 || y >= height) continue;
            var label = net.minecraft.network.chat.Component.translatable("jem.smp." + area.activity());
            var fitted = font.substrByWidth(label, width);
            graphics.drawString(font, fitted.getString(), Math.max(0, Math.min(width - font.width(fitted), x - font.width(fitted) / 2)),
                    Math.max(0, Math.min(height - font.lineHeight, y)), com.siirio.jemserver.client.ui.JemPalette.CREAM, true);
        }
    }
    private static void renderLayers(GuiGraphics graphics, ResourceLocation dimension, double cameraX, double cameraZ, double scale, int width, int height, List<Layer> source) {
        for (var layer : source) {
            if (!layer.dimension().equals(dimension)) continue;
            int color = layer.color() & 0xFFFFFF;
            for (var rect : layer.mesh().fills()) {
                int left = (int) Math.floor(width / 2.0 + (rect.minX() - cameraX) * scale);
                int top = (int) Math.floor(height / 2.0 + (rect.minZ() - cameraZ) * scale);
                int right = (int) Math.floor(width / 2.0 + (rect.maxX() - cameraX) * scale);
                int bottom = (int) Math.floor(height / 2.0 + (rect.maxZ() - cameraZ) * scale);
                if (right >= 0 && bottom >= 0 && left <= width && top <= height)
                    graphics.fill(Math.max(0, left), Math.max(0, top), Math.min(width, right), Math.min(height, bottom), FILL_ALPHA | color);
            }
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

