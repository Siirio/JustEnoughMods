package dev.taste.engine.light;

import dev.taste.engine.api.LightProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DynamicLightCollector {
    private final List<LightProvider> providers = new ArrayList<>();
    public void register(LightProvider provider) { providers.add(provider); }
    public List<LightProvider.Light> collect(LightProvider.Query query, int limit) {
        List<LightProvider.Light> lights = new ArrayList<>();
        for (LightProvider provider : providers) {
            List<LightProvider.Light> supplied = provider.lights(query);
            if (supplied != null) lights.addAll(supplied);
        }
        return lights.stream().filter(light -> light.intensity() > 0.0F && light.range() > 0.0F)
                .sorted(Comparator.comparingDouble((LightProvider.Light light) -> influence(light, query)).reversed())
                .limit(Math.max(0, limit)).toList();
    }
    private static double influence(LightProvider.Light light, LightProvider.Query query) {
        double dx = light.x() - query.cameraX(), dy = light.y() - query.cameraY(), dz = light.z() - query.cameraZ();
        return light.intensity() * light.range() * light.range() / Math.max(1.0, dx * dx + dy * dy + dz * dz);
    }
}
