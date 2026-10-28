package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateBeltTransfers;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.belt.transport.BeltInventory", remap = false)
public abstract class CreateBeltInputQuestMixin {
    @Shadow @Final BeltBlockEntity belt;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/behaviour/DirectBeltInputBehaviour;handleInsertion(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$actualPoweredBelt(DirectBeltInputBehaviour target, TransportedItemStack input, Direction side, boolean simulate) {
        return CreateBeltTransfers.insertion(belt, input, () -> target.handleInsertion(input, side, simulate));
    }
}
