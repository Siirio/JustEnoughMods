package dev.taste.engine.light;

import dev.taste.engine.api.LightProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dirty-section light index. It never scans every loaded block per frame. */
public final class SectionLightIndex {
    private final int maximumSections;
    private final Map<Long, List<LightProvider.Light>> sections = new LinkedHashMap<>();
    public SectionLightIndex(int maximumSections) { this.maximumSections = Math.max(1, maximumSections); }
    public synchronized void replace(long sectionKey, List<LightProvider.Light> lights) {
        sections.remove(sectionKey);
        sections.put(sectionKey, List.copyOf(lights));
        while (sections.size() > maximumSections) sections.remove(sections.keySet().iterator().next());
    }
    public synchronized void remove(long sectionKey) { sections.remove(sectionKey); }
    public synchronized List<LightProvider.Light> near(double x, double y, double z, double radius) {
        double radiusSquared = radius * radius;
        List<LightProvider.Light> result = new ArrayList<>();
        for (List<LightProvider.Light> lights : sections.values()) for (LightProvider.Light light : lights) {
            double dx = light.x() - x, dy = light.y() - y, dz = light.z() - z;
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) result.add(light);
        }
        return result;
    }
    public synchronized int sectionCount() { return sections.size(); }
}
