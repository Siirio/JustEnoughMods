package dev.taste.engine.material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Publishes one complete set of material rules to render and mesh workers. */
public final class MaterialResolver {
    @FunctionalInterface
    public interface Rule {
        Optional<MaterialDescription> resolve(MaterialQuery query);
    }

    private record Entry(int priority, Rule rule) { }
    private record RuleSet(List<MaterialRule> builtins, List<MaterialRule> packs, List<MaterialRule> user,
            List<MaterialRule> surfaceVariants) { }

    private static final Comparator<MaterialRule> ORDER = Comparator.comparingInt(MaterialRule::priority)
            .thenComparingInt(rule -> rule.match().specificity());
    private volatile RuleSet rules = new RuleSet(List.of(), List.of(), List.of(), List.of());
    private volatile List<Entry> providers = List.of();
    private volatile MaterialDescription fallback = MaterialDescription.fallback();

    public synchronized void add(int priority, Rule rule) {
        List<Entry> entries = new ArrayList<>(providers);
        entries.add(new Entry(priority, Objects.requireNonNull(rule)));
        entries.sort(Comparator.comparingInt(Entry::priority).reversed());
        providers = List.copyOf(entries);
    }

    public void fallback(MaterialDescription description) {
        fallback = Objects.requireNonNull(description);
    }

    public MaterialDescription resolve(MaterialQuery query) {
        RuleSet active = rules;
        MaterialDescription base = null;
        for (Entry entry : providers) {
            Optional<MaterialDescription> result = entry.rule().resolve(query);
            if (result.isPresent()) {
                base = result.get();
                break;
            }
        }
        EnumMap<MaterialRule.Field, Object> properties = new EnumMap<>(MaterialRule.Field.class);
        if (base == null) {
            base = fallback;
            collect(active.builtins(), query, properties);
        }
        collect(active.packs(), query, properties);
        collect(active.user(), query, properties);
        return MaterialRule.apply(base, properties, query.cutout());
    }

    public boolean mayClassifySurface(MaterialQuery coarse) {
        if (!providers.isEmpty()) return true;
        for (MaterialRule rule : rules.surfaceVariants()) {
            if (rule.match().matchesSubject(coarse)) return true;
        }
        return false;
    }

    void replace(List<MaterialRule> builtins, List<MaterialRule> packs, List<MaterialRule> user) {
        List<MaterialRule> surfaceVariants = Stream.of(builtins, packs, user).flatMap(List::stream)
                .filter(MaterialResolver::canIntroduceSurface).toList();
        rules = new RuleSet(ordered(builtins), ordered(packs), ordered(user), surfaceVariants);
    }

    public synchronized void clear() {
        replace(List.of(), List.of(), List.of());
        providers = List.of();
    }

    private static List<MaterialRule> ordered(List<MaterialRule> rules) {
        return rules.stream().sorted(ORDER).toList();
    }

    private static boolean canIntroduceSurface(MaterialRule rule) {
        if (rule.match().sprite() == null && rule.match().layer() == null) return false;
        if (rule.match().layer() == MaterialQuery.Layer.ENTITY || rule.match().layer() == MaterialQuery.Layer.PARTICLE) return false;
        Object emission = rule.properties().get(MaterialRule.Field.EMISSION);
        if (emission instanceof Float strength && strength > 0) return true;
        Object family = rule.properties().get(MaterialRule.Field.FAMILY);
        if (!(family instanceof MaterialDescription.Family value)) return false;
        return switch (value) {
            case WATER, GLASS, ICE, LAVA, PORTAL -> true;
            default -> false;
        };
    }

    private static void collect(List<MaterialRule> rules, MaterialQuery query, Map<MaterialRule.Field, Object> target) {
        for (MaterialRule rule : rules) {
            if (rule.match().matches(query)) target.putAll(rule.properties());
        }
    }
}
