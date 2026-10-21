package com.siirio.jemvisualsandbox.mixin;

import com.siirio.jemvisualsandbox.workbench.SandboxUtilities;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class SandboxLavaDrinkMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void sandbox$drink(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> result) {
        if(SandboxUtilities.tryLavaDrink(player,hand))result.setReturnValue(InteractionResultHolder.consume(player.getItemInHand(hand)));
    }
}
