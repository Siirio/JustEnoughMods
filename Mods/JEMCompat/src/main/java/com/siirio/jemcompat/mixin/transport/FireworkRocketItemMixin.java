package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.FireworkBoostPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void jemcompat$consumeElytraBoostCharge(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        if (!level.isClientSide && player.isFallFlying() && player instanceof ServerPlayer serverPlayer
                && !FireworkBoostPolicy.consume(serverPlayer)) {
            callback.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
}
