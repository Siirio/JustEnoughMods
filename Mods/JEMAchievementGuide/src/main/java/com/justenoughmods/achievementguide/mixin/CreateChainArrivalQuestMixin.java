package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateParcelProofs;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity", remap = false)
public abstract class CreateChainArrivalQuestMixin {
    @Redirect(method = "exportToPort", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packagePort/frogport/FrogportBlockEntity;startAnimation(Lnet/minecraft/world/item/ItemStack;Z)V"), remap = false)
    private void jem$actualNativeChainArrival(FrogportBlockEntity receiver, ItemStack box, boolean inward) {
        if (!inward) CreateParcelProofs.arriving(box, (BlockEntity)(Object)receiver);
        receiver.startAnimation(box, inward);
    }
}
