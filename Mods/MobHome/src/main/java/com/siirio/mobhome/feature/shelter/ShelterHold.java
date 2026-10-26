package com.siirio.mobhome.feature.shelter;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The home's temporary sit state belongs to the native entity and survives chunk saves. */
final class ShelterHold {
    private static final String WAS_NO_AI = "JemShelterWasNoAi";
    private static final String HELD = "JemShelterHeld";
    private static final String RELEASED = "JemShelterReleasedByOwner";

    private ShelterHold() {
    }

    static boolean isHeld(Entity entity) {
        return entity.getPersistentData().getBoolean(HELD);
    }

    static boolean isReleased(Entity entity) {
        return entity.getPersistentData().getBoolean(RELEASED);
    }

    static boolean place(ServerLevel level, LivingEntity animal, BlockPos anchor, java.util.UUID owner) {
        if (!level.hasChunkAt(anchor)) return false;
        BlockState state = level.getBlockState(anchor);
        if (!(state.getBlock() instanceof ShelterBlock block)
                || state.getValue(ShelterBlock.CELL) != 0
                || !(level.getBlockEntity(anchor) instanceof ShelterController controller)
                || !owner.equals(controller.owner())
                || !block.intact(level, anchor, state.getValue(ShelterBlock.FACING))) return false;
        Vec3 previous = animal.position();
        float yaw = animal.getYRot();
        float pitch = animal.getXRot();
        Vec3 point = ShelterRecovery.safePoint(level, animal, anchor, block,
                state.getValue(ShelterBlock.FACING), owner);
        if (point == null) {
            animal.moveTo(previous.x, previous.y, previous.z, yaw, pitch);
            return false;
        }
        animal.teleportTo(point.x, point.y, point.z);
        animal.setDeltaMovement(Vec3.ZERO);
        animal.fallDistance = 0;
        hold(animal);
        return true;
    }

    static void hold(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(RELEASED);
        if (!data.getBoolean(HELD)) {
            if (entity instanceof Mob mob) data.putBoolean(WAS_NO_AI, mob.isNoAi());
            data.putBoolean(HELD, true);
        }
        if (entity instanceof Mob mob) mob.setNoAi(true);
        if (CompanionAdapters.nativeOwnerTeleport(entity)) data.putBoolean(CompanionData.FOLLOW_HOLD, true);
    }

    static void release(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(HELD) && entity instanceof Mob mob && data.contains(WAS_NO_AI))
            mob.setNoAi(data.getBoolean(WAS_NO_AI));
        data.remove(HELD);
        data.remove(WAS_NO_AI);
        data.remove(CompanionData.FOLLOW_HOLD);
        data.remove(RELEASED);
    }

    static void follow(Entity entity) {
        release(entity);
        entity.getPersistentData().putBoolean(RELEASED, true);
    }
}
