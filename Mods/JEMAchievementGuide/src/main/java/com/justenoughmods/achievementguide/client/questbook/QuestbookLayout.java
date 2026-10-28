package com.justenoughmods.achievementguide.client.questbook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Immutable client geometry, never a source of progression or input hitboxes. */
@Mod.EventBusSubscriber(modid = "jem_advancements", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class QuestbookLayout {
    private static final ResourceLocation SOURCE = new ResourceLocation("jem_advancements", "questbook/layout.json");
    private static Map<Long, Board> boards = Map.of();
    private static int revision;

    private QuestbookLayout() {}

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) QuestbookLayout::reload);
    }

    private static void reload(ResourceManager manager) {
        Map<Long, Board> next = new LinkedHashMap<>();
        var resource = manager.getResource(SOURCE);
        if (resource.isPresent()) {
            try (var reader = resource.get().openAsReader()) {
                for (var entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                    JsonObject value = entry.getValue().getAsJsonObject();
                    var bounds = value.getAsJsonArray("bounds");
                    List<Connection> connections = new ArrayList<>();
                    for (var element : value.getAsJsonArray("connections")) {
                        JsonObject connection = element.getAsJsonObject();
                        List<Point> points = new ArrayList<>();
                        for (var point : connection.getAsJsonArray("points")) {
                            var pair = point.getAsJsonArray();
                            points.add(new Point(pair.get(0).getAsDouble(), pair.get(1).getAsDouble()));
                        }
                        int rgb = Integer.parseInt(connection.get("color").getAsString().substring(1), 16);
                        int alpha = (int)Math.round(connection.get("opacity").getAsDouble() * 255);
                        var dash = connection.getAsJsonArray("dash_gui");
                        connections.add(new Connection(List.copyOf(points), alpha << 24 | rgb,
                                connection.get("width_gui").getAsFloat(), dash.get(0).getAsDouble(), dash.get(1).getAsDouble()));
                    }
                    next.put(Long.parseUnsignedLong(entry.getKey(), 16), new Board(
                            bounds.get(0).getAsDouble(), bounds.get(1).getAsDouble(),
                            bounds.get(2).getAsDouble(), bounds.get(3).getAsDouble(), List.copyOf(connections)));
                }
            } catch (Exception exception) {
                LogUtils.getLogger().error("Invalid authored questbook layout, retaining native presentation", exception);
                next.clear();
            }
        }
        boards = Map.copyOf(next);
        revision++;
    }

    public static Board board(long chapter) { return boards.get(chapter); }
    public static int revision() { return revision; }
    public record Point(double x, double y) {}
    public record Connection(List<Point> points, int color, float width, double dash, double gap) {}
    public record Board(double minX, double minY, double maxX, double maxY, List<Connection> connections) {}
}
