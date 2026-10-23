package com.siirio.jemcompat.feature.shelter;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Exact controller-local model geometry, clipped to each occupied cell before rotation. */
public final class ShelterGeometry {
    private static final java.util.Map<Integer, VoxelShape> SHAPES = cache();
    private ShelterGeometry() {}

    public static VoxelShape shape(int size, int cell, Direction facing) {
        return SHAPES.get(size * 108 + facing.get2DDataValue() * 27 + cell);
    }

    private static java.util.Map<Integer, VoxelShape> cache() {
        var result = new java.util.HashMap<Integer, VoxelShape>();
        for (int size : new int[] {2, 3}) for (Direction facing : Direction.Plane.HORIZONTAL)
            for (int cell = 0; cell < 27; cell++)
                result.put(size * 108 + facing.get2DDataValue() * 27 + cell, calculate(size, cell, facing));
        return java.util.Map.copyOf(result);
    }

    private static VoxelShape calculate(int size, int cell, Direction facing) {
        int x = cell % size, z = cell / size % size, y = cell / (size * size);
        VoxelShape result = Shapes.empty();
        for (double[] cube : boxes(size)) {
            double x0 = Math.max(0, cube[0] - 16 * x), y0 = Math.max(0, cube[1] - 16 * y);
            double z0 = Math.max(0, cube[2] - 16 * z), x1 = Math.min(16, cube[3] - 16 * x);
            double y1 = Math.min(16, cube[4] - 16 * y), z1 = Math.min(16, cube[5] - 16 * z);
            if (x0 >= x1 || y0 >= y1 || z0 >= z1) continue;
            double a = x0, b = z0, c = x1, d = z1;
            if (facing == Direction.NORTH) { a = 16 - x1; b = 16 - z1; c = 16 - x0; d = 16 - z0; }
            else if (facing == Direction.EAST) { a = z0; b = 16 - x1; c = z1; d = 16 - x0; }
            else if (facing == Direction.WEST) { a = 16 - z1; b = x0; c = 16 - z0; d = x1; }
            result = Shapes.or(result, Shapes.box(a / 16, y0 / 16, b / 16, c / 16, y1 / 16, d / 16));
        }
        return result;
    }

    /** Native model units; these same boxes are the baked-model export authority. */
    public static double[][] boxes(int size) {
        if (size == 2) return new double[][] {
            {2,0,2,30,2,30}, {3,2,3,6,24,6}, {26,2,3,29,24,6},
            {3,2,26,6,24,29}, {26,2,26,29,24,29},
            {3,2,6,5,9,26}, {27,2,6,29,9,26},
            {3,14,6,5,24,26}, {27,14,6,29,24,26},
            {6,2,3,26,9,5}, {6,14,3,26,24,5},
            {0,24,0,32,26,32}, {4,26,0,28,28,32},
            {8,28,0,24,30,32}, {12,30,0,20,32,32}
        };
        return new double[][] {
            {2,0,2,46,2,46}, {3,2,3,6,41,6}, {42,2,3,45,41,6},
            {3,2,42,6,41,45}, {42,2,42,45,41,45},
            {3,2,6,5,14,42}, {43,2,6,45,14,42},
            {3,24,6,5,41,42}, {43,24,6,45,41,42},
            {6,2,3,42,14,5}, {6,24,3,42,41,5},
            {0,41,0,48,43,48}, {8,43,0,40,45,48}, {16,45,0,32,48,48}
        };
    }
}
