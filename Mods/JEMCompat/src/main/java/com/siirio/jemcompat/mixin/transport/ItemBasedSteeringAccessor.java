package com.siirio.jemcompat.mixin.transport;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ItemBasedSteering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemBasedSteering.class)
public interface ItemBasedSteeringAccessor {
    @Accessor("boosting") void jemcompat$setBoosting(boolean boosting);
    @Accessor("boosting") boolean jemcompat$isBoosting();
    @Accessor("boostTime") void jemcompat$setBoostTime(int boostTime);
    @Accessor("entityData") SynchedEntityData jemcompat$getEntityData();
    @Accessor("boostTimeAccessor") EntityDataAccessor<Integer> jemcompat$getBoostTimeAccessor();
}
