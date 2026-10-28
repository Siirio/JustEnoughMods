package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateOrderProofs;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.packager.PackagerBlockEntity", remap = false)
public abstract class CreatePackagerQuestMixin {
    @Shadow public ItemStack heldBox;
    @Shadow public List<BigItemStack> queuedExitingPackages;
    @Unique private ItemStack jem$priorBox;
    @Unique private int jem$priorQueue;
    @Unique private boolean jem$observing;

    @Inject(method = "attemptToSend", at = @At("HEAD"), remap = false)
    private void jem$begin(List<PackagingRequest> requests, CallbackInfo callback) {
        jem$observing = CreateOrderProofs.active();
        if (!jem$observing) return;
        jem$priorBox = heldBox;
        jem$priorQueue = queuedExitingPackages.size();
    }

    @Inject(method = "attemptToSend", at = @At("RETURN"), remap = false)
    private void jem$committed(List<PackagingRequest> requests, CallbackInfo callback) {
        if (!jem$observing) return;
        if (heldBox != jem$priorBox && !heldBox.isEmpty()) CreateOrderProofs.packaged(heldBox);
        for (int index = jem$priorQueue; index < queuedExitingPackages.size(); index++) {
            CreateOrderProofs.packaged(queuedExitingPackages.get(index).stack);
        }
        jem$priorBox = null;
        jem$observing = false;
    }
}
