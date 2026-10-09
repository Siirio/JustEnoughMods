package com.siirio.jemcompat.mixin.backtobed;

import com.siirio.jemcompat.feature.backtobed.GroupBedTeleport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.bakycoder.backtobed.item.MagicalReturner", remap = false)
public abstract class MagicalReturnerMixin {
    private static final int ACTIVATION_TICKS = 40;

    @Inject(method = "m_5929_", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$teleportGroup(Level level, LivingEntity livingEntity, ItemStack stack,
                                         int remainingUseTicks, CallbackInfo callback) {
        if (!(livingEntity instanceof ServerPlayer host) || level.isClientSide()) {
            return;
        }
        Item returner = (Item) (Object) this;
        if (returner.getUseDuration(stack) - remainingUseTicks < ACTIVATION_TICKS) {
            return;
        }
        GroupBedTeleport.activate(host, returner);
        callback.cancel();
    }
}
