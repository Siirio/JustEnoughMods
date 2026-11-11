package dev.taste.engine.api;

import java.nio.ByteBuffer;
import java.util.List;

@FunctionalInterface
public interface GeometryProvider {
    record Query(String dimension, long frame, double originX, double originY, double originZ) { }
    record Geometry(ByteBuffer vertices, int vertexCount, int stride, int primitive, long generation) {
        public Geometry { vertices = vertices.asReadOnlyBuffer(); }
    }
    List<Geometry> geometry(Query query);
}
