package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.siirio.jemclaims.compat.MachineContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.utility.BlockHelper", remap = false)
public abstract class CreateSchematicBlockMixin {
    @Inject(method = "placeSchematicBlock", at = @At("HEAD"), cancellable = true)
    private static void jemClaims$place(Level level, BlockState state, BlockPos target, ItemStack stack, CompoundTag data, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && MachineContext.source() != null
                && (!FlanBridge.canAutomate(serverLevel, MachineContext.source(), target, ClaimPermission.PLACE)
                || !FlanBridge.canAutomate(serverLevel, MachineContext.source(), target, ClaimPermission.BREAK))) { ci.cancel(); return; }
        if (level instanceof ServerLevel serverLevel) com.siirio.jemclaims.compat.CreatePlacement.placing(serverLevel, MachineContext.source(), target);
    }
}
