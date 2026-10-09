package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartFurnace.class)
public abstract class FurnaceMinecartMixin {
    private static final int COAL_BLOCK_FUEL_TICKS = 1620 * 20;
    private static final float FUELED_MAXIMUM_SPEED = 10.0F / 20.0F;

    @Shadow private int fuel;

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void jemcompat$acceptCoalBlock(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.COAL_BLOCK) || fuel > 0) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        fuel = COAL_BLOCK_FUEL_TICKS;
        MinecartFurnace minecart = (MinecartFurnace) (Object) this;
        minecart.xPush = minecart.getX() - player.getX();
        minecart.zPush = minecart.getZ() - player.getZ();
        callback.setReturnValue(InteractionResult.sidedSuccess(minecart.level().isClientSide));
    }

    @Inject(method = "getMaxCartSpeedOnRail", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$fueledMaximumSpeed(CallbackInfoReturnable<Float> callback) {
        if (fuel > 0) {
            callback.setReturnValue(FUELED_MAXIMUM_SPEED);
        }
    }
}
