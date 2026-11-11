package dev.taste.engine.api.instances;

import java.nio.ByteBuffer;

public record PoseGeometrySnapshot(long instanceId, long frame, ByteBuffer vertices, int vertexCount,
        float[] currentTransform, float[] previousTransform) {
    public PoseGeometrySnapshot {
        vertices = vertices.asReadOnlyBuffer();
        currentTransform = currentTransform.clone();
        previousTransform = previousTransform.clone();
    }
}
