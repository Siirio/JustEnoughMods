package com.siirio.jemcompat.feature.excavation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class ExcavationPattern {
    public static List<BlockPos> positions(BlockPos origin, Direction.Axis normal, int level) {
        int radius = level >= 2 ? 3 : level == 1 ? 1 : 0;
        int width = radius * 2 + 1;
        List<BlockPos> result = new ArrayList<>(width * width);
        for (int first = -radius; first <= radius; first++)
            for (int second = -radius; second <= radius; second++)
                result.add(offset(origin, normal, first, second));
        return result;
    }

    private static BlockPos offset(BlockPos origin, Direction.Axis normal, int first, int second) {
        return switch (normal) {
            case X -> origin.offset(0, first, second);
            case Y -> origin.offset(first, 0, second);
            case Z -> origin.offset(first, second, 0);
        };
    }

    private ExcavationPattern() {
    }
}
