package com.siirio.jemsecurity.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

public final class ResourcePackGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_JSON_BYTES = 64 * 1024;
    private static final int XRAY_MODEL_THRESHOLD = 8;
    private static final String[] VALUABLE_MODELS = {
            "models/block/coal_ore.json",
            "models/block/iron_ore.json",
            "models/block/gold_ore.json",
            "models/block/diamond_ore.json",
            "models/block/emerald_ore.json",
            "models/block/lapis_ore.json",
            "models/block/redstone_ore.json",
            "models/block/deepslate_diamond_ore.json",
            "models/block/nether_gold_ore.json",
            "models/block/nether_quartz_ore.json"
    };
    private static final Map<Pack, Boolean> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private ResourcePackGuard() { }

    public static String findForbidden(Stream<PackSelectionModel.Entry> selected) {
        var repository = Minecraft.getInstance().getResourcePackRepository();
        return findForbiddenPacks(selected.map(PackSelectionModel.Entry::getId)
                .filter(id -> id.startsWith("file/"))
                .map(repository::getPack));
    }

    public static String findForbiddenPacks(Stream<Pack> packs) {
        return packs.filter(pack -> pack != null && pack.getId().startsWith("file/"))
                .filter(pack -> CACHE.computeIfAbsent(pack, ResourcePackGuard::inspect))
                .map(Pack::getId)
                .findFirst()
                .orElse(null);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static boolean inspect(Pack pack) {
        try (PackResources resources = pack.open()) {
            return hasXrayModelTree(resources) || hasSelectiveGeometryOverride(resources);
        } catch (Exception exception) {
            LOGGER.warn("JEM Security could not inspect resource pack {}", pack.getId(), exception);
            return false;
        }
    }

    private static boolean hasXrayModelTree(PackResources resources) {
        int[] matches = {0};
        for (String namespace : resources.getNamespaces(PackType.CLIENT_RESOURCES)) {
            resources.listResources(PackType.CLIENT_RESOURCES, namespace, "models/block", (location, supplier) -> {
                String path = location.getPath();
                if (path.startsWith("models/block/xray/") || path.contains("/models/block/xray/")) matches[0]++;
            });
            if (matches[0] >= XRAY_MODEL_THRESHOLD) return true;
        }
        return false;
    }

    private static boolean hasSelectiveGeometryOverride(PackResources resources) throws IOException {
        String base = read(resources, "models/block/block.json");
        String solid = read(resources, "models/block/solid.json");
        if (base == null || solid == null || base.contains("\"elements\"") || !solid.contains("\"elements\"")) return false;

        int valuableSolidModels = 0;
        for (String path : VALUABLE_MODELS) {
            String model = read(resources, path);
            if (model != null && model.contains("solid")) valuableSolidModels++;
            if (valuableSolidModels >= 3) return true;
        }
        return false;
    }

    private static String read(PackResources resources, String path) throws IOException {
        IoSupplier<InputStream> supplier = resources.getResource(
                PackType.CLIENT_RESOURCES, new ResourceLocation("minecraft", path));
        if (supplier == null) return null;
        try (InputStream input = supplier.get()) {
            byte[] bytes = input.readNBytes(MAX_JSON_BYTES + 1);
            if (bytes.length > MAX_JSON_BYTES) return null;
            return new String(bytes, StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
        }
    }
}
