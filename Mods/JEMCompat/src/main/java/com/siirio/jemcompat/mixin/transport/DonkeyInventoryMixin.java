package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractChestedHorse.class)
public abstract class DonkeyInventoryMixin {
    private static final int DONKEY_TOTAL_INVENTORY_SIZE = 20;
    private static final int DONKEY_INVENTORY_COLUMNS = 6;

    @Inject(method = "getInventorySize", at = @At("HEAD"), cancellable = true)
    private void jemcompat$donkeyInventorySize(CallbackInfoReturnable<Integer> callback) {
        AbstractChestedHorse horse = (AbstractChestedHorse) (Object) this;
        if (horse.getType() == EntityType.DONKEY && horse.hasChest()) {
            callback.setReturnValue(DONKEY_TOTAL_INVENTORY_SIZE);
        }
    }

    @Inject(method = "getInventoryColumns", at = @At("HEAD"), cancellable = true)
    private void jemcompat$donkeyInventoryColumns(CallbackInfoReturnable<Integer> callback) {
        if (((AbstractChestedHorse) (Object) this).getType() == EntityType.DONKEY) {
            callback.setReturnValue(DONKEY_INVENTORY_COLUMNS);
        }
    }
}
