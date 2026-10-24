package com.siirio.jemserver.mixin;

import com.siirio.jemserver.smp.events.SolidBoundary;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public abstract class SolidBoundaryMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 jem$completeVehicleBoundary(Vec3 movement) {
        return SolidBoundary.limitMovement((Entity) (Object) this, movement);
    }

    @ModifyVariable(method = "collideBoundingBox", at = @At("HEAD"), argsOnly = true)
    private static List<VoxelShape> jem$solidBoundary(List<VoxelShape> collisions, Entity entity, Vec3 movement, AABB box, Level level) {
        if (entity == null) return collisions;
        List<VoxelShape> walls = SolidBoundary.collisionShapes(entity, box.expandTowards(movement));
        if (walls.isEmpty()) return collisions;
        List<VoxelShape> combined = new ArrayList<>(collisions.size() + walls.size());
        combined.addAll(collisions);
        combined.addAll(walls);
        return combined;
    }
}
