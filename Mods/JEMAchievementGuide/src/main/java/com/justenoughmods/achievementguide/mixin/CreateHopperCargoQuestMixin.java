package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCargoTransfers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = net.minecraftforge.items.VanillaInventoryCodeHooks.class, remap = false)
public abstract class CreateHopperCargoQuestMixin {
    @Unique private static final ThreadLocal<CreateCargoTransfers.Attempt> jem$attempt = new ThreadLocal<>();

    @Inject(method = "extractHook", at = @At("HEAD"), remap = false)
    private static void jem$begin(Level world, Hopper inventory, CallbackInfoReturnable<Boolean> callback) {
        jem$attempt.set(CreateCargoTransfers.begin(world, inventory));
    }

    @Inject(method = "extractHook", at = @At("RETURN"), remap = false)
    private static void jem$commit(Level world, Hopper inventory, CallbackInfoReturnable<Boolean> callback) {
        var attempt = jem$attempt.get();
        jem$attempt.remove();
        CreateCargoTransfers.committed(attempt, callback.getReturnValue());
    }
}
