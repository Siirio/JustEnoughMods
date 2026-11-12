package dev.taste.engine.material;

import dev.taste.engine.material.MaterialDescription.Metadata;
import dev.taste.engine.material.MaterialDescription.TextureFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Finds companion images for a sprite using the active resource pack stack. */
public final class CompanionAtlas {
    private static final int CACHE_LIMIT = 4096;

    public record Companions(Optional<ResourceLocation> normal, Optional<ResourceLocation> specular,
            Optional<ResourceLocation> height) {
        public Companions(Optional<ResourceLocation> normal, Optional<ResourceLocation> specular) {
            this(normal, specular, Optional.empty());
        }
    }

    private record Lookup(ResourceLocation base, ResourceLocation normal, ResourceLocation specular,
            ResourceLocation height) { }

    private final Map<Lookup, Companions> cache = new LinkedHashMap<>(32, 0.75F, true);
    private ResourceManager currentResources;

    public synchronized Companions resolve(ResourceManager resources, ResourceLocation base) {
        return resolve(resources, new Lookup(image(base), null, null, null));
    }

    public synchronized Companions resolve(ResourceManager resources, ResourceLocation sprite,
            MaterialDescription material, TextureFormat configured, TextureFormat declared) {
        Metadata metadata = material.metadata();
        boolean pbr = metadata.effectiveTextureFormat(configured, declared) != TextureFormat.PROCEDURAL;
        return resolve(resources, new Lookup(pbr ? optionalImage(sprite) : null,
                optionalImage(metadata.normalMap()), optionalImage(metadata.specularMap()),
                optionalImage(metadata.heightMap())));
    }

    public synchronized void clear() {
        cache.clear();
        currentResources = null;
    }

    private Companions resolve(ResourceManager resources, Lookup lookup) {
        if (currentResources != resources) {
            cache.clear();
            currentResources = resources;
        }
        Companions result = cache.computeIfAbsent(lookup, key -> new Companions(
                find(resources, key.normal(), key.base(), "_n"),
                find(resources, key.specular(), key.base(), "_s"),
                find(resources, key.height(), null, "")));
        if (cache.size() > CACHE_LIMIT) cache.remove(cache.keySet().iterator().next());
        return result;
    }

    private static Optional<ResourceLocation> find(ResourceManager resources, ResourceLocation explicit,
            ResourceLocation base, String suffix) {
        ResourceLocation candidate = explicit;
        if (candidate == null && base != null) {
            String path = base.getPath();
            candidate = new ResourceLocation(base.getNamespace(), path.substring(0, path.length() - 4) + suffix + ".png");
        }
        return candidate != null && resources.getResource(candidate).isPresent() ? Optional.of(candidate) : Optional.empty();
    }

    private static ResourceLocation optionalImage(ResourceLocation name) {
        return name == null ? null : image(name);
    }

    private static ResourceLocation image(ResourceLocation name) {
        String path = name.getPath();
        if (!path.startsWith("textures/")) path = "textures/" + path;
        if (!path.endsWith(".png")) path += ".png";
        return new ResourceLocation(name.getNamespace(), path);
    }
}
