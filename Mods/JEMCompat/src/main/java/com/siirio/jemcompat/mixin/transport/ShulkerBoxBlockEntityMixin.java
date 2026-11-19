package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.BackpackPolicy;
import com.siirio.jemcompat.feature.transport.ShulkerCapacity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin {
    public int getMaxStackSize() {
        return ShulkerCapacity.BLOCK_STACK_LIMIT;
    }

    @Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
    private void jemcompat$rejectBackpacksFromAutomation(int slot, ItemStack stack, Direction direction,
            CallbackInfoReturnable<Boolean> callback) {
        if (BackpackPolicy.isBackpack(stack)) {
            callback.setReturnValue(false);
        }
    }
}
