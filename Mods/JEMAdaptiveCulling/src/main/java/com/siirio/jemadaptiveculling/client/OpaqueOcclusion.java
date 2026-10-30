package com.siirio.jemadaptiveculling.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OpaqueOcclusion {
    private static final double INSET = 0.00001D;
    private static final double SEGMENT_MARGIN = 0.0000001D;
    private static final int MAX_BLOCK_READS = 192;
    private static final int MAX_CANDIDATES = 8;
    private static final int WALL_RADIUS = 4;

    private OpaqueOcclusion() {
    }

    public static AABB findOccluder(BlockGetter level, Vec3 camera, AABB target, double margin, long deadline) {
        Vec3 center = new Vec3((target.minX + target.maxX) * 0.5D,
                (target.minY + target.maxY) * 0.5D, (target.minZ + target.maxZ) * 0.5D);
        Query query = new Query(level, camera, target, margin, deadline, center);
        AABB result = BlockGetter.traverseBlocks(camera, center, query,
                (current, position) -> current.visit(position), current -> null);
        return result == target ? null : result;
    }

    public static boolean covers(AABB solid, Vec3 camera, AABB target, double margin) {
        if (!validSolid(solid) || solid.contains(camera)) return false;
        double minX = solid.minX + INSET - camera.x;
        double minY = solid.minY + INSET - camera.y;
        double minZ = solid.minZ + INSET - camera.z;
        double maxX = solid.maxX - INSET - camera.x;
        double maxY = solid.maxY - INSET - camera.y;
        double maxZ = solid.maxZ - INSET - camera.z;
        if (minX >= maxX || minY >= maxY || minZ >= maxZ) return false;
        for (int corner = 0; corner < 8; corner++) {
            double dx = ((corner & 1) == 0 ? target.minX - margin : target.maxX + margin) - camera.x;
            double dy = ((corner & 2) == 0 ? target.minY - margin : target.maxY + margin) - camera.y;
            double dz = ((corner & 4) == 0 ? target.minZ - margin : target.maxZ + margin) - camera.z;
            double near = SEGMENT_MARGIN;
            double far = 1.0D - SEGMENT_MARGIN;
            if (dx == 0) {
                if (minX >= 0 || maxX <= 0) return false;
            } else {
                double first = minX / dx, second = maxX / dx;
                near = Math.max(near, Math.min(first, second));
                far = Math.min(far, Math.max(first, second));
            }
            if (dy == 0) {
                if (minY >= 0 || maxY <= 0) return false;
            } else {
                double first = minY / dy, second = maxY / dy;
                near = Math.max(near, Math.min(first, second));
                far = Math.min(far, Math.max(first, second));
            }
            if (dz == 0) {
                if (minZ >= 0 || maxZ <= 0) return false;
            } else {
                double first = minZ / dz, second = maxZ / dz;
                near = Math.max(near, Math.min(first, second));
                far = Math.min(far, Math.max(first, second));
            }
            if (!Double.isFinite(near) || !Double.isFinite(far) || near + SEGMENT_MARGIN >= far) return false;
        }
        return true;
    }

    private static boolean validSolid(AABB solid) {
        return solid != null && Double.isFinite(solid.minX) && Double.isFinite(solid.minY) && Double.isFinite(solid.minZ)
                && Double.isFinite(solid.maxX) && Double.isFinite(solid.maxY) && Double.isFinite(solid.maxZ)
                && solid.getXsize() > INSET * 2 && solid.getYsize() > INSET * 2 && solid.getZsize() > INSET * 2
                && solid.getXsize() <= WALL_RADIUS * 2 + 1 && solid.getYsize() <= WALL_RADIUS * 2 + 1
                && solid.getZsize() <= WALL_RADIUS * 2 + 1;
    }

    private static final class Query {
        private final BlockGetter level;
        private final Vec3 camera;
        private final AABB target;
        private final double margin;
        private final long deadline;
        private final int axis;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private int reads;
        private int candidates;

        private Query(BlockGetter level, Vec3 camera, AABB target, double margin, long deadline, Vec3 center) {
            this.level = level;
            this.camera = camera;
            this.target = target;
            this.margin = margin;
            this.deadline = deadline;
            double dx = Math.abs(center.x - camera.x), dy = Math.abs(center.y - camera.y), dz = Math.abs(center.z - camera.z);
            axis = dx >= dy && dx >= dz ? 0 : dy >= dz ? 1 : 2;
        }

        private AABB visit(BlockPos position) {
            if (expired() || candidates >= MAX_CANDIDATES) return target;
            BlockState state = read(position);
            if (state == null || !CompatibilityPolicy.canOcclude(state)) return null;
            VoxelShape shape = state.getOcclusionShape(level, position);
            ShapeClassifier.ShapeKind kind = ShapeClassifier.classify(shape);
            if (kind == ShapeClassifier.ShapeKind.EMPTY) return null;
            candidates++;
            if (kind == ShapeClassifier.ShapeKind.FULL) {
                AABB block = new AABB(position);
                if (covers(block, camera, target, margin)) return block;
                AABB wall = wall(position);
                return wall != null && covers(wall, camera, target, margin) ? wall : null;
            }
            int boxes = 0;
            for (AABB box : ShapeClassifier.boxes(shape)) {
                if (++boxes > 32 || expired()) return target;
                if (!validSolid(box) || box.minX < 0 || box.minY < 0 || box.minZ < 0
                        || box.maxX > 1 || box.maxY > 1 || box.maxZ > 1) continue;
                AABB solid = box.move(position);
                if (covers(solid, camera, target, margin)) return solid;
            }
            return null;
        }

        private AABB wall(BlockPos origin) {
            int lowU = 0, highU = 0, lowV = 0, highV = 0;
            while (lowU > -WALL_RADIUS && row(origin, lowU - 1, lowV, highV, true)) lowU--;
            while (highU < WALL_RADIUS && row(origin, highU + 1, lowV, highV, true)) highU++;
            while (lowV > -WALL_RADIUS && row(origin, lowV - 1, lowU, highU, false)) lowV--;
            while (highV < WALL_RADIUS && row(origin, highV + 1, lowU, highU, false)) highV++;
            if (expired()) return null;
            int x = origin.getX(), y = origin.getY(), z = origin.getZ();
            return switch (axis) {
                case 0 -> new AABB(x, y + lowU, z + lowV, x + 1, y + highU + 1, z + highV + 1);
                case 1 -> new AABB(x + lowU, y, z + lowV, x + highU + 1, y + 1, z + highV + 1);
                default -> new AABB(x + lowU, y + lowV, z, x + highU + 1, y + highV + 1, z + 1);
            };
        }

        private boolean row(BlockPos origin, int fixed, int low, int high, boolean fixedU) {
            for (int value = low; value <= high; value++) {
                if (expired()) return false;
                int u = fixedU ? fixed : value, v = fixedU ? value : fixed;
                switch (axis) {
                    case 0 -> cursor.set(origin.getX(), origin.getY() + u, origin.getZ() + v);
                    case 1 -> cursor.set(origin.getX() + u, origin.getY(), origin.getZ() + v);
                    default -> cursor.set(origin.getX() + u, origin.getY() + v, origin.getZ());
                }
                BlockState state = read(cursor);
                if (state == null || !CompatibilityPolicy.canOcclude(state)
                        || ShapeClassifier.classify(state.getOcclusionShape(level, cursor)) != ShapeClassifier.ShapeKind.FULL) return false;
            }
            return true;
        }

        private BlockState read(BlockPos position) {
            if (expired()) return null;
            reads++;
            return level.getBlockState(position);
        }

        private boolean expired() {
            return reads >= MAX_BLOCK_READS || System.nanoTime() >= deadline;
        }
    }
}
