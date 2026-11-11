package dev.taste.engine.render;

import java.util.Arrays;
import java.util.Objects;

/** Original foundation: immutable data only; no Minecraft, GL, or worker-owned objects. */
public record FrameSnapshot(long frameIndex, long worldEpoch, String dimension,
        Vec3d origin, Vec3d cameraWorld, Matrix4 view, Matrix4 projection,
        Matrix4 inverseProjection, Matrix4 unjitteredProjection,
        int width, int height, double timeSeconds) {
    public FrameSnapshot {
        if (frameIndex < 0 || width < 1 || height < 1 || !Double.isFinite(timeSeconds))
            throw new IllegalArgumentException("Invalid frame dimensions/time/index");
        Objects.requireNonNull(dimension);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(cameraWorld);
        Objects.requireNonNull(view);
        Objects.requireNonNull(projection);
        Objects.requireNonNull(inverseProjection);
        Objects.requireNonNull(unjitteredProjection);
    }

    public record Vec3d(double x, double y, double z) {
        public Vec3d {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Non-finite world coordinate");
        }
        /** Subtract in double precision BEFORE converting to the GPU float domain. */
        public float[] relativeTo(Vec3d origin) {
            float[] out = {(float) (x - origin.x), (float) (y - origin.y), (float) (z - origin.z)};
            for (float v : out) if (!Float.isFinite(v)) throw new IllegalArgumentException("Relative overflow");
            return out;
        }
    }

    /** Column-major; view consumes origin-relative coordinates. Never exposes its backing array. */
    public static final class Matrix4 {
        private final float[] elements;
        public Matrix4(float[] elements) {
            if (elements.length != 16) throw new IllegalArgumentException("Expected 16 matrix entries");
            this.elements = elements.clone();
            for (float v : this.elements) if (!Float.isFinite(v)) throw new IllegalArgumentException("Non-finite matrix");
        }
        public float[] columnMajor() { return elements.clone(); }
        @Override public boolean equals(Object o) {
            return o instanceof Matrix4 m && Arrays.equals(elements, m.elements);
        }
        @Override public int hashCode() { return Arrays.hashCode(elements); }
    }
}
