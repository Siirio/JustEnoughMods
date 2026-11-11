package dev.taste.engine.effects;

import dev.taste.engine.material.MaterialDescription;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityMaterialBridge {
    private final Map<String, MaterialDescription> materials = new LinkedHashMap<>();
    public void register(String rendererId, MaterialDescription material) { materials.put(rendererId, material); }
    public MaterialDescription material(String rendererId) { return materials.getOrDefault(rendererId, MaterialDescription.fallback()); }
    public boolean known(String rendererId) { return materials.containsKey(rendererId); }
}
