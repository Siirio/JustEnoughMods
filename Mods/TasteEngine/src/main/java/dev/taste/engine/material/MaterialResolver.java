package dev.taste.engine.material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Priority ordered rules. First exact match wins and fallback is explicit. */
public final class MaterialResolver {
    @FunctionalInterface public interface Rule { Optional<MaterialDescription> resolve(MaterialQuery query); }
    private record Entry(int priority, Rule rule) { }
    private final List<Entry> rules = new ArrayList<>();
    private MaterialDescription fallback = MaterialDescription.fallback();

    public synchronized void add(int priority, Rule rule) {
        rules.add(new Entry(priority, rule));
        rules.sort(Comparator.comparingInt(Entry::priority).reversed());
    }
    public synchronized void fallback(MaterialDescription description) { fallback = description; }
    public synchronized MaterialDescription resolve(MaterialQuery query) {
        for (Entry entry : rules) {
            Optional<MaterialDescription> result = entry.rule().resolve(query);
            if (result.isPresent()) return result.get();
        }
        return fallback;
    }
    public synchronized void clear() { rules.clear(); }
}
