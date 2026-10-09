package com.siirio.jemcompat.mixin;

import com.siirio.jemcompat.feature.create.DrillWearAccess;
import com.siirio.jemcompat.feature.create.StationaryDrillWear;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DrillBlockEntity.class, remap = false)
public abstract class DrillBlockEntityMixin implements DrillWearAccess {
    @Unique
    private int jemcompat$wear;
    @Unique
    private int jemcompat$repairs;

    @Override
    public int jemcompat$getWear() {
        return jemcompat$wear;
    }

    @Override
    public void jemcompat$setWear(int wear) {
        jemcompat$wear = Math.max(0, Math.min(StationaryDrillWear.MAX_WEAR, wear));
    }

    @Override
    public int jemcompat$getRepairs() {
        return jemcompat$repairs;
    }

    @Override
    public void jemcompat$setRepairs(int repairs) {
        jemcompat$repairs = Math.max(0, Math.min(StationaryDrillWear.MAX_REPAIRS, repairs));
    }

    @Inject(method = "onBlockBroken", at = @At("TAIL"))
    private void jemcompat$recordWear(BlockState state, CallbackInfo callback) {
        StationaryDrillWear.recordBreak((DrillBlockEntity) (Object) this, state);
    }
}
