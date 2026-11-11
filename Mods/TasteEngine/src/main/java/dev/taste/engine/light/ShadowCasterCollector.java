package dev.taste.engine.light;

import java.util.List;

@FunctionalInterface
public interface ShadowCasterCollector {
    record Query(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, long generation) { }
    record Caster(long sectionKey, int vertexBuffer, int vertexCount, long generation) { }
    List<Caster> collect(Query query);
}
