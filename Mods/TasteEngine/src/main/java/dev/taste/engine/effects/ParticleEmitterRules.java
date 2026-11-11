package dev.taste.engine.effects;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ParticleEmitterRules {
    public record Rule(int maximum, float distance, float spawnRate, boolean lit, boolean soft, boolean additive) { }
    private final Map<String, Rule> rules = new LinkedHashMap<>();
    public void register(String id, Rule rule) { rules.put(id, rule); }
    public Rule rule(String id) { return rules.get(id); }
    public Map<String, Rule> snapshot() { return Map.copyOf(rules); }
}
