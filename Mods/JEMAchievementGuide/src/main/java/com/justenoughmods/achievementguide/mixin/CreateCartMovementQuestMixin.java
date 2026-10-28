package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCartProofs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class CreateCartMovementQuestMixin {
    @Unique private double jem$x, jem$y, jem$z;
    @Inject(method = "moveAlongTrack", at = @At("HEAD"))
    private void jem$before(BlockPos pos, BlockState state, CallbackInfo callback) {
        AbstractMinecart cart = (AbstractMinecart)(Object)this; jem$x = cart.getX(); jem$y = cart.getY(); jem$z = cart.getZ();
    }
    @Inject(method = "moveAlongTrack", at = @At("RETURN"))
    private void jem$after(BlockPos pos, BlockState state, CallbackInfo callback) {
        if (ModList.get().isLoaded("create")) CreateCartProofs.moved((AbstractMinecart)(Object)this, jem$x, jem$y, jem$z);
    }
}
