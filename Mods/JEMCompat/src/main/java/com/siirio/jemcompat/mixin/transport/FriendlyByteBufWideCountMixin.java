package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.ShulkerCapacity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufWideCountMixin {
    @Inject(method = "readItem", at = @At("RETURN"))
    private void jemcompat$readUnsignedWideCount(CallbackInfoReturnable<ItemStack> callback) {
        ItemStack stack = callback.getReturnValue();
        if (stack.getCount() == Byte.MIN_VALUE && ShulkerCapacity.isEligibleBlockStack(stack)) {
            stack.setCount(ShulkerCapacity.BLOCK_STACK_LIMIT);
        }
    }
}
