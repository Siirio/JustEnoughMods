package com.siirio.jemdrill.mixin;

import com.siirio.jemdrill.drill.DrillWearPolicy;
import com.siirio.jemdrill.drill.DrillWearAccess;
import com.siirio.jemdrill.drill.StationaryDrillWear;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockBreakingKineticBlockEntity.class, remap = false)
public abstract class BlockBreakingKineticBlockEntityMixin {
    @Inject(method = "write", at = @At("TAIL"))
    private void jemcompat$writeWear(CompoundTag tag, boolean clientPacket, CallbackInfo callback) {
        if ((Object) this instanceof DrillWearAccess wear) {
            tag.putInt(DrillWearPolicy.WEAR_KEY, wear.jemcompat$getWear());
            tag.putInt(DrillWearPolicy.REPAIRS_KEY, wear.jemcompat$getRepairs());
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void jemcompat$readWear(CompoundTag tag, boolean clientPacket, CallbackInfo callback) {
        if ((Object) this instanceof DrillWearAccess wear) {
            wear.jemcompat$setWear(tag.getInt(DrillWearPolicy.WEAR_KEY));
            wear.jemcompat$setRepairs(tag.getInt(DrillWearPolicy.REPAIRS_KEY));
        }
    }

    @Inject(method = "getBreakSpeed", at = @At("RETURN"), cancellable = true)
    private void jemcompat$slowWornDrill(CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof DrillWearAccess wear && wear.jemcompat$getWear() >= DrillWearPolicy.DULL_WEAR) {
            callback.setReturnValue(callback.getReturnValueF() * DrillWearPolicy.DULL_SPEED_MULTIPLIER);
        }
    }

    @Inject(method = "canBreak", at = @At("HEAD"), cancellable = true)
    private void jemcompat$breakOnBedrock(BlockState state, float hardness, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof DrillBlockEntity drill && StationaryDrillWear.destroyForBedrock(drill, state)) {
            callback.setReturnValue(false);
        }
    }
}
