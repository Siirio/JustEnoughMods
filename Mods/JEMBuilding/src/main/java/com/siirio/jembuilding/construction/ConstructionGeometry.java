package com.siirio.jembuilding.construction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class ConstructionGeometry {
    public static final int MAX_SPAN = 256;
    public static final int MAX_SPHERE_RADIUS = (MAX_SPAN - 1) / 2;

    public static BlockPos constrain(ConstructionShape shape, ConstructionPlane plane, BlockPos start, BlockPos requested) {
        BlockPos limited = new BlockPos(
                clamp(requested.getX(), start.getX()),
                clamp(requested.getY(), start.getY()),
                clamp(requested.getZ(), start.getZ()));
        return switch (shape) {
            case WALL, FLOOR -> plane.constrain(start, limited);
            case COLUMN -> new BlockPos(start.getX(), limited.getY(), start.getZ());
            case SPHERE -> constrainSphere(start, limited);
        };
    }

    public static int sphereRadius(BlockPos center, BlockPos endpoint) {
        double distance = Math.sqrt(center.distSqr(endpoint));
        return Math.min(MAX_SPHERE_RADIUS, Math.max(0, (int) Math.round(distance)));
    }

    public static boolean validSpan(BlockPos first, BlockPos second) {
        return span(first.getX(), second.getX()) <= MAX_SPAN
                && span(first.getY(), second.getY()) <= MAX_SPAN
                && span(first.getZ(), second.getZ()) <= MAX_SPAN;
    }

    public static List<BlockPos> spherePositions(BlockPos center, int radius) {
        if (radius == 0) return List.of(center.immutable());
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) {
            double outer = (radius + 0.5D) * (radius + 0.5D) - dx * dx - dy * dy;
            if (outer < 0D) continue;
            int maxAbsZ = (int) Math.floor(Math.sqrt(outer));
            double inner = Math.max(0D, (radius - 0.5D) * (radius - 0.5D) - dx * dx - dy * dy);
            int minAbsZ = inner > 0D ? (int) Math.floor(Math.sqrt(inner)) + 1 : 0;
            for (int dz = -maxAbsZ; dz <= maxAbsZ; dz++) {
                if (Math.abs(dz) >= minAbsZ) positions.add(center.offset(dx, dy, dz));
            }
        }
        return Collections.unmodifiableList(positions);
    }

    private static BlockPos constrainSphere(BlockPos center, BlockPos requested) {
        int radius = sphereRadius(center, requested);
        if (radius == 0) return center;
        double dx = requested.getX() - center.getX();
        double dy = requested.getY() - center.getY();
        double dz = requested.getZ() - center.getZ();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5D) return center.offset(radius, 0, 0);
        return center.offset((int) Math.round(dx / length * radius), (int) Math.round(dy / length * radius),
                (int) Math.round(dz / length * radius));
    }

    private static int clamp(int value, int origin) {
        return Math.max(origin - MAX_SPAN + 1, Math.min(origin + MAX_SPAN - 1, value));
    }

    private static long span(int first, int second) {
        return Math.abs((long) first - second) + 1L;
    }

    private ConstructionGeometry() { }
}
