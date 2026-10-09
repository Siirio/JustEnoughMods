package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.talhanation.smallships.world.inventory.ContainerUtility", remap = false)
public abstract class ShipCargoMixin {
    @Inject(method = "openShipMenu", at = @At("HEAD"), cancellable = true)
    private static void jemClaims$open(Player player, @Coerce Object ship, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && ship instanceof Entity entity
                && !FlanBridge.can(serverPlayer, serverPlayer.serverLevel(), entity.blockPosition(), ClaimPermission.OPENCONTAINER)) ci.cancel();
    }
}
