package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Resolve visible collision tops, never the center of the carrier block. */
final class SurfacePlacement {
    private SurfacePlacement() {}
    static Vec3 top(ServerPlayer player,Vec3 requested,int verticalRange) {
        var level=player.serverLevel();BlockPos column=BlockPos.containing(requested);
        for(int distance=0;distance<=verticalRange;distance++)for(int sign:distance==0?new int[]{1}:new int[]{1,-1}) {
            BlockPos floor=column.offset(0,distance*sign-1,0);
            if(!level.isInWorldBounds(floor) || !level.hasChunkAt(floor) || !level.getWorldBorder().isWithinBounds(floor))continue;
            var shape=level.getBlockState(floor).getCollisionShape(level,floor);
            if(shape.isEmpty() || !level.getBlockState(floor.above()).getCollisionShape(level,floor.above()).isEmpty())continue;
            return new Vec3(requested.x,floor.getY()+shape.max(Direction.Axis.Y)+.04,requested.z);
        }
        return null;
    }
    static List<Vec3> sculkPath(ServerPlayer player,List<Vec3> centers) {
        List<Vec3> result=new ArrayList<>();
        for(Vec3 center:centers) {
            Vec3 top=top(player,center.add(0,.5,0),10);
            // A missing exposed surface splits the strip, never bridges buried terrain.
            if(top==null) { if(result.size()>1)break;else continue; }
            result.add(top);if(result.size()==128)break;
        }
        return List.copyOf(result);
    }
}
