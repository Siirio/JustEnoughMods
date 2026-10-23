package com.siirio.jemcompat.feature.shelter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShelterRecovery {
    private ShelterRecovery() { }
    public static void nearby(ServerPlayer player) {
        ServerLevel level = player.serverLevel(); CompanionData data = CompanionData.get(level);
        for (CompanionData.Pet pet : data.pendingHomes(level.dimension().location().toString())) {
            if (pet.home.distToCenterSqr(player.position()) <= 32 * 32 && level.hasChunkAt(pet.home)) restore(level, data, pet);
        }
    }
    public static boolean homeValid(ServerLevel level, CompanionData.Pet pet) {
        if (pet.home == null || !level.hasChunkAt(pet.home)) return false;
        BlockState state = level.getBlockState(pet.home);
        if (!(state.getBlock() instanceof ShelterBlock block) || state.getValue(ShelterBlock.CELL) != 0
                || !(level.getBlockEntity(pet.home) instanceof ShelterController controller) || !pet.owner.equals(controller.owner())) return false;
        // Chunk availability and structural validity are separate: a naturally unloaded neighboring cell is not destruction.
        for (BlockPos pos : block.cells(pet.home, state.getValue(ShelterBlock.FACING))) if (!level.hasChunkAt(pos)) return false;
        return block.intact(level, pet.home, state.getValue(ShelterBlock.FACING));
    }
    private static void restore(ServerLevel level, CompanionData data, CompanionData.Pet pet) {
        // Even an entity in another loaded dimension owns this identity until native death removal completes.
        for (ServerLevel other : level.getServer().getAllLevels()) if (other.getEntity(pet.id) != null) return;
        if (pet.restoring) {
            // An unacknowledged native write may already contain a moved pet in another unloaded chunk.
            // Never guess whether that entity exists. Keep the WAL until native disk/join reconciliation.
            pet.blocked = "awaiting_native_reconciliation"; data.setDirty(); return;
        }
        if (level.getServer().getPlayerList().getPlayer(pet.owner) == null) { pet.blocked = "owner_offline"; data.setDirty(); return; }
        if (!homeValid(level, pet)) { pet.blocked = "home_unavailable"; data.setDirty(); return; }
        Entity restored = EntityType.loadEntityRecursive(pet.pending.copy(), level, entity -> entity);
        if (!(restored instanceof LivingEntity animal) || !pet.id.equals(restored.getUUID()) || !VanillaCompanions.supported(restored) || !pet.owner.equals(VanillaCompanions.owner(restored))) {
            pet.blocked = "native_data_unsupported"; data.setDirty(); return;
        }
        animal.setHealth(animal.getMaxHealth()); animal.deathTime = 0; animal.hurtTime = 0;
        animal.clearFire(); animal.fallDistance = 0; animal.setAirSupply(animal.getMaxAirSupply());
        animal.removeAllEffects(); animal.setDeltaMovement(Vec3.ZERO);
        BlockState state = level.getBlockState(pet.home); ShelterBlock block = (ShelterBlock) state.getBlock();
        Vec3 point = safePoint(level, animal, pet.home, block, state.getValue(ShelterBlock.FACING), pet.owner);
        if (point == null) { pet.blocked = "no_safe_space"; data.setDirty(); return; }
        animal.moveTo(point.x, point.y, point.z, animal.getYRot(), 0);
        animal.getPersistentData().putLong(CompanionData.ENTITY_EPOCH, pet.epoch);
        if (VanillaCompanions.nativeOwnerTeleport(animal)) animal.getPersistentData().putBoolean(CompanionData.FOLLOW_HOLD, true);
        pet.restoring = true; pet.blocked = ""; pet.dimension = level.dimension().location().toString(); pet.lastPosition = animal.blockPosition();
        try { data.flush(level); }
        catch (RuntimeException failure) { pet.restoring = false; data.setDirty(); throw failure; }
        // Keep Pending until the exact native entity-storage write succeeds (or matching native disk reload).
        if (level.addFreshEntity(animal)) {
            data.setDirty();
        } else { pet.restoring = false; pet.blocked = "spawn_denied"; data.flush(level); }
    }
    private record Candidate(Vec3 feet, boolean interior, double distance, int projection) { }
    static Vec3 safePoint(ServerLevel level, LivingEntity animal, BlockPos anchor, ShelterBlock block, Direction facing, java.util.UUID owner) {
        List<BlockPos> footprint = block.cells(anchor, facing).stream().filter(p -> p.getY() == anchor.getY()).toList();
        double centerX = footprint.stream().mapToDouble(p -> p.getX() + .5).average().orElse(anchor.getX() + .5);
        double centerZ = footprint.stream().mapToDouble(p -> p.getZ() + .5).average().orElse(anchor.getZ() + .5);
        List<Candidate> candidates = new ArrayList<>();
        // The authored floor top is two model units above the anchor floor.
        candidates.add(new Candidate(new Vec3(centerX, anchor.getY() + 2.0 / 16, centerZ), true, 0, 0));
        for (int x = -8; x <= block.size() + 7; x++) for (int z = -8; z <= block.size() + 7; z++) {
            BlockPos column = anchor.relative(facing.getCounterClockWise(), x).relative(facing, z);
            int horizontal = Math.max(0, -x) + Math.max(0, x - block.size() + 1) + Math.max(0, -z) + Math.max(0, z - block.size() + 1);
            if (horizontal > 8) continue;
            if (!level.hasChunksAt(column.offset(-1, -1, -1), column.offset(1, 1, 1))) continue;
            for (int y = -8; y <= 32; y++) {
                BlockPos support = new BlockPos(column.getX(), anchor.getY() + y - 1, column.getZ());
                if (level.isOutsideBuildHeight(support) || !level.getFluidState(support).isEmpty()) continue;
                var shape = level.getBlockState(support).getCollisionShape(level, support);
                if (shape.isEmpty()) continue;
                double surface = support.getY() + shape.max(Direction.Axis.Y);
                Vec3 feet = new Vec3(column.getX() + .5, surface, column.getZ() + .5);
                double displacement = surface - anchor.getY();
                double dx = feet.x - centerX, dz = feet.z - centerZ;
                candidates.add(new Candidate(feet, false, dx * dx + dz * dz + displacement * displacement, horizontal == 0 ? 0 : 1));
            }
        }
        candidates.sort(Comparator.comparing((Candidate c) -> !c.interior).thenComparingDouble(c -> Math.floor(Math.sqrt(c.distance)))
                .thenComparingInt(Candidate::projection).thenComparingDouble(Candidate::distance)
                .thenComparingDouble(c -> c.feet.y).thenComparingDouble(c -> c.feet.x).thenComparingDouble(c -> c.feet.z));
        for (Candidate candidate : candidates) if (safe(level, animal, candidate.feet, owner)) return candidate.feet;
        return null;
    }
    private static boolean safe(ServerLevel level, LivingEntity animal, Vec3 feet, java.util.UUID owner) {
        animal.moveTo(feet.x, feet.y, feet.z, animal.getYRot(), 0); AABB box = animal.getBoundingBox();
        if (box.minY < level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(box)) return false;
        // hasChunksAt precedes collision/fluid calls: those vanilla calls may otherwise fetch missing chunks.
        BlockPos min = BlockPos.containing(box.minX - .01, box.minY - 1, box.minZ - .01);
        BlockPos max = BlockPos.containing(box.maxX + .01, box.maxY + .01, box.maxZ + .01);
        // Native/Forge path-type and support-shape callbacks may examine immediate neighbors.
        if (!level.hasChunksAt(min.offset(-1, -1, -1), max.offset(1, 1, 1))) return false;
        if (!level.noCollision(animal, box) || !level.getEntities(animal, box, entity -> entity.isAlive()).isEmpty() || level.containsAnyLiquid(box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            var nativePathType = net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable());
            if (nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_OTHER
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_OTHER
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_CAUTIOUS) return false;
            if (!level.getFluidState(pos).isEmpty() || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)
                    || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.END_PORTAL) || state.is(Blocks.NETHER_PORTAL)) return false;
            if (!ShelterProtection.recoveryAllowed(level, owner, pos)) return false;
        }
        // Every part of the feet needs supporting collision; no half-supported ledges or water landings.
        AABB support = new AABB(box.minX + .01, box.minY - .03, box.minZ + .01, box.maxX - .01, box.minY, box.maxZ - .01);
        for (double x : new double[]{support.minX, (support.minX + support.maxX) / 2, support.maxX}) for (double z : new double[]{support.minZ, (support.minZ + support.maxZ) / 2, support.maxZ}) {
            AABB corner = new AABB(x - .005, support.minY, z - .005, x + .005, support.maxY, z + .005);
            if (level.noCollision(animal, corner)) return false;
        }
        return true;
    }
}
