package com.siirio.jemserver.smp.events;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class BoundaryCollision {
    private static final double CLEARANCE = 0.08;

    public static AABB structure(BoundingBox bounds) {
        return new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1);
    }

    public static AABB outside(BoundingBox bounds, float width, float height) {
        double radius = width / 2.0;
        return new AABB(bounds.minX() - radius, bounds.minY() - height, bounds.minZ() - radius,
                bounds.maxX() + 1 + radius, bounds.maxY() + 1, bounds.maxZ() + 1 + radius);
    }

    public static AABB inside(BoundingBox bounds, float width, float height) {
        return inside(bounds, width, height, 0);
    }

    public static AABB inside(BoundingBox bounds, float width, float height, double clearance) {
        double radius = width / 2.0 + clearance;
        return new AABB(bounds.minX() + radius, bounds.minY(), bounds.minZ() + radius,
                bounds.maxX() + 1 - radius, bounds.maxY() + 1 - height, bounds.maxZ() + 1 - radius);
    }

    public static AABB outside(EventNetwork.Boundary boundary, float width, float height) {
        return outside(bounds(boundary), width, height);
    }

    public static AABB inside(EventNetwork.Boundary boundary, float width, float height) {
        return inside(bounds(boundary), width, height);
    }

    public static boolean crosses(AABB box, Vec3 start, Vec3 end) {
        return intersection(box, start, end).isPresent();
    }

    public static Optional<Vec3> intersection(AABB box, Vec3 start, Vec3 end) {
        boolean startInside = box.contains(start);
        boolean endInside = box.contains(end);
        if (startInside == endInside && startInside) return Optional.empty();
        Optional<Vec3> hit = startInside ? box.clip(end, start) : box.clip(start, end);
        return startInside != endInside || hit.isPresent() ? hit : Optional.empty();
    }

    public static Vec3 keepOutside(AABB box, Vec3 start, Vec3 end) {
        Vec3 hit = intersection(box, start, end).orElse(start);
        Vec3 direction = end.subtract(start);
        return direction.lengthSqr() == 0 ? start : hit.subtract(direction.normalize().scale(CLEARANCE));
    }

    public static Vec3 keepOutside(AABB box, Vec3 position) {
        double minX = Math.abs(position.x - box.minX);
        double maxX = Math.abs(box.maxX - position.x);
        double minY = Math.abs(position.y - box.minY);
        double maxY = Math.abs(box.maxY - position.y);
        double minZ = Math.abs(position.z - box.minZ);
        double maxZ = Math.abs(box.maxZ - position.z);
        double edge = Math.min(Math.min(Math.min(minX, maxX), Math.min(minY, maxY)), Math.min(minZ, maxZ));
        if (edge == minX) return new Vec3(box.minX - CLEARANCE, position.y, position.z);
        if (edge == maxX) return new Vec3(box.maxX + CLEARANCE, position.y, position.z);
        if (edge == minY) return new Vec3(position.x, box.minY - CLEARANCE, position.z);
        if (edge == maxY) return new Vec3(position.x, box.maxY + CLEARANCE, position.z);
        if (edge == minZ) return new Vec3(position.x, position.y, box.minZ - CLEARANCE);
        return new Vec3(position.x, position.y, box.maxZ + CLEARANCE);
    }

    public static Vec3 keepInside(AABB box, Vec3 position) {
        return new Vec3(clamp(position.x, box.minX, box.maxX), clamp(position.y, box.minY, box.maxY), clamp(position.z, box.minZ, box.maxZ));
    }

    public static Vec3 constrainedMotion(Vec3 current, Vec3 corrected, Vec3 motion) {
        return new Vec3(current.x == corrected.x ? motion.x : 0, current.y == corrected.y ? motion.y : 0, current.z == corrected.z ? motion.z : 0);
    }

    public static Vec3 reflect(AABB box, Vec3 hit, Vec3 motion) {
        double x = Math.min(Math.abs(hit.x - box.minX), Math.abs(hit.x - box.maxX));
        double y = Math.min(Math.abs(hit.y - box.minY), Math.abs(hit.y - box.maxY));
        double z = Math.min(Math.abs(hit.z - box.minZ), Math.abs(hit.z - box.maxZ));
        if (x <= y && x <= z) return new Vec3(-motion.x, motion.y, motion.z);
        if (y <= z) return new Vec3(motion.x, -motion.y, motion.z);
        return new Vec3(motion.x, motion.y, -motion.z);
    }

    private static BoundingBox bounds(EventNetwork.Boundary boundary) {
        return new BoundingBox(boundary.minX(), boundary.minY(), boundary.minZ(), boundary.maxX(), boundary.maxY(), boundary.maxZ());
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max - CLEARANCE, value));
    }

    private BoundaryCollision() {}
}
