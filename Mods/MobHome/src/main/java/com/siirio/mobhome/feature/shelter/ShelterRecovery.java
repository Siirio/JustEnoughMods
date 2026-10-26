package com.siirio.mobhome.feature.shelter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
    private static final java.util.Set<Entity> ADMITTING = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private ShelterRecovery() { }
    public static boolean admitting(Entity entity) { return ADMITTING.contains(entity); }
    public static void nearby(ServerPlayer player) {
        ServerLevel level = player.serverLevel(); CompanionData data = CompanionData.get(level);
        for (CompanionData.Pet pet : data.pendingHomes(level.dimension().location().toString())) {
            if (pet.home.distToCenterSqr(player.position()) <= 32 * 32 && level.hasChunkAt(pet.home)) restore(level, data, pet);
        }
        for (CompanionData.Pet pet : data.owned(player.getUUID())) {
            if (pet.home == null || pet.pending != null || !pet.homeDimension.equals(level.dimension().location().toString())
                    || pet.home.distToCenterSqr(player.position()) > 32 * 32 || !level.hasChunkAt(pet.home)) continue;
            Entity entity = level.getEntity(pet.id);
            if (entity instanceof LivingEntity animal && !ShelterHold.isHeld(animal) && !ShelterHold.isReleased(animal)
                    && ShelterHold.place(level, animal, pet.home, pet.owner)) {
                pet.blocked = "";
                data.setDirty();
            }
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
        if (level.getServer().getPlayerList().getPlayer(pet.owner) == null) { pet.blocked = "owner_offline"; data.setDirty(); return; }
        if (!homeValid(level, pet)) { pet.blocked = "home_unavailable"; data.setDirty(); return; }
        if (!com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter.recoveryAllowed(pet.pending, level)) {
            CompoundTag nativeExtra = pet.pending.getCompound("ForgeData");
            pet.blocked = pet.pending.getString("id").startsWith("irons_spellbooks:")
                    && (!nativeExtra.hasUUID("jemShelterNativeSummoner") || nativeExtra.getLong("jemShelterNativeSummonDeadline") <= 0)
                    ? "native_lifetime_unavailable" : "native_expired";
            data.setDirty(); return;
        }
        Entity restored = EntityType.loadEntityRecursive(pet.pending.copy(), level, entity -> entity);
        if (!(restored instanceof LivingEntity animal) || !pet.id.equals(restored.getUUID()) || !CompanionAdapters.supported(restored) || !pet.owner.equals(CompanionAdapters.owner(restored))) {
            pet.blocked = "native_data_unsupported"; data.setDirty(); return;
        }
        if (!CompanionAdapters.ready(animal)) { pet.blocked = CompanionAdapters.readinessReason(animal); data.setDirty(); return; }
        animal.setHealth(animal.getMaxHealth()); animal.deathTime = 0; animal.hurtTime = 0;
        animal.clearFire(); animal.fallDistance = 0; animal.setAirSupply(animal.getMaxAirSupply());
        animal.removeAllEffects(); animal.setDeltaMovement(Vec3.ZERO);
        BlockState state = level.getBlockState(pet.home); ShelterBlock block = (ShelterBlock) state.getBlock();
        Vec3 point = safePoint(level, animal, pet.home, block, state.getValue(ShelterBlock.FACING), pet.owner);
        if (point == null) { pet.blocked = "no_safe_space"; data.setDirty(); return; }
        animal.moveTo(point.x, point.y, point.z, animal.getYRot(), 0);
        AABB nativeBounds = CompanionAdapters.bounds(animal);
        for (int x = net.minecraft.util.Mth.floor(nativeBounds.minX) >> 4; x <= net.minecraft.util.Mth.floor(nativeBounds.maxX) >> 4; x++)
            for (int z = net.minecraft.util.Mth.floor(nativeBounds.minZ) >> 4; z <= net.minecraft.util.Mth.floor(nativeBounds.maxZ) >> 4; z++)
                if (!ShelterPersistence.ready(level, new net.minecraft.world.level.ChunkPos(x, z))) {
                    pet.blocked = "native_chunk_pending"; data.setDirty(); return;
                }
        // Every retry gets a fresh fence, including a crash between the native write and journal commit.
        // The synchronous first write below returns before this entity can tick or expose an inventory.
        long previousEpoch = pet.epoch; pet.epoch++;
        animal.getPersistentData().putLong(CompanionData.ENTITY_EPOCH, pet.epoch);
        ShelterHold.hold(animal);
        pet.restoring = true; pet.blocked = ""; pet.dimension = level.dimension().location().toString(); pet.lastPosition = animal.blockPosition();
        try { data.flush(level); }
        catch (RuntimeException failure) { pet.epoch = previousEpoch; pet.restoring = false; data.setDirty(); throw failure; }
        // Keep Pending until the exact native entity-storage write succeeds (or matching native disk reload).
        ADMITTING.add(animal);
        try {
            if (level.addFreshEntity(animal)) {
                try {
                    CompanionAdapters.recovered(animal);
                    ShelterPersistence.commit(level, animal, data, pet);
                } catch (RuntimeException failure) {
                    try { CompanionAdapters.rollback(animal); }
                    catch (RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
                    finally { animal.discard(); }
                    throw failure;
                }
                CompanionAdapters.committed(animal);
            } else { pet.restoring = false; pet.blocked = "spawn_denied"; data.flush(level); }
        } finally { ADMITTING.remove(animal); }
    }
    private record Candidate(Vec3 feet, boolean interior, double distance, int projection) { }
    static Vec3 safePoint(ServerLevel level, LivingEntity animal, BlockPos anchor, ShelterBlock block, Direction facing, java.util.UUID owner) {
        List<BlockPos> footprint = block.cells(anchor, facing).stream().filter(p -> p.getY() == anchor.getY()).toList();
        double centerX = footprint.stream().mapToDouble(p -> p.getX() + .5).average().orElse(anchor.getX() + .5);
        double centerZ = footprint.stream().mapToDouble(p -> p.getZ() + .5).average().orElse(anchor.getZ() + .5);
        AABB home = new AABB(footprint.stream().mapToInt(BlockPos::getX).min().orElseThrow(), anchor.getY(),
                footprint.stream().mapToInt(BlockPos::getZ).min().orElseThrow(),
                footprint.stream().mapToInt(p -> p.getX() + 1).max().orElseThrow(), anchor.getY() + block.size(),
                footprint.stream().mapToInt(p -> p.getZ() + 1).max().orElseThrow());
        AABB nativeBounds = CompanionAdapters.bounds(animal);
        boolean outsideOnly = nativeBounds.maxY - nativeBounds.minY > 1.0;
        List<Candidate> candidates = new ArrayList<>();
        CompanionAdapters.Habitat habitat = CompanionAdapters.habitat(animal);
        // The authored floor top is two model units above the anchor floor.
        if (!outsideOnly) candidates.add(new Candidate(new Vec3(centerX, anchor.getY() + 2.0 / 16, centerZ), true, 0, 0));
        for (int x = -8; x <= block.size() + 7; x++) for (int z = -8; z <= block.size() + 7; z++) {
            BlockPos column = anchor.relative(facing.getCounterClockWise(), x).relative(facing, z);
            int horizontal = Math.max(0, -x) + Math.max(0, x - block.size() + 1) + Math.max(0, -z) + Math.max(0, z - block.size() + 1);
            if (horizontal > 8 || (outsideOnly && horizontal == 0)) continue;
            if (!level.hasChunksAt(column.offset(-1, -1, -1), column.offset(1, 1, 1))) continue;
            for (int y = -8; y <= 32; y++) {
                BlockPos support = new BlockPos(column.getX(), anchor.getY() + y - 1, column.getZ());
                if (habitat != CompanionAdapters.Habitat.GROUND) {
                    Vec3 feet = new Vec3(column.getX() + .5, anchor.getY() + y, column.getZ() + .5);
                    double dx = feet.x - centerX, dz = feet.z - centerZ, dy = feet.y - anchor.getY();
                    candidates.add(new Candidate(feet, false, dx * dx + dz * dz + dy * dy, horizontal == 0 ? 0 : 1));
                }
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
        for (Candidate candidate : candidates) if (safe(level, animal, candidate.feet, owner, outsideOnly ? home : null)) return animal.position();
        return null;
    }
    private static boolean safe(ServerLevel level, LivingEntity animal, Vec3 feet, java.util.UUID owner, AABB outsideHome) {
        double bottomOffset = CompanionAdapters.bounds(animal).minY - animal.getY();
        animal.moveTo(feet.x, feet.y - bottomOffset, feet.z, animal.getYRot(), 0); AABB box = CompanionAdapters.bounds(animal);
        // Tall pets must stand beside the whole home, including multipart clearance, never inside or on its roof.
        if (outsideHome != null && box.minX < outsideHome.maxX && box.maxX > outsideHome.minX
                && box.minZ < outsideHome.maxZ && box.maxZ > outsideHome.minZ) return false;
        CompanionAdapters.Habitat habitat = CompanionAdapters.habitat(animal);
        if (box.minY < level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(box)) return false;
        // hasChunksAt precedes collision/fluid calls: those vanilla calls may otherwise fetch missing chunks.
        BlockPos min = BlockPos.containing(box.minX - .01, box.minY - 1, box.minZ - .01);
        BlockPos max = BlockPos.containing(box.maxX + .01, box.maxY + .01, box.maxZ + .01);
        // Native/Forge path-type and support-shape callbacks may examine immediate neighbors.
        if (!level.hasChunksAt(min.offset(-1, -1, -1), max.offset(1, 1, 1))) return false;
        if (!level.noCollision(animal, box) || !level.getEntities(animal, box, entity -> entity.isAlive()).isEmpty()) return false;
        boolean water = true, lava = true, dry = true;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX + .001, box.minY + .001, box.minZ + .001),
                BlockPos.containing(box.maxX - .001, box.maxY - .001, box.maxZ - .001))) {
            var fluid = level.getFluidState(pos);
            water &= fluid.is(net.minecraft.tags.FluidTags.WATER) && pos.getY() + fluid.getHeight(level, pos) >= Math.min(box.maxY, pos.getY() + 1) - .001;
            lava &= fluid.is(net.minecraft.tags.FluidTags.LAVA);
            dry &= fluid.isEmpty();
        }
        boolean fluidLanding = (water && (habitat == CompanionAdapters.Habitat.AQUATIC || habitat == CompanionAdapters.Habitat.AMPHIBIOUS))
                || (lava && habitat == CompanionAdapters.Habitat.LAVA);
        if (!fluidLanding && (!dry || habitat == CompanionAdapters.Habitat.AQUATIC)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            var nativePathType = net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable());
            if ((habitat != CompanionAdapters.Habitat.LAVA && (nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE
                    ))
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_OTHER
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_OTHER
                    || nativePathType == net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_CAUTIOUS) return false;
            if ((habitat != CompanionAdapters.Habitat.LAVA && (state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)))
                    || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.END_PORTAL) || state.is(Blocks.NETHER_PORTAL)) return false;
            if (!ShelterProtection.recoveryAllowed(level, owner, pos)) return false;
        }
        if (fluidLanding || habitat == CompanionAdapters.Habitat.FLYING) return true;
        // Every part of the feet needs supporting collision; no half-supported ledges or water landings.
        AABB support = new AABB(box.minX + .01, box.minY - .03, box.minZ + .01, box.maxX - .01, box.minY, box.maxZ - .01);
        for (double x : new double[]{support.minX, (support.minX + support.maxX) / 2, support.maxX}) for (double z : new double[]{support.minZ, (support.minZ + support.maxZ) / 2, support.maxZ}) {
            AABB corner = new AABB(x - .005, support.minY, z - .005, x + .005, support.maxY, z + .005);
            if (level.noCollision(animal, corner)) return false;
        }
        return true;
    }
}
