package dev.taste.engine.render.reflection;

import dev.taste.engine.render.FrameSnapshot;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Chooses one visible still-water elevation from bounded resident samples. */
final class WaterPlane {
    private record Candidate(double height, float score, int samples) {
        Candidate add(float weight) { return new Candidate(height, score + weight, samples + 1); }
    }

    private static final int RADIUS = 24;
    private static final int STRIDE = 4;
    private static final int REFRESH_FRAMES = 10;
    private double current = Double.NaN;
    private long lastScan = -REFRESH_FRAMES;
    private FrameSnapshot.Vec3d scanOrigin;
    private boolean dirty = true;

    double select(ClientLevel level, FrameSnapshot frame) {
        var eye = frame.cameraWorld();
        if (!dirty && frame.frameIndex() - lastScan < REFRESH_FRAMES
                && scanOrigin != null && distanceSquared(eye, scanOrigin) < 16.0) return current;
        scanOrigin = eye;
        lastScan = frame.frameIndex();
        dirty = false;
        int eyeX = (int) Math.floor(eye.x());
        int eyeY = (int) Math.floor(eye.y());
        int eyeZ = (int) Math.floor(eye.z());
        int bottom = Math.max(level.getMinBuildHeight(), eyeY - 34);
        int top = Math.min(level.getMaxBuildHeight() - 2, eyeY);
        Matrix4f viewProjection = matrix(frame.unjitteredProjection()).mul(matrix(frame.view()));
        var origin = frame.origin();
        var camera = Minecraft.getInstance().getCameraEntity();
        Vec3 eyePosition = new Vec3(eye.x(), eye.y(), eye.z());
        Map<Integer, Candidate> candidates = new HashMap<>();
        BlockPos.MutableBlockPos water = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        Vector4f projected = new Vector4f();
        for (int dz = -RADIUS; dz <= RADIUS; dz += STRIDE) {
            for (int dx = -RADIUS; dx <= RADIUS; dx += STRIDE) {
                int x = eyeX + dx;
                int z = eyeZ + dz;
                if (level.getChunkSource().getChunk(x >> 4, z >> 4, false) == null) continue;
                for (int y = top; y >= bottom; y--) {
                    water.set(x, y, z);
                    var fluid = level.getFluidState(water);
                    if (!fluid.isSource() || !fluid.is(FluidTags.WATER)) continue;
                    above.set(x, y + 1, z);
                    var cover = level.getBlockState(above);
                    if (cover.getFluidState().is(FluidTags.WATER) || cover.canOcclude()) continue;
                    double surface = y + (double) fluid.getHeight(level, water);
                    if (surface >= eye.y() - 0.04 || !level.getBlockState(water).getCollisionShape(level, water).isEmpty())
                        continue;
                    projected.set((float) (x + 0.5 - origin.x()), (float) (surface - origin.y()),
                            (float) (z + 0.5 - origin.z()), 1.0F);
                    viewProjection.transform(projected);
                    float score = score(projected, dx, eye.y() - surface, dz);
                    if (score <= 0.0F) continue;
                    if (camera != null && level.clip(new ClipContext(eyePosition,
                            new Vec3(x + 0.5, surface + 0.02, z + 0.5),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera)).getType() != HitResult.Type.MISS)
                        continue;
                    int key = key(surface);
                    Candidate next = candidates.getOrDefault(key, new Candidate(surface, 0.0F, 0));
                    candidates.put(key, next.add(score));
                    break;
                }
            }
        }

        Candidate best = null;
        for (Candidate candidate : candidates.values()) {
            if (candidate.samples() >= 6 && (best == null || candidate.score() > best.score()
                    || (candidate.score() == best.score() && candidate.height() > best.height()))) best = candidate;
        }
        Candidate existing = Double.isFinite(current) ? candidates.get(key(current)) : null;
        if (existing != null && existing.samples() >= 4 && (best == null || existing.score() >= best.score() * 0.70F)) {
            return current;
        }
        current = best == null ? Double.NaN : best.height();
        return current;
    }

    void dirty(BlockPos position) {
        if (scanOrigin == null || (Math.abs(position.getX() - scanOrigin.x()) <= RADIUS + 4
                && Math.abs(position.getZ() - scanOrigin.z()) <= RADIUS + 4)) dirty = true;
    }

    void reset() {
        current = Double.NaN;
        lastScan = -REFRESH_FRAMES;
        scanOrigin = null;
        dirty = true;
    }

    private static int key(double height) { return (int) Math.round(height * 64.0); }

    private static float score(Vector4f projected, int dx, double dy, int dz) {
        if (projected.w <= 0.001F) return 0.0F;
        float extent = Math.max(Math.abs(projected.x / projected.w), Math.abs(projected.y / projected.w));
        if (extent > 1.15F) return 0.0F;
        float visibility = 1.0F - Math.max(0.0F, extent - 0.65F);
        double distance = Math.sqrt((double) dx * dx + dy * dy + (double) dz * dz);
        return (float) (visibility / (1.0 + distance * 0.07));
    }

    private static double distanceSquared(FrameSnapshot.Vec3d a, FrameSnapshot.Vec3d b) {
        double x = a.x() - b.x();
        double y = a.y() - b.y();
        double z = a.z() - b.z();
        return x * x + y * y + z * z;
    }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 matrix) { return new Matrix4f().set(matrix.columnMajor()); }
}
