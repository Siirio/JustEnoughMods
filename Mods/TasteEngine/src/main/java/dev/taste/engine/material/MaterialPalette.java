package dev.taste.engine.material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic 16-bit material IDs. ID zero is the fallback material. */
public final class MaterialPalette {
    private final Map<MaterialDescription, Integer> ids = new LinkedHashMap<>();
    private final List<MaterialDescription> values = new ArrayList<>();
    private long generation;
    public MaterialPalette() { id(MaterialDescription.fallback()); }
    public synchronized int id(MaterialDescription material) {
        Integer existing = ids.get(material);
        if (existing != null) return existing;
        if (values.size() >= 65_536) throw new IllegalStateException("Material palette exhausted");
        int id = values.size();
        ids.put(material, id);
        values.add(material);
        generation++;
        return id;
    }
    public synchronized MaterialDescription material(int id) { return values.get(id); }
    public synchronized List<MaterialDescription> snapshot() { return List.copyOf(values); }
    public synchronized long generation() { return generation; }
}
