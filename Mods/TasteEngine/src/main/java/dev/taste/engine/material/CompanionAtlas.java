package dev.taste.engine.material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Resolves LabPBR normal/specular companions without replacing the owning sprite. */
public final class CompanionAtlas {
    public record Companions(Optional<ResourceLocation> normal, Optional<ResourceLocation> specular) { }
    private final Map<ResourceLocation, Companions> cache = new LinkedHashMap<>();
    public synchronized Companions resolve(ResourceManager resources, ResourceLocation base) {
        return cache.computeIfAbsent(base, key -> new Companions(find(resources, key, "_n"), find(resources, key, "_s")));
    }
    public synchronized void clear() { cache.clear(); }
    private static Optional<ResourceLocation> find(ResourceManager resources, ResourceLocation base, String suffix) {
        String path = base.getPath();
        int dot = path.lastIndexOf('.');
        String candidatePath = dot < 0 ? path + suffix : path.substring(0, dot) + suffix + path.substring(dot);
        ResourceLocation candidate = new ResourceLocation(base.getNamespace(), candidatePath);
        return resources.getResource(candidate).isPresent() ? Optional.of(candidate) : Optional.empty();
    }
}
