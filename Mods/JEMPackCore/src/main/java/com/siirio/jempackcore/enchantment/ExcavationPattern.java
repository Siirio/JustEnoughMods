package com.siirio.jempackcore.enchantment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class ExcavationPattern {
    private static final int RADIUS = 1;

    public static List<BlockPos> positions(BlockPos origin, Direction.Axis normal) {
        List<BlockPos> result = new ArrayList<>(9);
        for (int first = -RADIUS; first <= RADIUS; first++)
            for (int second = -RADIUS; second <= RADIUS; second++)
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
