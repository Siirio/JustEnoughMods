package com.siirio.jemserver.smp.events;

import java.util.*;

final class CookingRecipeGraph {
    private static final int MAX_DEPTH = 16;
    private static final int STAGE_WEIGHT = 2;
    private static final int DEPTH_WEIGHT = 2;
    private static final int STATION_WEIGHT = 3;
    private static final int DIMENSION_WEIGHT = 6;
    record Ingredient(int rarity, int dimensions, boolean obtainable) {}
    record Step(String id, String station, List<List<String>> ingredients) {}
    record Cost(Set<String> ingredients, Set<String> stages, Set<String> stations, int depth, int rarity, int dimensions) {
        int score() {
            return ingredients.size() + stages.size() * STAGE_WEIGHT + depth * DEPTH_WEIGHT
                    + stations.size() * STATION_WEIGHT + rarity + dimensions * DIMENSION_WEIGHT;
        }
    }
    private final Map<String, Cost> dishes = new HashMap<>();

    CookingRecipeGraph(Map<String, Ingredient> ingredients, Map<String, List<Step>> recipes, int unlocked) {
        Map<String, Cost> available = new HashMap<>();
        for (var entry : ingredients.entrySet()) {
            String item = entry.getKey();
            Ingredient source = entry.getValue();
            if ((source.obtainable() || !recipes.containsKey(item)) && (source.dimensions() & ~unlocked) == 0)
                available.put(item, new Cost(Set.of(item), Set.of(), Set.of(), 0, source.rarity(), Integer.bitCount(source.dimensions())));
        }
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            var next = new HashMap<>(available);
            boolean changed = false;
            for (var entry : recipes.entrySet()) {
                for (var step : entry.getValue()) {
                    Cost cost = evaluate(step, available);
                    if (cost == null) continue;
                    if (better(cost, dishes.get(entry.getKey()))) dishes.put(entry.getKey(), cost);
                    if (better(cost, next.get(entry.getKey()))) {
                        next.put(entry.getKey(), cost);
                        changed = true;
                    }
                }
            }
            if (!changed) break;
            available = next;
        }
    }

    Cost dish(String item) {
        return dishes.get(item);
    }

    private static boolean better(Cost candidate, Cost current) {
        if (current == null) return true;
        int comparison = Integer.compare(candidate.score(), current.score());
        if (comparison == 0) comparison = Integer.compare(candidate.depth(), current.depth());
        if (comparison == 0) comparison = compare(candidate.ingredients(), current.ingredients());
        if (comparison == 0) comparison = compare(candidate.stages(), current.stages());
        if (comparison == 0) comparison = compare(candidate.stations(), current.stations());
        return comparison < 0;
    }

    private static int compare(Set<String> left, Set<String> right) {
        if (left.equals(right)) return 0;
        var a = new TreeSet<>(left).iterator();
        var b = new TreeSet<>(right).iterator();
        while (a.hasNext() && b.hasNext()) {
            int comparison = a.next().compareTo(b.next());
            if (comparison != 0) return comparison;
        }
        return Boolean.compare(a.hasNext(), b.hasNext());
    }

    private static Cost evaluate(Step step, Map<String, Cost> available) {
        if (step.ingredients().isEmpty()) return null;
        var roots = new HashSet<String>();
        var stages = new HashSet<String>();
        var stations = new HashSet<String>();
        int nested = 0, rarity = 0, dimensions = 0;
        for (var alternatives : step.ingredients()) {
            Cost choice = null;
            for (var candidate : alternatives) {
                Cost cost = available.get(candidate);
                if (cost != null && better(cost, choice)) choice = cost;
            }
            if (choice == null) return null;
            roots.addAll(choice.ingredients());
            stages.addAll(choice.stages());
            stations.addAll(choice.stations());
            nested = Math.max(nested, choice.depth());
            rarity += choice.rarity();
            dimensions += choice.dimensions();
        }
        stages.add(step.id());
        if (!step.station().equals("minecraft:crafting")) stations.add(step.station());
        return new Cost(roots, stages, stations, nested + 1, rarity, dimensions);
    }
}
