package com.siirio.jemserver.client.xaero;

import com.siirio.jemserver.MapNetwork;
import com.siirio.jemserver.map.RegionGeometry;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = Dist.CLIENT)
public final class MapClient {
    private static List<MapNetwork.Death> deaths = List.of();
    private record Layer(ResourceLocation dimension, int color, int fillAlpha, RegionGeometry.Mesh mesh) {}
    private static List<MapNetwork.EventArea> events = List.of();
    private static List<Layer> eventLayers = List.of();
    private static List<MapNetwork.ProjectArea> projects = List.of();
    private static List<Layer> projectLayers = List.of();
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
        connected = true;
    }
    private static void acceptProjects(List<MapNetwork.ProjectArea> areas) {
        if (projects.equals(areas)) return;
        projects = List.copyOf(areas);
        projectLayers = projects.stream().map(project -> new Layer(project.dimension(), project.color(), 0,
                RegionGeometry.union(project.rectangles().stream().map(rectangle -> new RegionGeometry.Rectangle(
                        rectangle.minX(), rectangle.minZ(), rectangle.maxX(), rectangle.maxZ())).toList()))).toList();
    }
    public static void acceptEvents(List<MapNetwork.EventArea> areas) {
        if (events.equals(areas)) return;
        events = List.copyOf(areas);
        if (net.minecraftforge.fml.ModList.get().isLoaded("xaerominimap")) com.siirio.jemserver.client.XaeroLandmarks.acceptEvents(events);
        eventLayers = events.stream().map(area -> new Layer(area.dimension(), eventColor(area.activity()), 0,
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
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { deaths = List.of(); events = List.of(); eventLayers = List.of(); projects = List.of(); projectLayers = List.of(); connected = false; ProjectRegionClient.cancel(); }
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
}
