package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.BackpackPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BackpackBlockEntity.class, remap = false)
public abstract class BackpackBlockEntityMigrationMixin {
    @Inject(method = "startOpen", at = @At("HEAD"))
    private void jemcompat$migrateBeforeOpen(Player player, CallbackInfo callback) {
        if (player instanceof ServerPlayer serverPlayer) {
            BackpackPolicy.migrate(((BackpackBlockEntity) (Object) this).getBackpackWrapper().getBackpack(), serverPlayer);
        }
    }
}
