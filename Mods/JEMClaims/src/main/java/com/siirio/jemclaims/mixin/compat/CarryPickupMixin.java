package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "tschipp.carryon.common.carry.PickupHandler", remap = false)
public abstract class CarryPickupMixin {
    @Inject(method = "tryPickupEntity", at = @At("HEAD"), cancellable = true)
    private static void jemClaims$pickup(ServerPlayer player, Entity target, Function<Entity, Boolean> callback, CallbackInfoReturnable<Boolean> cir) {
        if (!FlanBridge.can(player, player.serverLevel(), target.blockPosition(), ClaimPermission.ANIMALINTERACT)
                || target.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()
                && !FlanBridge.can(player, player.serverLevel(), target.blockPosition(), ClaimPermission.OPENCONTAINER)) {
            cir.setReturnValue(false);
        }
    }
}
