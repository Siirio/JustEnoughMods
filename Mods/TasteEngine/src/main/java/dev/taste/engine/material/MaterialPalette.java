package dev.taste.engine.material;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/** Deterministic 16-bit material IDs. ID zero is the fallback material. */
public final class MaterialPalette {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int RESERVED_ID = 65_535;

    private final Map<MaterialDescription, Integer> ids = new LinkedHashMap<>();
    private final List<MaterialDescription> values = new ArrayList<>();
    private long generation;
    private boolean overflow;
    public MaterialPalette() { id(MaterialDescription.fallback()); }
    public synchronized int id(MaterialDescription material) {
        Integer existing = ids.get(material);
        if (existing != null) return existing;
        if (values.size() >= RESERVED_ID) {
            if (!overflow) LOGGER.warn("Taste material palette exhausted. New materials use the neutral material.");
            overflow = true;
            return 0;
        }
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
