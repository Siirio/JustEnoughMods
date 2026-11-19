package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.ShulkerCapacity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackWideCountMixin {
    @Inject(method = "save", at = @At("RETURN"))
    private void jemcompat$markWideShulkerCount(CompoundTag target,
            CallbackInfoReturnable<CompoundTag> callback) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getCount() == ShulkerCapacity.BLOCK_STACK_LIMIT
                && ShulkerCapacity.isEligibleBlockStack(stack)) {
            callback.getReturnValue().putBoolean(ShulkerCapacity.WIDE_COUNT_TAG, true);
        } else {
            callback.getReturnValue().remove(ShulkerCapacity.WIDE_COUNT_TAG);
        }
    }

    @Inject(method = "of", at = @At("RETURN"))
    private static void jemcompat$readWideShulkerCount(CompoundTag source,
            CallbackInfoReturnable<ItemStack> callback) {
        ItemStack stack = callback.getReturnValue();
        if (source.getBoolean(ShulkerCapacity.WIDE_COUNT_TAG) && source.getByte("Count") == Byte.MIN_VALUE
                && ShulkerCapacity.isEligibleBlockStack(stack)) {
            stack.setCount(ShulkerCapacity.BLOCK_STACK_LIMIT);
        }
    }
}
