package dev.taste.engine.client.ui;

import dev.taste.engine.config.SettingRegistry;
import dev.taste.engine.config.SettingSpec;
import java.util.List;
import java.util.Locale;

public final class SearchIndex {
    private final SettingRegistry registry;
    public SearchIndex(SettingRegistry registry) { this.registry = registry; }
    public List<SettingSpec> search(String query, String category) {
        String[] terms = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return registry.settings().values().stream().filter(spec -> category == null || category.isBlank() || category.equals(spec.category()))
                .filter(spec -> { String text=(spec.id()+" "+spec.label("en_us")+" "+spec.label("ru_ru")+" "+spec.description()).toLowerCase(Locale.ROOT); for(String term:terms) if(!term.isBlank()&&!text.contains(term)) return false; return true; }).toList();
    }
}
