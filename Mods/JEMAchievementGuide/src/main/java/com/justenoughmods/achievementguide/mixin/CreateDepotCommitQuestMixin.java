package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateTransportInputs;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.depot.DepotBehaviour", remap = false)
public abstract class CreateDepotCommitQuestMixin {
    @Shadow TransportedItemStack heldItem;
    @Inject(method = "setCenteredHeldItem", at = @At("RETURN"), remap = false)
    private void jem$installedNativeOutput(TransportedItemStack output, CallbackInfo callback) {
        if (heldItem == output && ((BlockEntityBehaviour)(Object)this).getWorld() instanceof ServerLevel level) {
            CreateTransportInputs.committed(level, output);
        }
    }
}
