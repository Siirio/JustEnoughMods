package dev.taste.engine.render.forward;

import dev.taste.engine.api.MaterialProvider;
import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ForwardMaterialRenderer {
    private record Entry(int priority, MaterialProvider provider) { }
    private final List<Entry> providers = new ArrayList<>();
    public void register(int priority, MaterialProvider provider) {
        providers.add(new Entry(priority, provider));
        providers.sort(Comparator.comparingInt(Entry::priority).reversed());
    }
    public MaterialDescription resolve(MaterialQuery query) {
        for (Entry entry : providers) {
            Optional<MaterialDescription> material = entry.provider().material(query);
            if (material.isPresent()) return material.get();
        }
        return MaterialDescription.fallback();
    }
}
