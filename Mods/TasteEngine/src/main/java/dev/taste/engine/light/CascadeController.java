package dev.taste.engine.light;

import dev.taste.engine.render.FrameSnapshot;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class CascadeController {
    public record Cascade(float near, float far, float texelWorldSize) { }
    public record LightView(float far, float depthSpan, Matrix4f projection) { }
    public Cascade[] splits(float near, float far, int count, float lambda, int resolution) {
        if (near <= 0.0F || far <= near || count < 1 || count > 4 || resolution < 1) throw new IllegalArgumentException("Invalid cascade setup");
        Cascade[] cascades = new Cascade[count];
        float previous = near;
        for (int i = 1; i <= count; i++) {
            float ratio = i / (float) count;
            float logarithmic = near * (float) Math.pow(far / near, ratio);
            float uniform = near + (far - near) * ratio;
            float split = logarithmic * lambda + uniform * (1.0F - lambda);
            cascades[i - 1] = new Cascade(previous, split, (split - previous) * 2.0F / resolution);
            previous = split;
        }
        return cascades;
    }
    public float stabilize(float coordinate, float texelWorldSize) { return Math.round(coordinate / texelWorldSize) * texelWorldSize; }

    public LightView fit(Cascade cascade, FrameSnapshot frame, Vector3f towardsSun, int resolution) {
        Matrix4f inverseProjection = new Matrix4f().set(frame.unjitteredProjection().columnMajor()).invert();
        Matrix4f inverseView = new Matrix4f().set(frame.view().columnMajor()).invert();
        Vector3f[] corners = new Vector3f[8];
        Vector3f center = new Vector3f();
        int index = 0;
        for (float z : new float[]{cascade.near(), cascade.far()}) {
            for (int y : new int[]{-1, 1}) {
                for (int x : new int[]{-1, 1}) {
                    Vector4f ray = inverseProjection.transform(new Vector4f(x, y, 1, 1));
                    float scale = -z / ray.z;
                    Vector3f corner = new Vector3f(ray.x * scale, ray.y * scale, -z);
                    inverseView.transformPosition(corner);
                    corners[index++] = corner;
                    center.add(corner);
                }
            }
        }
        center.mul(1.0F / corners.length);
        float radius = 0;
        for (Vector3f corner : corners) radius = Math.max(radius, center.distance(corner));
        radius = (float) Math.ceil(radius * 16) / 16;
        Vector3f up = Math.abs(towardsSun.y) > 0.98F ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);
        Matrix4f basis = new Matrix4f().lookAlong(new Vector3f(towardsSun).negate(), up);
        Vector3f lightCenter = basis.transformPosition(new Vector3f(center));
        float texelSize = 2 * radius / (resolution - 2);
        float halfExtent = texelSize * resolution / 2;
        FrameSnapshot.Vec3d origin = frame.origin();
        double originX = basis.m00() * origin.x() + basis.m10() * origin.y() + basis.m20() * origin.z();
        double originY = basis.m01() * origin.x() + basis.m11() * origin.y() + basis.m21() * origin.z();
        double originZ = basis.m02() * origin.x() + basis.m12() * origin.y() + basis.m22() * origin.z();
        float x = snapRelative(lightCenter.x, originX, texelSize);
        float y = snapRelative(lightCenter.y, originY, texelSize);
        float z = snapRelative(lightCenter.z, originZ, texelSize);
        float lightwardReach = Math.max(96.0F, cascade.far());
        float depthSpan = halfExtent * 2 + lightwardReach;
        Matrix4f projection = new Matrix4f().ortho(x - halfExtent, x + halfExtent,
                y - halfExtent, y + halfExtent,
                -z - halfExtent - lightwardReach, -z + halfExtent).mul(basis);
        return new LightView(cascade.far(), depthSpan, projection);
    }

    private static float snapRelative(float coordinate, double origin, float texelSize) {
        double remainder = origin % texelSize;
        return (float) (Math.round((coordinate + remainder) / texelSize) * (double) texelSize - remainder);
    }
}
