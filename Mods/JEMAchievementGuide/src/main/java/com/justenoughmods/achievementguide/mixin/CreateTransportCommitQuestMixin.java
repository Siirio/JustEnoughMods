package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateTransportInputs;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.belt.transport.BeltInventory", remap = false)
public abstract class CreateTransportCommitQuestMixin {
    @Shadow @Final BeltBlockEntity belt;
    @Shadow @Final private List<TransportedItemStack> items;
    @Inject(method = "insert", at = @At("RETURN"), remap = false)
    private void jem$insertedNativeOutput(TransportedItemStack output, CallbackInfo callback) {
        if (items.contains(output) && belt.getLevel() instanceof ServerLevel level) CreateTransportInputs.committed(level, output);
    }
}
