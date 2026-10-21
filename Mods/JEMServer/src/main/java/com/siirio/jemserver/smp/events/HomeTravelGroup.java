package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.DeathHistory;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded whole-group preflight, native UUID-preserving transfers and explicit rollback. */
final class HomeTravelGroup {
    private record Snapshot(Entity entity, ServerLevel level, Vec3 position, float yaw, float pitch, UUID vehicle) {}
    private HomeTravelGroup() {}
    static int execute(ServerPlayer player, ServerLevel destination, BlockPos bed) {
        LinkedHashSet<Entity> selected = new LinkedHashSet<>();
        selected.add(player);
        player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1D),
                entity -> entity.isAlive() && !entity.isSpectator() && entity.position().distanceToSqr(player.position()) <= 1D).forEach(selected::add);
        if (player.isPassenger()) player.getRootVehicle().getSelfAndPassengers().forEach(selected::add);
        for (Entity entity : List.copyOf(selected)) if (entity.isPassenger()) entity.getRootVehicle().getSelfAndPassengers().forEach(selected::add);
        if (selected.size() > 64) return fail(player, "jem.home.group_too_large");
        for (Entity entity : selected) {
            if (!entity.isAlive() || entity.isSpectator() || entity.level() != player.level()
                    || net.minecraftforge.fml.ModList.get().isLoaded("jem_settlements") && !HomeSettlementAccess.permits(player, entity)
                    || !(entity instanceof LivingEntity) && entity.getControllingPassenger() != null && entity.getControllingPassenger() != player
                    || entity instanceof OwnableEntity owned && owned.getOwnerUUID() != null && !owned.getOwnerUUID().equals(player.getUUID())
                    || entity instanceof ServerPlayer member && !HomeTravel.ready(member)
                    || destination != entity.level() && !entity.canChangeDimensions()) return fail(player, "jem.home.group_unavailable");
        }
        List<Entity> roots = selected.stream().filter(entity -> entity.getVehicle() == null).toList();
        Map<UUID, Vec3> positions = new LinkedHashMap<>();
        List<AABB> occupied = new ArrayList<>();
        for (Entity root : roots) {
            var family = root.getSelfAndPassengers().filter(selected::contains).toList();
            Vec3 chosen = null;
            search: for (int radius = 0; radius <= 8; radius++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                for (int y = -3; y <= 4; y++) {
                    Vec3 candidate = Vec3.atBottomCenterOf(bed.offset(x, y, z));
                    Vec3 delta = candidate.subtract(root.position());
                    AABB union = root.getBoundingBox().move(delta);
                    for (Entity member : family) union = union.minmax(member.getBoundingBox().move(delta));
                    if (occupied.stream().anyMatch(union::intersects) || !safe(root, destination, union, selected)) continue;
                    boolean permitted = true;
                    for (Entity member : family) {
                        Vec3 target = member.position().add(delta);
                        var decision = BossTeleportSafety.validate(member, destination, target);
                        if (decision.matched() && !target.equals(decision.destination())) { permitted = false; break; }
                    }
                    if (!permitted) continue;
                    chosen = delta; occupied.add(union); break search;
                }
            }
            if (chosen == null) return fail(player, "jem.home.unsafe");
            for (Entity member : family) positions.put(member.getUUID(), member.position().add(chosen));
        }
        List<Snapshot> before = selected.stream().map(entity -> new Snapshot(entity, (ServerLevel)entity.level(), entity.position(),
                entity.getYRot(), entity.getXRot(), entity.getVehicle() == null ? null : entity.getVehicle().getUUID())).toList();
        Map<UUID, Entity> current = new HashMap<>(); selected.forEach(entity -> current.put(entity.getUUID(), entity));
        try {
            for (Entity entity : selected) { if (entity instanceof ServerPlayer member) member.closeContainer(); entity.stopRiding(); }
            for (Snapshot state : before) {
                Vec3 target = positions.get(state.entity.getUUID());
                Entity moved = move(state.entity, destination, target, state.yaw, state.pitch);
                if (moved == null) throw new IllegalStateException("Native group transfer rejected " + state.entity.getUUID());
                current.put(moved.getUUID(), moved);
            }
            attach(before, current);
        } catch (RuntimeException failure) {
            boolean restored = true;
            for (Entity entity : current.values()) {
                try { entity.stopRiding(); }
                catch (RuntimeException detachFailure) { restored = false; failure.addSuppressed(detachFailure); }
            }
            Set<UUID> recovered = new HashSet<>();
            for (Snapshot state : before) {
                try {
                    UUID id = state.entity.getUUID();
                    // A native transfer may replace the entity before throwing.
                    Entity live = destination.getEntity(id);
                    if (live == null) live = state.level.getEntity(id);
                    Entity moved = move(live, state.level, state.position, state.yaw, state.pitch);
                    if (moved == null) restored = false;
                    else { current.put(id, moved); recovered.add(id); }
                } catch (RuntimeException restoreFailure) { restored = false; failure.addSuppressed(restoreFailure); }
            }
            for (Snapshot state : before) if (state.vehicle != null) {
                try {
                    if (!recovered.contains(state.entity.getUUID()) || !recovered.contains(state.vehicle)
                            || !current.get(state.entity.getUUID()).startRiding(current.get(state.vehicle), true)) restored = false;
                } catch (RuntimeException attachFailure) { restored = false; failure.addSuppressed(attachFailure); }
            }
            com.mojang.logging.LogUtils.getLogger().error("/home all native transfer failed; rollback complete={}", restored, failure);
            return fail(player, restored ? "jem.home.group_rolled_back" : "jem.home.group_recovery_required");
        }
        for (Entity entity : current.values()) if (entity instanceof ServerPlayer member) HomeTravel.used(member);
        player.sendSystemMessage(Component.translatable("jem.home.arrived"), true); return 1;
    }
    private static Entity move(Entity entity, ServerLevel level, Vec3 position, float yaw, float pitch) {
        if (entity == null || !entity.teleportTo(level, position.x, position.y, position.z, Set.of(), yaw, pitch)) return null;
        Entity result = level.getEntity(entity.getUUID());
        if (result != null) { result.setDeltaMovement(Vec3.ZERO); result.fallDistance = 0F; }
        return result;
    }
    private static void attach(List<Snapshot> before, Map<UUID, Entity> entities) {
        for (Snapshot state : before) if (state.vehicle != null && !entities.get(state.entity.getUUID()).startRiding(entities.get(state.vehicle), true))
            throw new IllegalStateException("Native passenger restoration rejected " + state.entity.getUUID());
    }
    private static boolean safe(Entity root, ServerLevel level, AABB box, Set<Entity> moving) {
        if (box.minY < level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!level.hasChunkAt(pos)) return false;
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || DeathHistory.dangerous(state)) return false;
        }
        BlockPos floor = BlockPos.containing((box.minX + box.maxX) / 2D, box.minY - .01D, (box.minZ + box.maxZ) / 2D);
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP) || DeathHistory.dangerous(level.getBlockState(floor))) return false;
        return !level.getBlockCollisions(root, box).iterator().hasNext()
                && level.getEntities(root, box, entity -> !moving.contains(entity) && entity.isAlive() && !entity.isSpectator()).isEmpty();
    }
    private static int fail(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable(key), true); return 0; }
}
