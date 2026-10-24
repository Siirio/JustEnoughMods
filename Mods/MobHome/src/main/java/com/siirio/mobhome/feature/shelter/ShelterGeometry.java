package com.siirio.mobhome.feature.shelter;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Native exported solids, clipped per cell. Sloped roof collision uses conservative two-unit stairs. */
public final class ShelterGeometry {
    private static final java.util.Map<Integer, double[][]> BOXES = load();
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

    private static double[][] boxes(int size) { return BOXES.get(size); }

    private static java.util.Map<Integer, double[][]> load() {
        var result = new java.util.HashMap<Integer, double[][]>();
        try (var stream = ShelterGeometry.class.getResourceAsStream("/data/mobhome/shelter_geometry.json")) {
            if (stream == null) throw new IllegalStateException("Missing native shelter collision export");
            var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (int size : new int[] {2, 3}) {
                var source = data.getAsJsonArray(Integer.toString(size));
                double[][] boxes = new double[source.size()][6];
                for (int index = 0; index < source.size(); index++) {
                    var box = source.get(index).getAsJsonArray();
                    if (box.size() != 6) throw new IllegalStateException("Invalid native shelter collision box");
                    for (int coordinate = 0; coordinate < 6; coordinate++) boxes[index][coordinate] = box.get(coordinate).getAsDouble();
                }
                result.put(size, boxes);
            }
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read native shelter collision export", failure);
        }
        return java.util.Map.copyOf(result);
    }
}
