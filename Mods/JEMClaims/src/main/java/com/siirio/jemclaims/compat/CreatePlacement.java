package com.siirio.jemclaims.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class CreatePlacement {
    @FunctionalInterface
    public interface Observer {
        void placing(ServerLevel level, BlockPos source, BlockPos target);
    }

    private static Observer observer = (level, source, target) -> {};

    private CreatePlacement() {}

    public static void setObserver(Observer value) { observer = java.util.Objects.requireNonNull(value); }

    public static void placing(ServerLevel level, BlockPos source, BlockPos target) {
        observer.placing(level, source == null ? target.immutable() : source.immutable(), target.immutable());
    }

    public static void placing(ServerLevel level, BlockPos source, LaunchedItem item) {
        placing(level, source, item.target);
        if (item instanceof LaunchedItem.ForBelt belt) {
            BlockPos step = BeltBlock.nextSegmentPosition(belt.state, BlockPos.ZERO, belt.state.getValue(BeltBlock.PART) == BeltPart.START);
            if (step != null) for (int segment = 1; segment < belt.length; segment++) placing(level, source, item.target.offset(step.multiply(segment)));
        }
    }

    public static boolean allowed(ServerLevel level, BlockPos source, LaunchedItem item) {
        if (!allowed(level, source, item.target)) return false;
        if (item instanceof LaunchedItem.ForBelt belt) {
            BlockPos step = BeltBlock.nextSegmentPosition(belt.state, BlockPos.ZERO, belt.state.getValue(BeltBlock.PART) == BeltPart.START);
            if (step == null) return false;
            for (int segment = 1; segment < belt.length; segment++) {
                if (!allowed(level, source, item.target.offset(step.multiply(segment)))) return false;
            }
        }
        return true;
    }

    private static boolean allowed(ServerLevel level, BlockPos source, BlockPos target) {
        return FlanBridge.canAutomate(level, source, target, ClaimPermission.PLACE)
                && FlanBridge.canAutomate(level, source, target, ClaimPermission.BREAK);
    }
}
