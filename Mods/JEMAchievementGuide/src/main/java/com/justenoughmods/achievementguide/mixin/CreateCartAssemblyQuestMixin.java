package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCartProofs;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity", remap = false)
public abstract class CreateCartAssemblyQuestMixin {
    @Unique private boolean jem$unassembled;
    @Inject(method = "assemble", at = @At("HEAD"), remap = false)
    private void jem$before(Level world, BlockPos pos, AbstractMinecart cart, CallbackInfo callback) { jem$unassembled = cart.getPassengers().isEmpty(); }
    @Inject(method = "assemble", at = @At("RETURN"), remap = false)
    private void jem$assembled(Level world, BlockPos pos, AbstractMinecart cart, CallbackInfo callback) {
        if (jem$unassembled) CreateCartProofs.assembled((CartAssemblerBlockEntity)(Object)this, cart);
    }
}
