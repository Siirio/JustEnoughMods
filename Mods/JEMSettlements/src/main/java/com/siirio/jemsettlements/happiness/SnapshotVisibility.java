package com.siirio.jemsettlements.happiness;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Disclosure policy runs before palette construction, including cutaway and filter data. */
public final class SnapshotVisibility {
    private SnapshotVisibility() { }

    public static boolean exposed(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (level.hasChunkAt(adjacent) && !level.getBlockState(adjacent).canOcclude()) return true;
        }
        return false;
    }

    public static boolean disclose(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer viewer) {
        if (!exposed(level, pos, state)) return false;
        // An ore exposed to an enclosed cave is still secret. Native ore families and
        // installed ore tags require actual server ray visibility from this viewer.
        boolean resource = state.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.equals("ores") || path.startsWith("ores/") || path.endsWith("_ores");
        });
        if (!resource) return true;
        if (viewer == null || viewer.level() != level || viewer.distanceToSqr(Vec3.atCenterOf(pos)) > 4096) return false;
        for (Direction direction : Direction.values()) {
            Vec3 target = Vec3.atCenterOf(pos).add(direction.getStepX() * .499, direction.getStepY() * .499, direction.getStepZ() * .499);
            var hit = level.clip(new ClipContext(viewer.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) return true;
        }
        return false;
    }
}
