package com.siirio.jemserver.mixin.cooking;

import com.siirio.jemserver.smp.events.CookingStations;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceCookingMixin {
    @Unique private Map<Item, Integer> jem$before;

    @Inject(method = "burn", at = @At("HEAD"))
    private void jem$beforeCooking(CallbackInfoReturnable<Boolean> cir) {
        jem$before = CookingStations.snapshot(this);
    }

    @Inject(method = "burn", at = @At("RETURN"))
    private void jem$afterCooking(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) CookingStations.completed(this, jem$before);
    }
}
